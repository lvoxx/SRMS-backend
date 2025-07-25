# Makefile with workspace directory and Docker Compose support
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
DOCKER_COMPOSE_DIR := ./Docker
DOCKER_COMPOSE_CMD := docker-compose

# Build flags
BUILD_ARGS := --build-arg JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

# Docker Compose files
COMPOSE_COMMON := -f $(DOCKER_COMPOSE_DIR)/docker-compose.common.yml
COMPOSE_FILES := $(wildcard $(DOCKER_COMPOSE_DIR)/docker-compose.*.yml)
COMPOSE_EXCLUDE_COMMON := $(filter-out $(DOCKER_COMPOSE_DIR)/docker-compose.common.yml,$(COMPOSE_FILES))

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

# Docker Compose targets
.PHONY: compose-up
compose-up: compose-up-all

.PHONY: compose-up-all
compose-up-all:
	@echo "Starting all services with docker-compose..."
	@$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) $(foreach file,$(COMPOSE_EXCLUDE_COMMON),-f $(file)) up -d

.PHONY: compose-up-%
compose-up-%:
	@if [ -f "$(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml" ]; then \
		echo "Starting $* service with docker-compose..."; \
		$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) -f $(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml up -d; \
	else \
		echo "docker-compose.$*.yml not found in $(DOCKER_COMPOSE_DIR)"; \
		exit 1; \
	fi

.PHONY: compose-down
compose-down:
	@echo "Stopping all docker-compose services..."
	@$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) $(foreach file,$(COMPOSE_EXCLUDE_COMMON),-f $(file)) down

.PHONY: compose-down-%
compose-down-%:
	@if [ -f "$(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml" ]; then \
		echo "Stopping $* service with docker-compose..."; \
		$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) -f $(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml down; \
	else \
		echo "docker-compose.$*.yml not found in $(DOCKER_COMPOSE_DIR)"; \
		exit 1; \
	fi

.PHONY: compose-logs
compose-logs:
	@$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) $(foreach file,$(COMPOSE_EXCLUDE_COMMON),-f $(file)) logs -f

.PHONY: compose-logs-%
compose-logs-%:
	@if [ -f "$(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml" ]; then \
		$(DOCKER_COMPOSE_CMD) $(COMPOSE_COMMON) -f $(DOCKER_COMPOSE_DIR)/docker-compose.$*.yml logs -f; \
	else \
		echo "docker-compose.$*.yml not found in $(DOCKER_COMPOSE_DIR)"; \
		exit 1; \
	fi

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
	@echo "Multi-Workspace Docker Build and Compose System"
	@echo "Usage: make [target] [wkdir=<workspace-dir>]"
	@echo ""
	@echo "Build Targets:"
	@echo "  all          Build all services (default)"
	@echo "  build        Build JARs and Docker images for all services"
	@echo "  package      Build JAR packages only"
	@echo "  docker       Build Docker images for all services"
	@echo "  service      Build single service (requires name=service-name)"
	@echo "  push         Push all images to registry"
	@echo "  clean        Clean project and remove Docker images"
	@echo ""
	@echo "Docker Compose Targets:"
	@echo "  compose-up           Start all services with docker-compose"
	@echo "  compose-up-<service> Start specific service (e.g., compose-up-kafka)"
	@echo "  compose-down         Stop all services"
	@echo "  compose-down-<service> Stop specific service"
	@echo "  compose-logs         Show logs for all services"
	@echo "  compose-logs-<service> Show logs for specific service"
	@echo "  help                 Show this help"
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
	@echo ""
	@echo "Available Docker Compose services:"
	@for file in $(COMPOSE_EXCLUDE_COMMON); do \
		service=$$(basename $$file | sed 's/docker-compose.\(.*\).yml/\1/'); \
		echo "  - $$service"; \
	done