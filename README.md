# BizCord Backend

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.13-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-25-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-336791?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-ready-2496ED?logo=docker&logoColor=white)](https://hub.docker.com/r/achrafyoussef/bizcord-backend)

REST API and WebSocket backend for **BizCord** — a Discord-like real-time team communication platform.

## 📑 Table of Contents

- [Overview](#-overview)
- [Architecture](#️-architecture)
- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Prerequisites](#-prerequisites)
- [Setup Guide](#-setup-guide)
- [Environment Variables](#-environment-variables)
- [API Overview](#-api-overview)
- [Running Tests](#-running-tests)
- [Docker](#-docker)
- [Author](#-author)

---

## 🔭 Overview

BizCord Backend powers the full server-side logic of the BizCord platform. It exposes a REST API for resource management and uses STOMP over WebSocket for real-time events (messages, typing indicators, notifications, voice signalling). Authentication is JWT-based with short-lived access tokens and long-lived refresh tokens stored in HttpOnly cookies.

---

## 🏗️ Architecture

```
Client (Angular SPA)
        │
        ├── REST  →  Spring Boot Controllers (HTTP/HTTPS)
        └── STOMP →  Spring WebSocket (SockJS-free, raw WS)
                          │
                          ├── MessageWebSocketController
                          ├── DirectMessageWebSocketController
                          ├── TypingWebSocketController
                          ├── VoiceWebSocketController
                          └── CallWebSocketController
                                    │
                          Service Layer (Business Logic)
                                    │
                          Spring Data JPA  →  PostgreSQL 17
                                    │
                          mediasoup SFU  (voice / video rooms via REST)
```

---

## ✨ Features

- **Authentication** — JWT access tokens (15 min) + HttpOnly refresh-token cookies (7 days), token rotation on refresh
- **Email Verification** — Account activation via email link on registration; resend endpoint included
- **Password Reset** — Forgot-password flow with time-limited token sent by email
- **Servers & Channels** — Create/manage servers, text and voice channels, channel categories with drag-and-drop ordering support
- **Real-time Messaging** — STOMP WebSocket for live message delivery, edits, and deletes
- **Direct Messages** — Private 1-to-1 conversations with real-time delivery
- **Voice & Video** — WebRTC room orchestration delegated to the mediasoup SFU
- **File Uploads** — Images and attachments stored in MinIO object storage behind stable backend upload/download endpoints
- **Reactions** — Emoji reactions on messages
- **Typing Indicators** — Broadcast presence in text channels and DMs
- **Notifications** — Server-sent notification events over WebSocket
- **Message Search** — Full-text search across channel messages
- **Invites** — Token-based server invite links
- **Rate Limiting** — Per-endpoint request throttling via Bucket4j
- **Member Management** — Role-based access within servers
- **Soft Delete** — Users are soft-deleted (flagged) rather than physically removed

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.5.13 |
| Language | Java 25 (virtual threads enabled) |
| Security | Spring Security + JJWT 0.13 |
| Database | PostgreSQL 17 via Spring Data JPA (Hibernate) |
| Real-time | Spring WebSocket (STOMP, no SockJS) |
| Email | Spring Mail + Thymeleaf HTML templates |
| Rate Limiting | Bucket4j |
| Build tool | Maven (mvnw wrapper included) |
| Container | Eclipse Temurin base image |

---

## ✅ Prerequisites

| Tool | Version |
|---|---|
| Java (JDK) | 25+ |
| Maven | 3.9+ (or use `./mvnw`) |
| PostgreSQL | 17+ |
| Docker & Docker Compose | 24+ / 2+ |
| mediasoup service | Running on `http://localhost:3000` (for voice features) |

---

## 🚀 Setup Guide

### 1. Clone the repository

```bash
git clone https://github.com/ACHRAF-YOUSSEF/bizcord.git
cd bizcord/bizcord-backtend
```

### 2. Start PostgreSQL

```bash
# Using Docker
docker run -d \
  --name bizcord-postgres \
  -e POSTGRES_USER=admin \
  -e POSTGRES_PASSWORD=admin \
  -e POSTGRES_DB=bizcord \
  -p 5432:5432 \
  postgres:17-alpine
```

### 3. Run in development mode

The `dev` profile uses `localhost:5432` and `APP_COOKIE_SECURE=false` by default.

```bash
./mvnw spring-boot:run
```

### 4. Run with Docker Compose (full stack)

See the [root README](../README.md) for the full stack setup.

---

## 🔑 Environment Variables

Set these when running with `SPRING_PROFILES_ACTIVE=prod`.

| Variable | Required | Default | Description |
|---|---|---|---|
| `SPRING_PROFILES_ACTIVE` | Yes | `dev` | Use `prod` in Docker |
| `DB_URL` | Yes (prod) | — | JDBC URL, e.g. `jdbc:postgresql://postgres:5432/bizcord` |
| `DB_USERNAME` | Yes (prod) | — | PostgreSQL username |
| `DB_PASSWORD` | Yes (prod) | — | PostgreSQL password |
| `APP_JWT_SECRET_KEY` | Yes (prod) | `bizcord-secret-key` | HS256 signing key — use a strong random value |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | Yes (prod) | `https://bizcord.achrafyoussef.tech` | Comma-separated origins |
| `APP_COOKIE_SECURE` | No | `true` | `false` when serving over plain HTTP |
| `APP_MEDIASOUP_API_SECRET` | No | `bizcord-mediasoup-secret` | Shared secret with the mediasoup service |
| `APP_MEDIASOUP_URL` | No | `http://mediasoup:3000` | Internal URL of the mediasoup container |
| `APP_STORAGE_PUBLIC_BASE_PATH` | No | `/api/uploads` | Relative URL prefix stored in API responses |
| `APP_STORAGE_MINIO_ENDPOINT` | Yes | `http://minio:9000` | MinIO/S3-compatible endpoint |
| `APP_STORAGE_MINIO_ACCESS_KEY` | Yes | `minioadmin` | MinIO access key |
| `APP_STORAGE_MINIO_SECRET_KEY` | Yes | `minioadmin` | MinIO secret key |
| `APP_STORAGE_MINIO_BUCKET` | No | `bizcord` | Bucket name used for BizCord uploads |
| `APP_STORAGE_MINIO_SECURE` | No | `false` | Whether to use secure MinIO endpoint wiring |
| `MAIL_HOST` | Yes (prod) | `localhost` | SMTP server hostname |
| `MAIL_PORT` | No | `1025` | SMTP server port |
| `MAIL_USERNAME` | No | — | SMTP username |
| `MAIL_PASSWORD` | No | — | SMTP password |
| `MAIL_SMTP_AUTH` | No | `false` | Enable SMTP authentication |
| `MAIL_SMTP_STARTTLS` | No | `false` | Enable STARTTLS |
| `MAIL_FROM` | No | `noreply@bizcord.achrafyoussef.tech` | Sender address for outgoing emails |
| `APP_FRONTEND_URL` | No | `https://bizcord.achrafyoussef.tech` | Base URL embedded in email links |

---

## 📡 API Overview

| Domain | Base Path |
|---|---|
| Authentication | `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout`, `GET /api/auth/verify`, `POST /api/auth/resend-verification`, `POST /api/auth/forgot-password`, `POST /api/auth/reset-password` |
| Users | `GET/PATCH /api/users/me`, `PATCH /api/users/me/avatar` |
| Servers | `CRUD /api/servers` |
| Channels | `CRUD /api/channels` |
| Channel Categories | `CRUD /api/channel-categories` |
| Members | `GET/DELETE /api/servers/{id}/members` |
| Messages | `CRUD /api/channels/{id}/messages` |
| Direct Messages | `CRUD /api/conversations` |
| Reactions | `POST/DELETE /api/messages/{id}/reactions` |
| Search | `GET /api/search/messages` |
| Invites | `POST/GET /api/invites` |
| Uploads | `POST /api/uploads` |
| Voice | `POST/DELETE /api/voice/rooms` |
| Notifications | `GET /api/notifications` |

WebSocket endpoint: `ws://host/ws` — connect with STOMP, sending `Authorization: Bearer <token>` in the CONNECT frame.

---

## 🧪 Running Tests

```bash
./mvnw test
```

Test reports are written to `target/surefire-reports/`.

---

## 🐳 Docker

```bash
# Build
docker build -t achrafyoussef/bizcord-backend:latest .

# Run standalone (dev profile, local DB)
docker run -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=dev \
  achrafyoussef/bizcord-backend:latest
```

See [DOCKER_HUB.md](./DOCKER_HUB.md) for the full Docker Hub description and all environment variables.

---

## 👨‍💻 Author

**Achraf Youssef**

[![GitHub](https://img.shields.io/badge/GitHub-ACHRAF--YOUSSEF-181717?logo=github)](https://github.com/ACHRAF-YOUSSEF)
[![Portfolio](https://img.shields.io/badge/Portfolio-achraf--youssef.github.io-0A66C2)](https://achraf-youssef.github.io/portfolio/)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-achraf--youssef-0077B5?logo=linkedin)](https://www.linkedin.com/in/achraf-youssef/)
