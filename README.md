# University Management System

A production-ready University Management application built with **Spring Boot 4**, **Java 21**, **Spring Security (JWT)**, **PostgreSQL 16**, and **Docker**.

Includes a complete RESTful API, stateless JWT authentication with token revocation, automated administrator bootstrapping, and a clean web dashboard for managing university accounts.

---

## Features

- **Role-Based Access Control (RBAC)**: Supports `ADMIN`, `TEACHER`, and `STUDENT` roles with granular permission checks.
- **Stateless JWT Security**:
  - Secure HS256 JWT tokens with configurable expiration.
  - Server-side token revocation / logout blacklist persisted in PostgreSQL.
  - BCrypt password hashing.
- **Admin Bootstrapping**: Automatically initializes a default administrator account upon application startup if one does not exist.
- **Built-in Web Dashboard**: Modern responsive Single Page Interface served directly by Spring Boot at `/`.
- **Database & Persistence**:
  - PostgreSQL 16 with Spring Data JPA & Hibernate 7.
  - Dedicated Flyway/Hibernate DDL auto-updates.
- **Docker Ready**:
  - Multi-stage `Dockerfile` with non-root security execution (`USER 10001:10001`).
  - `compose.yaml` with health checks, persistent volumes, and custom port bindings.
- **Testing & Quality**:
  - Automated unit and integration tests using an isolated in-memory H2 database.
  - Postman collection and Newman runner scripts included.

---

## Architecture & Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Framework** | Spring Boot 4.1.1 |
| **Language** | Java 21 (Eclipse Temurin) |
| **Security** | Spring Security 6 / JJWT (0.13.0) |
| **ORM / Data** | Spring Data JPA / Hibernate 7 |
| **Database** | PostgreSQL 16 Alpine |
| **Containerization** | Docker & Docker Compose |
| **Frontend** | Vanilla HTML5 / Modern CSS / JavaScript (ES6+) |
| **Build Tool** | Apache Maven 3.9 Wrapper |

---

## Getting Started

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (recommended for containerized execution)
- Or [Java 21 JDK](https://adoptium.net/) & [PostgreSQL](https://www.postgresql.org/) for local execution.

---

### Running with Docker (Recommended)

1. Clone the repository:
   ```bash
   git clone https://github.com/rahi-sadat/University_Management_SpringBoot.git
   cd University_Management_SpringBoot
   ```

2. Configure environment variables (a pre-configured `.env.example` is provided):
   ```bash
   cp .env.example .env
   ```

3. Start the application and database containers:
   ```bash
   docker compose up --build -d
   ```

4. Verify running containers:
   ```bash
   docker compose ps
   ```

5. Access the application:
   - **Web UI:** [http://localhost:18080](http://localhost:18080)
   - **PostgreSQL Database:** `localhost:5433` (`university_management`)

---

### Default Credentials

| Field | Default Value |
| :--- | :--- |
| **Email** | `admin@university.local` |
| **Password** | `ChangeThis_Admin_123!` |
| **Role** | `ADMIN` |

---

### Running Locally (Without Docker)

1. Ensure a PostgreSQL instance is running on port `5432` with database `university_management`.
2. Configure environment variables in `.env` or your terminal session.
3. Build and launch:
   ```bash
   ./mvnw spring-boot:run
   ```
4. Access the application at `http://localhost:8081`.

---

## API Reference

### Authentication

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticate with email and password; returns JWT token. | Public |
| `POST` | `/api/auth/logout` | Revoke the active JWT token. | Authenticated |

### Administrator Endpoints (`/api/admins`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/admins` | List all administrator accounts. | `ADMIN` |
| `POST` | `/api/admins` | Create a new administrator account. | `ADMIN` |
| `PUT` | `/api/admins/{id}` | Update an administrator account. | `ADMIN` |
| `DELETE` | `/api/admins/{id}` | Delete an administrator account. | `ADMIN` |

### Teacher Endpoints (`/api/teachers`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/teachers` | List all teacher accounts. | `ADMIN`, `TEACHER` |
| `POST` | `/api/teachers` | Create a new teacher account. | `ADMIN` |
| `PUT` | `/api/teachers/{id}` | Update a teacher account. | `ADMIN` |
| `DELETE` | `/api/teachers/{id}` | Delete a teacher account. | `ADMIN` |

### Student Endpoints (`/api/students`)

| Method | Endpoint | Description | Access |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/students` | List all student accounts. | `ADMIN`, `TEACHER`, `STUDENT` |
| `POST` | `/api/students` | Create a new student account. | `ADMIN` |
| `PUT` | `/api/students/{id}` | Update a student account. | `ADMIN` |
| `DELETE` | `/api/students/{id}` | Delete a student account. | `ADMIN` |

---

## Testing

Run unit and integration tests with the Maven wrapper:
```bash
./mvnw clean test
```

Run Newman / Postman verification tests:
```bash
./mvnw verify -Dpostman=true
```

---

## License

This project is open source and available under the [MIT License](LICENSE).
