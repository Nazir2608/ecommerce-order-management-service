# 🛒 OrderFlow — Production-Ready Order Management System

> A full-featured, production-grade Order Management System built with **Spring Boot 3**, **Java 21**, and modern best practices. Covers the complete e-commerce backend lifecycle: auth → cart → orders → payments → notifications.

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-blue)](https://www.postgresql.org/)
[![Redis](https://img.shields.io/badge/Redis-7-red)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-Compose-blue)](https://www.docker.com/)
[![Stripe](https://img.shields.io/badge/Stripe-Integrated-purple)](https://stripe.com/)
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
- [Email Notifications](#-email-notifications)
- [Stripe Payments](#-stripe-payments)
- [Contributing](#-contributing)

---

## ✨ Features

| Module | Features |
|--------|----------|
| **Auth** | JWT login/register, refresh tokens, logout with Redis blacklist, role-based access (CUSTOMER / ADMIN) |
| **Products** | CRUD, categories, pagination & filtering by price/category/search, low-stock alerts, pessimistic locking for stock |
| **Cart** | Add/update/remove items, auto-creates cart on first add, coupon/discount system (flat & percentage), dynamic shipping calculation, stock validation |
| **Orders** | Place orders from cart, order status state machine, stock deduction with pessimistic locking, order history, cancellation with stock restore |
| **Payments** | Real Stripe integration (PaymentIntent), webhook handling (signature verified), refunds via Stripe API |
| **Notifications** | Async email via Thymeleaf HTML templates + SMS (Twilio mock), retry logic (3 attempts), full notification audit log |
| **Admin** | User management (activate/deactivate), all order management, status transitions, product & category CRUD |

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 3.2, Java 21 |
| Database | PostgreSQL 15 |
| Cache / Token Store | Redis 7 |
| Migrations | Flyway |
| Security | Spring Security + JWT (jjwt) |
| Payments | Stripe Java SDK v25 |
| Notifications | Spring Mail (JavaMail) + Twilio |
| Email Templates | Thymeleaf |
| API Docs | SpringDoc OpenAPI 3 / Swagger UI |
| Validation | Jakarta Bean Validation |
| Containerization | Docker + Docker Compose |
| Monitoring | Spring Boot Actuator |
| Build | Maven 3.9+ |

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Client / Frontend                        │
└─────────────────────┬───────────────────────────────────────┘
                      │ HTTP/HTTPS
┌─────────────────────▼───────────────────────────────────────┐
│                   Spring Boot App (:8080)                    │
│  ┌──────────────┐  ┌──────────────┐  ┌───────────────────┐  │
│  │  Controllers │→ │   Services   │→ │   Repositories    │  │
│  │  (REST API)  │  │ (Biz Logic)  │  │  (JPA + Flyway)   │  │
│  └──────────────┘  └──────┬───────┘  └─────────┬─────────┘  │
│                           │                    │             │
│              ┌────────────▼──────┐   ┌─────────▼─────────┐  │
│              │  Redis            │   │  PostgreSQL 15     │  │
│              │  - JWT blacklist  │   │  - All entities    │  │
│              │  - Refresh tokens │   │  - Flyway V1-V10   │  │
│              └───────────────────┘   └───────────────────┘  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │     Async Notification Layer  (@Async)                 │  │
│  │     Thymeleaf HTML Email  |  Twilio SMS (mock)         │  │
│  └────────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐  │
│  │     Stripe Webhook  (/api/v1/payments/webhook/stripe)  │  │
│  │     Signature verified → auto-confirms payment         │  │
│  └────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

---

## 📁 Project Structure

```
ecommerce-order-management-service/
├── src/
│   ├── main/
│   │   ├── java/com/nazir/orderservice/
│   │   │   ├── EcommerceOrderManagementServiceApplication.java
│   │   │   ├── config/              # Security, Redis, Async, Swagger, AppProperties
│   │   │   ├── controller/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── CartController.java
│   │   │   │   ├── CategoryController.java
│   │   │   │   ├── OrderController.java
│   │   │   │   ├── PaymentController.java
│   │   │   │   ├── ProductController.java
│   │   │   │   ├── UserController.java
│   │   │   │   └── admin/
│   │   │   │       ├── AdminCouponController.java
│   │   │   │       ├── AdminOrderController.java
│   │   │   │       ├── AdminProductController.java
│   │   │   │       └── AdminUserController.java
│   │   │   ├── service/
│   │   │   │   └── impl/
│   │   │   │       ├── AuthServiceImpl.java
│   │   │   │       ├── CartServiceImpl.java
│   │   │   │       ├── CategoryServiceImpl.java
│   │   │   │       ├── NotificationServiceImpl.java
│   │   │   │       ├── OrderServiceImpl.java
│   │   │   │       ├── PaymentServiceImpl.java
│   │   │   │       ├── ProductServiceImpl.java
│   │   │   │       └── UserServiceImpl.java
│   │   │   ├── repository/          # JPA Repositories (JpaSpecificationExecutor on Product)
│   │   │   ├── entity/              # JPA Entities (BaseEntity with UUID + audit fields)
│   │   │   ├── dto/
│   │   │   │   ├── request/
│   │   │   │   └── response/
│   │   │   ├── exception/           # Custom exceptions + GlobalExceptionHandler
│   │   │   ├── security/            # JwtFilter, JwtUtil, UserDetailsServiceImpl
│   │   │   ├── enums/               # Role, OrderStatus, PaymentStatus, DiscountType, etc.
│   │   │   └── util/                # ProductSpecification, RequestLoggingFilter
│   │   └── resources/
│   │       ├── application.yml           # Base config (JWT, Stripe, Twilio, Swagger)
│   │       ├── application-dev.yml       # Dev: localhost Postgres/Redis, Mailpit, DEBUG logs
│   │       ├── application-prod.yml      # Prod: env vars, WARN logs, file appender
│   │       ├── templates/email/          # Thymeleaf HTML email templates
│   │       │   ├── welcome.html
│   │       │   ├── order-placed.html
│   │       │   ├── payment-success.html
│   │       │   ├── payment-failed.html
│   │       │   ├── order-status-update.html
│   │       │   ├── order-cancelled.html
│   │       │   └── refund-processed.html
│   │       └── db/migration/             # Flyway migrations V1–V10
├── docker/
│   ├── Dockerfile                        # Multi-stage build (Eclipse Temurin 21 JRE Alpine)
│   ├── docker-compose.yml                # App + Postgres + Redis + Mailpit
│   └── .env.example                      # Template for secrets (never commit .env)
├── .github/workflows/ci.yml              # GitHub Actions CI
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

- **Docker & Docker Compose** (recommended)
- **Java 21** (for local dev without Docker)
- **Maven 3.9+**
- **Stripe account** (free test account at https://dashboard.stripe.com)

---

### ▶️ Run with Docker Compose (Recommended)

```bash
# 1. Clone the repository
git clone https://github.com/Nazir2608/ecommerce-order-management-service.git
cd ecommerce-order-management-service

# 2. Create your secrets file (never commit this)
cp docker/.env.example docker/.env
# Edit docker/.env and fill in your Stripe keys

# 3. Start all services
cd docker
docker compose up --build

# 4. Access
# API:       http://localhost:8080
# Swagger:   http://localhost:8080/swagger-ui.html
# Mailpit:   http://localhost:8025   (view all sent emails)
# Actuator:  http://localhost:8080/actuator/health
```

**Default seeded accounts:**
| Role | Email | Password |
|------|-------|----------|
| ADMIN | admin@orderflow.com | admin123 |

---

### ▶️ Run Locally (Dev Profile)

```bash
# Requires: PostgreSQL on localhost:5432 and Redis on localhost:6379

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

---

### ▶️ Build Production JAR

```bash
mvn clean package -DskipTests
java -jar target/*.jar --spring.profiles.active=prod
```

---

## 📖 API Documentation

Once running, full interactive docs are at:

- **Swagger UI:** http://localhost:8080/swagger-ui.html
- **OpenAPI JSON:** http://localhost:8080/api-docs

Import the Postman collection (`OrderFlow_API_Collection.postman_collection.json`) for pre-built requests with auto-saved tokens and IDs.

---

## 🔗 API Endpoints

### 🔐 Auth (`/api/v1/auth`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/register` | ❌ | Register new customer |
| POST | `/login` | ❌ | Login — returns `accessToken` + `refreshToken` |
| POST | `/refresh` | ❌ | Refresh access token |
| POST | `/logout` | ✅ | Logout — blacklists token in Redis |

### 👤 User Profile (`/api/v1/users`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/me` | ✅ | Profile with all addresses |
| PUT | `/me` | ✅ | Update name / phone |
| GET | `/me/orders` | ✅ | My order history (paginated) |
| POST | `/me/addresses` | ✅ | Add address (first = default) |
| GET | `/me/addresses` | ✅ | List all addresses |
| PUT | `/me/addresses/{id}` | ✅ | Update address |
| PATCH | `/me/addresses/{id}/default` | ✅ | Set as default |
| DELETE | `/me/addresses/{id}` | ✅ | Delete address |

### 📦 Categories (`/api/v1/categories`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | ❌ | List with optional `?search=` and pagination |
| GET | `/{id}` | ❌ | Get by ID |
| POST | `/api/v1/admin/categories` | ADMIN | Create |
| PUT | `/api/v1/admin/categories/{id}` | ADMIN | Update |
| DELETE | `/api/v1/admin/categories/{id}` | ADMIN | Soft delete |

**Seeded categories:** Electronics, Clothing, Home & Kitchen, Books, Sports, Beauty, Toys, Grocery

### 🛍️ Products (`/api/v1/products`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | ❌ | Filter by `?search=`, `?categoryId=`, `?minPrice=`, `?maxPrice=`, paginated |
| GET | `/{id}` | ❌ | Product detail |
| POST | `/api/v1/admin/products` | ADMIN | Create |
| PUT | `/api/v1/admin/products/{id}` | ADMIN | Update |
| PATCH | `/api/v1/admin/products/{id}/stock` | ADMIN | Update stock |
| GET | `/api/v1/admin/products/low-stock` | ADMIN | Products below threshold |
| DELETE | `/api/v1/admin/products/{id}` | ADMIN | Soft delete |

### 🛒 Cart (`/api/v1/cart`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| GET | `/` | ✅ | View cart with calculated totals |
| POST | `/items` | ✅ | Add item — auto-increments if already in cart |
| PUT | `/items/{id}` | ✅ | Update quantity (0 = remove) |
| DELETE | `/items/{id}` | ✅ | Remove item |
| DELETE | `/` | ✅ | Clear entire cart |
| POST | `/coupon` | ✅ | Apply coupon code |
| DELETE | `/coupon` | ✅ | Remove applied coupon |

### 📬 Orders (`/api/v1/orders`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/` | ✅ | Place order from cart (deducts stock, creates payment record) |
| GET | `/` | ✅ | My orders — filter by `?status=` |
| GET | `/{id}` | ✅ | Order detail with items and status history |
| POST | `/{id}/cancel` | ✅ | Cancel (restores stock) |

### 💳 Payments (`/api/v1/payments`)
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| POST | `/initiate?orderId=` | ✅ | Creates Stripe PaymentIntent — returns `clientSecret` |
| POST | `/confirm` | ✅ | Manual confirm for dev/testing |
| GET | `/order/{orderId}` | ✅ | Payment status for order |
| POST | `/webhook/stripe` | ❌ | Stripe webhook (signature verified) |
| POST | `/api/v1/admin/payments/{id}/refund` | ADMIN | Process refund via Stripe |

### 🔧 Admin — Orders (`/api/v1/admin/orders`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | All orders — filter by `?status=`, `?from=`, `?to=` |
| PATCH | `/{id}/status` | Update status with reason |

**Valid status transitions:**
```
PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → REFUNDED
PENDING → CANCELLED
CONFIRMED → CANCELLED
```

### 🔧 Admin — Users (`/api/v1/admin/users`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/` | All users (paginated) |
| GET | `/{id}` | User detail |
| PATCH | `/{id}/deactivate` | Deactivate account |
| PATCH | `/{id}/activate` | Activate account |

---

## 🗄 Database Schema

```
users ──────────┬── addresses
                └── cart ──── cart_items ──── products ──── categories
                └── orders ── order_items ─── (product ref)
                      │
                      ├── order_status_history
                      └── payments

notifications
coupons
flyway_schema_history  (migration tracking)
```

Managed by **Flyway** (migrations V1–V10). Schema is never auto-created — `ddl-auto: validate` in all profiles.

---

## 🔐 Security

- **JWT Authentication** — stateless, passed as `Authorization: Bearer <token>`
- **Refresh Token Rotation** — stored in Redis, single-use, 7-day expiry
- **Token Blacklisting** — logout stores token in Redis until expiry
- **BCrypt** — password hashing strength 12
- **Role-Based Access** — `CUSTOMER` vs `ADMIN` via `@PreAuthorize`
- **Pessimistic Locking** — `SELECT FOR UPDATE` on products during order placement to prevent overselling
- **Stripe Webhook Verification** — `Webhook.constructEvent()` validates `Stripe-Signature` header

---

## ⚙️ Configuration

All production config comes from environment variables. Create `docker/.env` from the template:

```bash
cp docker/.env.example docker/.env
```

| Variable | Description |
|----------|-------------|
| `DB_URL` | PostgreSQL JDBC URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `REDIS_HOST` | Redis hostname |
| `REDIS_PORT` | Redis port (default 6379) |
| `JWT_SECRET` | Base64 JWT signing secret |
| `STRIPE_SECRET_KEY` | Stripe secret key (`sk_test_...` or `sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook signing secret (`whsec_...`) |
| `MAIL_HOST` | SMTP hostname |
| `MAIL_PORT` | SMTP port |
| `TWILIO_ENABLED` | `true` / `false` |
---

## 📧 Email Notifications

All emails are sent **asynchronously** via `@Async` and rendered with **Thymeleaf HTML templates**.

| Event | Template | Variables |
|-------|----------|-----------|
| User registered | `welcome.html` | `name`, `email`, `joinedAt` |
| Order placed | `order-placed.html` | `name`, `orderNumber`, `items`, `totalAmount`, `discountAmount`, `shippingAmount`, `finalAmount`, `shippingAddress`, `placedAt` |
| Payment success | `payment-success.html` | `name`, `orderNumber`, `amount`, `currency`, `paymentMethod`, `transactionId`, `paidAt`, `items`, `shippingAddress` |
| Payment failed | `payment-failed.html` | `name`, `orderNumber`, `amount`, `failedAt` |
| Order status update | `order-status-update.html` | `name`, `orderNumber`, `status`, `updatedAt` |
| Order cancelled | `order-cancelled.html` | `name`, `orderNumber`, `amount`, `items`, `cancelledAt` |
| Refund processed | `refund-processed.html` | `name`, `orderNumber`, `amount`, `transactionId`, `refundedAt` |

In dev, emails are captured by **Mailpit** at http://localhost:8025 — no real email is sent.

---

## 💳 Stripe Payments

### Flow
```
POST /payments/initiate → Stripe creates PaymentIntent → returns clientSecret
        ↓
Frontend uses clientSecret with Stripe.js to collect card details
        ↓
Stripe processes payment → calls webhook → payment_intent.succeeded
        ↓
App auto-confirms: paymentStatus = SUCCESS, orderStatus = CONFIRMED
```

### Dev Testing (No Frontend)
```bash
# 1. Forward webhooks to local app
stripe listen --forward-to http://localhost:8080/api/v1/payments/webhook/stripe

# 2. Confirm payment using real PaymentIntent ID
stripe payment_intents confirm pi_xxx --payment-method pm_card_visa

# OR use Postman /confirm endpoint directly for quick testing
```

### Stripe Test Cards
| Card Number | Result |
|-------------|--------|
| `4242 4242 4242 4242` | ✅ Success |
| `4000 0000 0000 0002` | ❌ Declined |
| `4000 0025 0000 3155` | 🔐 3D Secure required |

---

## 🤝 Contributing

1. Fork the project
2. Create your feature branch: `git checkout -b feature/amazing-feature`
3. Commit your changes using conventional commits
4. Push to the branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

### Commit Convention
```
feat:     New feature
fix:      Bug fix
docs:     Documentation
refactor: Code refactor
test:     Tests
chore:    Build / tooling
```

---

## 📄 License

This project is licensed under the MIT License — see the [LICENSE](LICENSE) file for details.

---

## 👨‍💻 Author

Built as a production-ready portfolio project demonstrating Java 21 / Spring Boot 3 backend expertise.

> Stack: Java 21 · Spring Boot 3 · PostgreSQL · Redis · Stripe · Docker · JWT · Flyway · Thymeleaf