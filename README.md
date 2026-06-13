# Enterprise JWT Auth Server

A robust, production-ready authentication and authorization server built with **Spring Boot 3** and **Spring Security 6**. Designed with a focus on security, stateless scalability, and containerized deployment.

## 🚀 Features

*   **Stateless Authentication**: Implements JWT (JSON Web Tokens) with a 15-minute access token lifespan and a 7-day refresh token strategy with database-backed revocation.
*   **Production-Grade Security**: 
    *   Passwords protected with **BCrypt** hashing.
    *   Role-Based Access Control (RBAC) using `@PreAuthorize`.
    *   Credential enumeration protection (generic login error messages).
    *   No sensitive data exposed via global exception handling.
*   **Enterprise Architecture**:
    *   **Custom Handlers**: Dedicated `JwtAuthenticationEntryPoint` and `JwtAccessDeniedHandler` for clean JSON error responses (401/403).
    *   **Environment-Aware**: Configured via environment variables for 12-factor app compliance.
    *   **Safety Guards**: `@Profile("!prod")` prevents accidental seeding of test accounts in production environments.
*   **Infrastructure**:
    *   **Dockerized**: Multi-stage `Dockerfile` (Maven builder + Alpine JRE runtime) for minimal image size (~100MB).
    *   **Orchestration**: `docker-compose.yml` with automated MySQL health checks to ensure dependency readiness.

## 🛠 Tech Stack

*   **Language**: Java 21
*   **Framework**: Spring Boot 3 / Spring Security 6
*   **Database**: MySQL
*   **Containerization**: Docker & Docker Compose
*   **Build Tool**: Maven

## 📋 Prerequisites

*   [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running.
*   Java 21 installed (if running locally without Docker).

## 🚀 Quick Start (Docker)

1. **Clone the repository:**
```bash
   git clone [https://github.com/rohit-santraa/enterprise-jwt-auth-server.git](https://github.com/rohit-santraa/enterprise-jwt-auth-server.git)
   cd auth-server ```
Build and start the infrastructure:

