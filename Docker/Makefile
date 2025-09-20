# =========================================
# Variables
# =========================================
COMPOSE       := docker compose
ENV_FILE      := .env
SERVICE       ?= web             # default service nếu không chỉ định
COMPOSE_FILE  := docker-compose.$(SERVICE).yml

# =========================================
# Help
# =========================================
.PHONY: help
help:
	@echo "Usage: make <target> [SERVICE=<service-name>]"
	@echo ""
	@echo "Targets:"
	@echo "  up            - Start the specified service"
	@echo "  down          - Stop and remove the specified service"
	@echo "  restart       - Restart the specified service"
	@echo "  logs          - View logs for the specified service"
	@echo "  build         - Build images for the specified service"
	@echo "  ps            - Show running containers"
	@echo "  exec cmd=sh   - Exec into a container (default: sh)"
	@echo "  clean         - Remove stopped containers, unused volumes, networks"
	@echo "  prune         - Remove ALL unused docker resources (DANGEROUS)"
	@echo ""
	@echo "Examples:"
	@echo "  make up SERVICE=api"
	@echo "  make logs SERVICE=keycloak"
	@echo "  make exec SERVICE=web cmd=bash"

# =========================================
# Docker Compose Commands
# =========================================

.PHONY: up
up:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) up -d

.PHONY: down
down:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) down

.PHONY: restart
restart:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) restart

.PHONY: logs
logs:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) logs -f

.PHONY: build
build:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) build

.PHONY: ps
ps:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) ps

.PHONY: exec
exec:
	$(COMPOSE) --env-file $(ENV_FILE) -f $(COMPOSE_FILE) exec $(SERVICE) $(cmd)

# =========================================
# Utilities
# =========================================
.PHONY: clean
clean:
	docker system prune -f
	docker volume prune -f

.PHONY: prune
prune:
	docker system prune -a --volumes -f
