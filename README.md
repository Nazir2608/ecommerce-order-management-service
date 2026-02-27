# 🛒 Ecommerce Order Management Service — Production-Ready Order Management System

> A full-featured, production-grade Order Management System built with **Spring Boot 3**, **Java 21**, and modern best practices. Ideal for showcasing production Java backend experience.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

## 📋 Table of Contents

- [Features](#-features)
- [Tech Stack](#-tech-stack)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
- [API Documentation](#-api-documentation)
- [API Endpoints](#-api-endpoints)
- [Database Schema](#-database-schema)
- [Security](#-security)
- [Configuration](#-configuration)
- [Testing](#-testing)
- [Contributing](#-contributing)

---

## ✨ Features

| Module | Features |
|--------|----------|
| **Auth** | JWT login/register, refresh tokens, logout with Redis blacklist, role-based access (CUSTOMER / ADMIN) |
| **Products** | CRUD, categories, Redis caching, pagination & filtering by price/category/search, low-stock alerts |
| **Cart** | Add/update/remove items, coupon/discount system, dynamic shipping calculation |
| **Orders** | Place orders, order status state machine, stock management with pessimistic locking, order history |
| **Payments** | Stripe integration, webhook handling, COD support, refunds, mock mode for development |
| **Notifications** | Async email (Thymeleaf templates) + SMS (Twilio), retry logic, notification audit log |
| **Admin** | User management, analytics dashboard (revenue, top products, sales by period), all order management |

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 17 |
| Database | PostgreSQL 15 |
| Cache | Redis 7 |
| Migrations | Flyway |
| Security | Spring Security + JWT (jjwt) |
| Payments | Stripe Java SDK |
| Notifications | Spring Mail (JavaMail) + Twilio SDK |
| Email Templates | Thymeleaf |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Object Mapping | MapStruct |
| Validation | Jakarta Bean Validation |
| Testing | JUnit 5 + Mockito + Testcontainers |
| Containerization | Docker + Docker Compose |
| Monitoring | Spring Actuator |
| Build | Maven 3.9+ |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client / Frontend                        │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTPS
┌─────────────────────▼───────────────────────────────────────┐
│                   Spring Boot App (8080)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  Controllers │  │   Services   │  │   Repositories    │  │
│  │  (REST API)  │→ │ (Biz Logic)  │→ │  (JPA / Flyway)   │  │
│  └──────────────┘  └──────┬───────┘  └─────────┬─────────┘  │
│                           │                    │             │
│              ┌────────────▼────────┐  ┌────────▼──────────┐ │
│              │   Redis (Cache +    │  │   PostgreSQL 15    │ │
│              │   Token Store)      │  │   (Primary DB)     │ │
│              └─────────────────────┘  └───────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │         Async Notification Layer (@Async + Events)     │  │
│  │         Email (MailHog/SMTP)  |  SMS (Twilio)          │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure (Java package root: `com.nazir.orderservice`)

```
ecommerce-order-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/nazir/orderservice/
│   │   │   ├── OrderFlowApplication.java       # Entry point
│   │   │   ├── config/                         # Security, Redis, JPA, Swagger, AppProperties
│   │   │   ├── controller/                     # REST Controllers
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── CartController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   └── admin/                      # Admin-only endpoints
│   │   │   │       ├── AdminOrderController.java
│   │   │   │       ├── AdminProductController.java
│   │   │   │       ├── AdminUserController.java
│   │   │   │       └── AdminAnalyticsController.java
│   │   │   ├── service/                        # Service interfaces
│   │   │   │   └── impl/                       # Service implementations
│   │   │   ├── repository/                     # JPA Repositories
│   │   │   ├── entity/                         # JPA Entities
│   │   │   ├── dto/
│   │   │   │   ├── request/                    # Incoming request DTOs
│   │   │   │   └── response/                   # Outgoing response DTOs
│   │   │   ├── mapper/                         # MapStruct mappers
│   │   │   ├── exception/                      # Custom exceptions + GlobalExceptionHandler
│   │   │   ├── security/                       # JWT filter, UserDetailsService, JwtUtil
│   │   │   ├── enums/                          # Role, OrderStatus, PaymentStatus, etc.
│   │   │   ├── event/                          # Spring Application Events
│   │   │   └── util/                           # SecurityUtils, OrderStateMachine, etc.
│   │   └── resources/
│   │       ├── application.yml                 # Main config
│   │       ├── application-dev.yml             # Dev overrides (MailHog, debug logs)
│   │       ├── application-prod.yml            # Prod overrides
│   │       ├── templates/email/                # Thymeleaf email templates
│   │       └── db/migration/                   # Flyway SQL migrations (V1–V10)
│   └── test/
│       └── java/com/nazir/orderservice/
│           ├── unit/service/                   # Unit tests (Mockito)
│           └── integration/                    # Integration tests (Testcontainers)
├── docker/
│   ├── Dockerfile                              # Multi-stage Docker build
│   └── docker-compose.yml                      # App + Postgres + Redis + MailHog
├── .github/
│   └── workflows/
│       └── ci.yml                              # GitHub Actions CI pipeline
├── .gitignore
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Docker** & **Docker Compose** (recommended)
- **Java 17** (for local development without Docker)
- **Maven 3.9+**

### ▶️ Run with Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/ecommerce-order-management-service.git
cd ecommerce-order-management-service

# 2. Start all services (App + Postgres + Redis + MailHog)
docker-compose -f docker/docker-compose.yml up --build

# 3. Access the app
# API:       http://localhost:8080
# Swagger:   http://localhost:8080/swagger-ui.html
# MailHog:   http://localhost:8025  (view dev emails)
# Actuator:  http://localhost:8080/actuator/health
```

### ▶️ Run Locally (Dev, without Docker)

```bash
# 1. Build with Java 21
mvn -Dmaven.test.skip=true clean package

# 2. Run with H2 (dev profile uses in-memory DB)
mvn -Dmaven.test.skip=true spring-boot:run -Dspring-boot.run.profiles=dev

# Health check
open http://localhost:8080/actuator/health
```

### ▶️ Build Production JAR

```bash
mvn clean package -DskipTests
java -jar target/ecommerce-order-management-service-1.0.0-SNAPSHOT.jar --spring.profiles.active=prod
```

---

## 📖 API Documentation

Once the app is running, access interactive API docs at:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

---

## 🔗 API Endpoints

### 🔐 Auth (`/api/v1/auth`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | ❌ | Register new user |
| POST | `/login` | ❌ | Login, returns JWT |
| POST | `/refresh` | ❌ | Refresh access token |
| POST | `/logout` | ✅ | Logout, blacklists token |

### 👤 User (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/me` | ✅ | Get current user profile |
| PUT | `/me` | ✅ | Update profile |
| POST | `/me/addresses` | ✅ | Add address |
| GET | `/me/addresses` | ✅ | List addresses |
| PUT | `/me/addresses/{id}` | ✅ | Update address |
| DELETE | `/me/addresses/{id}` | ✅ | Delete address |
| PATCH | `/me/addresses/{id}/default` | ✅ | Set default address |

### 📦 Products (`/api/v1/products`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | ❌ | List products (with filters & pagination) |
| GET | `/{id}` | ❌ | Get product details |
| GET | `/categories` | ❌ | List categories |

### 🛒 Cart (`/api/v1/cart`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | ✅ | View cart with totals |
| POST | `/items` | ✅ | Add item to cart |
| PUT | `/items/{id}` | ✅ | Update item quantity |
| DELETE | `/items/{id}` | ✅ | Remove item |
| DELETE | `/` | ✅ | Clear cart |
| POST | `/coupon` | ✅ | Apply coupon |
| DELETE | `/coupon` | ✅ | Remove coupon |

### 📬 Orders (`/api/v1/orders`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/` | ✅ | Place order |
| GET | `/` | ✅ | My orders (paginated) |
| GET | `/{id}` | ✅ | Order detail |
| POST | `/{id}/cancel` | ✅ | Cancel order |

### 💳 Payments (`/api/v1/payments`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/initiate` | ✅ | Initiate payment (Stripe) |
| POST | `/webhook` | ❌ | Stripe webhook |
| POST | `/confirm` | ✅ | Confirm payment (mock/dev) |
| GET | `/order/{orderId}` | ✅ | Payment details |

### 🔧 Admin (`/api/v1/admin`) — ADMIN role required
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/orders` | All orders (filterable) |
| PATCH | `/orders/{id}/status` | Update order status |
| POST | `/products` | Create product |
| PUT | `/products/{id}` | Update product |
| DELETE | `/products/{id}` | Soft delete product |
| PATCH | `/products/{id}/stock` | Update stock |
| GET | `/products/low-stock` | Products with low stock |
| POST | `/categories` | Create category |
| GET | `/users` | All users (paginated) |
| GET | `/users/{id}` | User with order history |
| PATCH | `/users/{id}/deactivate` | Deactivate user |
| PATCH | `/users/{id}/activate` | Activate user |
| GET | `/analytics/sales` | Sales summary |
| GET | `/analytics/revenue` | Revenue over time |
| GET | `/analytics/products/top` | Top products |

---

## 🗄 Database Schema

```
users ──────────────┬── addresses
  │                  └── cart ──── cart_items ──── products
  │                                                    │
  └── orders ────── order_items ──────────────────── (ref)
        │
        ├── order_status_history
        └── payments

categories ──── products

notifications
coupons
```

### Order Status Flow
```
PENDING ──→ CONFIRMED ──→ PROCESSING ──→ SHIPPED ──→ DELIVERED ──→ REFUNDED
   │              │
   └──────────────┴──→ CANCELLED
```

---

## 🔐 Security

- **JWT Authentication** — stateless tokens stored in Authorization header
- **Refresh Token Rotation** — stored in Redis, single-use
- **Token Blacklisting** — logout invalidates token in Redis
- **BCrypt Password Hashing** — strength 12
- **Role-Based Access Control** — `@PreAuthorize` on controller methods
- **Rate Limiting** — Redis-based, 100 req/min per IP
- **CORS** — configurable per environment

---

## ⚙️ Configuration

All config is environment-variable driven. Key variables:

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/orderservice` | Database URL |
| `DB_USERNAME` | `orderservice` | DB username |
| `DB_PASSWORD` | `orderservice` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `JWT_SECRET` | *(see application.yml)* | Base64 JWT signing key |
| `STRIPE_SECRET_KEY` | `sk_test_...` | Stripe API key |
| `STRIPE_WEBHOOK_SECRET` | `whsec_...` | Stripe webhook signing secret |
| `TWILIO_ENABLED` | `false` | Enable/disable real SMS |
| `MAIL_HOST` | `localhost` | SMTP host |

---

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run only unit tests
mvn test -Dgroups=unit

# Run only integration tests (requires Docker)
mvn test -Dgroups=integration

# Run with coverage report
mvn verify
# Coverage report: target/site/jacoco/index.html
```

### Test Structure
- **Unit Tests** — `src/test/java/.../unit/` — all service logic tested with Mockito
- **Integration Tests** — `src/test/java/.../integration/` — full API tests with Testcontainers (real PostgreSQL + Redis)

---

## 🤝 Contributing

1. Fork the project
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes: `git commit -m 'feat: add amazing feature'`
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Commit Convention
```
feat:     New feature
fix:      Bug fix
docs:     Documentation changes
refactor: Code refactor
test:     Adding tests
chore:    Build/tooling changes
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

Built as a production-ready portfolio project demonstrating Java/Spring Boot expertise.
