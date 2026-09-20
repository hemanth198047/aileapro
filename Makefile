.PHONY: up down api web test frontend-install

up:
	docker compose -f infra/compose/docker-compose.yml up -d

down:
	docker compose -f infra/compose/docker-compose.yml down

api:
	cd backend && ./gradlew :api:bootRun

frontend-install:
	cd frontend && pnpm install

web: frontend-install
	cd frontend && pnpm --filter web dev

test:
	cd backend && ./gradlew check
	cd frontend && pnpm -r typecheck
