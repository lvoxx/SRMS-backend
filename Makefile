SHELL := /bin/bash

WORKSPACE := SpringServices
DOCKER_REGISTRY := lvoxx/srms
VERSION ?= 1.0.0

# Khai báo module/service cố định
MODULES := contactor customer dashboard gateway kitchen notification order payment reporting warehouse

.PHONY: all build docker push clean

all: build docker

## ========================
## Build tất cả JARs
## ========================
build:
	@echo ">>> Building all modules from root pom..."
	@cd $(WORKSPACE) && mvn clean package -DskipTests

## ========================
## Build Docker images
## ========================
docker: $(addprefix docker-,$(MODULES))

# Rule build Docker cho từng module
docker-%:
	@echo ">>> Building Docker image for $*"
	$(eval ARTIFACT_NAME := $(shell mvn help:evaluate -Dexpression=project.build.finalName -q -DforceStdout -f $(WORKSPACE)/$*/pom.xml))
	cd $(WORKSPACE)/$* && \
	docker build -t $(DOCKER_REGISTRY)/$*:$(VERSION) .

## ========================
## Push images
## ========================
push: $(addprefix push-,$(SERVICES))

push-%:
	@echo ">>> Pushing $(DOCKER_REGISTRY)/$*:$(VERSION)"
	@docker push $(DOCKER_REGISTRY)/$*:$(VERSION)

## ========================
## Clean
## ========================
clean:
	@echo ">>> Cleaning root project"
	@cd $(WORKSPACE) && mvn clean


## ========================
## Help
## ========================
.PHONY: help
help:
	@echo "Usage: make <target>"
	@echo ""
	@echo "Targets:"
	@echo "  build         Build JARs for all services"
	@echo "  build-<svc>   Build JAR for one service"
	@echo "  docker        Build Docker images for all services"
	@echo "  docker-<svc>  Build Docker image for one service"
	@echo "  push          Push all Docker images to registry"
	@echo "  clean         Clean all services"
