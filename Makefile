# Makefile with workspace directory support
SHELL := /bin/bash

# Configuration
PROJECT_ROOT := $(shell pwd)
DOCKER_REGISTRY := your-registry/your-org
MAVEN_CMD := ./mvnw
VERSION := $(shell $(MAVEN_CMD) help:evaluate -Dexpression=project.version -q -DforceStdout)

# Default workspace directory (can be overridden with -wd or --wkdir)
WORKSPACE ?= SpringServices

# Service discovery
SERVICE_DIRS := $(wildcard $(WORKSPACE)/*)
SERVICES := $(foreach dir,$(SERVICE_DIRS),$(if $(wildcard $(dir)/pom.xml),$(notdir $(dir)),))

# Docker settings
DOCKER_CMD := docker
DOCKER_FILE := $(PROJECT_ROOT)/Dockerfile
DOCKER_CONTEXT := $(PROJECT_ROOT)

# Build flags
BUILD_ARGS := --build-arg JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

.PHONY: all
all: build

.PHONY: build
build: package docker

.PHONY: package
package:
	@echo "Building JAR packages in $(WORKSPACE)..."
	@cd $(WORKSPACE) && $(MAVEN_CMD) clean package -DskipTests -Dskip.docker.build=true

.PHONY: docker
docker: $(addprefix docker-,$(SERVICES))

.PHONY: $(addprefix docker-,$(SERVICES))
$(addprefix docker-,$(SERVICES)): docker-%:
	@echo "Building $* with custom port..."
	@cd $(WORKSPACE) && \
	SERVICE_NAME=$* && \
	ARTIFACT_NAME=$$($(MAVEN_CMD) -f $*/pom.xml help:evaluate -Dexpression=project.build.finalName -q -DforceStdout) && \
	EXPOSED_PORT=$$($(MAVEN_CMD) -f $*/pom.xml help:evaluate -Dexpression=service.port -q -DforceStdout) && \
	$(DOCKER_CMD) build \
		--build-arg SERVICE_NAME=$$SERVICE_NAME \
		--build-arg ARTIFACT_NAME=$$ARTIFACT_NAME \
		--build-arg EXPOSED_PORT=$$EXPOSED_PORT \
		-t $(DOCKER_REGISTRY)/$$SERVICE_NAME:$(VERSION) \
		-f $(DOCKER_FILE) \
		.

.PHONY: service
service:
ifndef name
	$(error "Usage: make service name=<service-name> [wkdir=<workspace-dir>]")
endif
	@$(MAKE) docker-$(name) WORKSPACE=$(WORKSPACE)

.PHONY: push
push:
	@for service in $(SERVICES); do \
		echo "Pushing image: $(DOCKER_REGISTRY)/$$service:$(VERSION)"; \
		$(DOCKER_CMD) push $(DOCKER_REGISTRY)/$$service:$(VERSION); \
		$(DOCKER_CMD) push $(DOCKER_REGISTRY)/$$service:latest; \
	done

.PHONY: clean
clean:
	@echo "Cleaning workspace: $(WORKSPACE)"
	@cd $(WORKSPACE) && $(MAVEN_CMD) clean
	@for service in $(SERVICES); do \
		echo "Removing Docker images for: $$service"; \
		$(DOCKER_CMD) rmi -f $(DOCKER_REGISTRY)/$$service:$(VERSION) || true; \
		$(DOCKER_CMD) rmi -f $(DOCKER_REGISTRY)/$$service:latest || true; \
	done

.PHONY: help
help:
	@echo "Multi-Workspace Docker Build System"
	@echo "Usage: make [target] [wkdir=<workspace-dir>]"
	@echo ""
	@echo "Targets:"
	@echo "  all          Build all services (default)"
	@echo "  build        Build JARs and Docker images for all services"
	@echo "  package      Build JAR packages only"
	@echo "  docker       Build Docker images for all services"
	@echo "  service      Build single service (requires name=service-name)"
	@echo "  push         Push all images to registry"
	@echo "  clean        Clean project and remove Docker images"
	@echo "  help         Show this help"
	@echo ""
	@echo "Options:"
	@echo "  wkdir=<dir>  Specify workspace directory (default: SpringServices)"
	@echo ""
	@echo "Available workspaces:"
	@echo "  - SpringServices"
	@echo "  - PythonServices"
	@echo "  - Docker"
	@echo ""
	@echo "Current workspace services ($(WORKSPACE)):"
	@for service in $(SERVICES); do \
		echo "  - $$service"; \
	done