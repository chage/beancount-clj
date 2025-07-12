.PHONY: dev docker-up docker-down

DEV_CONTAINER_NAME = beancount-clj
HOST_UID := $(shell id -u)
HOST_GID := $(shell id -g)
HOST_UNAME := $(shell whoami)
HOST_GNAME := $(shell id -gn)

env:
	@echo "Please run . venv/bin/activate first."
	@echo "====================================="
	@echo "python -m venv venv"
	@echo ". venv/bin/activate"
	@echo "pip install beancount"

dev: env
	clojure -M:common:dev:nrepl $(ARGS)

docker-up:
	@if [ -n "$$(docker ps -q -f name=$(DEV_CONTAINER_NAME) -f status=running)" ]; then \
		echo "Container $(DEV_CONTAINER_NAME) is already running."; \
	else \
		echo "Container $(DEV_CONTAINER_NAME) is not running. Starting..."; \
        export HOST_UID=$(HOST_UID) HOST_GID=$(HOST_GID) HOST_UNAME=$(HOST_UNAME) HOST_GNAME=$(HOST_GNAME); \
		docker compose up --build -d; \
	fi

docker-down:
	@echo "Shutting down Container $(DEV_CONTAINER_NAME)..."; \
	docker compose down
