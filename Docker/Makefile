# Variables
COMPOSE := docker compose
ENV_FILE := .env

# Default target
.PHONY: help
help:
	@echo "Usage: make <target> service=<service-name>"
	@echo "Available targets:"
	@echo "  up        - Start the specified service using Docker Compose"
	@echo "  down      - Stop and remove the specified service"
	@echo "  logs      - View logs for the specified service"
	@echo "  restart   - Restart the specified service"
	@echo "Available services: Define service=<service-name> (e.g., service=web, service=api, service=keycloak)"

# Check if service is defined
ifndef service
$(error service is not defined. Usage: make <target> service=<service-name>)
endif

# Start service using Docker Compose
.PHONY: up
up:
	$(COMPOSE) --env-file $(ENV_FILE) -f docker-compose.$(service).yml up -d

# Stop and remove service
.PHONY: down
down:
	$(COMPOSE) --env-file $(ENV_FILE) -f docker-compose.$(service).yml down

# View logs
.PHONY: logs
logs:
	$(COMPOSE) --env-file $(ENV_FILE) -f docker-compose.$(service).yml logs -f

# Restart service
.PHONY: restart
restart:
	$(COMPOSE) --env-file $(ENV_FILE) -f docker-compose.$(service).yml restart