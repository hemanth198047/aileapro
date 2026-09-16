.PHONY: up down api web test

up:
	docker compose -f infra/compose/docker-compose.yml up -d

down:
	docker compose -f infra/compose/docker-compose.yml down

api:
	cd backend && ./gradlew :api:bootRun

web:
	pnpm --filter web dev

test:
	cd backend && ./gradlew check
