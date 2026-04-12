# Docker Commands

## Build Images

```bash
docker build -t achrafyoussef/bizcord-mediasoup:latest .

docker build -t achrafyoussef/bizcord-backend:latest .

docker build -t achrafyoussef/bizcord-frontend:latest .
```

## Docker Compose

```bash
docker compose up --build -d

docker compose up -d

docker compose --env-file=.env up -d

docker compose down

docker compose down -v

docker compose restart backend

docker compose up --build -d backend
```

## Logs

```bash
docker compose logs

docker compose logs -f

docker compose logs -f backend
docker compose logs -f postgres
docker compose logs -f frontend
```

## Status

```bash
docker compose ps

docker stats
```

