# Project: Enterprise JWT Authentication & Authorization Server

## 🎯 Project Overview
This is a secure, stateless backend authentication service acting as a central security layer. It handles user registration, login, JWT access/refresh token management, and Role-Based Access Control (RBAC). It is designed to be production-ready and enterprise-grade.

## 🛠️ Technology Stack
* **Language:** Java 21
* **Framework:** Spring Boot 3.x
* **Security:** Spring Security 6.x, JWT (JSON Web Tokens)
* **Persistence:** Spring Data JPA, Hibernate, MySQL
* **Build Tool:** Maven
* **Deployment:** Docker

## 🏗️ Architectural Rules (Strict Adherence Required)
You must strictly follow a layered architecture. Do not bypass layers.
1.  **Controller Layer (`/controller`):** Only handles HTTP request/response mapping and basic input validation. Must delegate all business logic to the Service layer.
2.  **Service Layer (`/service`):** Contains all core business, authentication, and token management logic. 
3.  **Repository Layer (`/repository`):** Standard Spring Data JPA interfaces. No business logic here.
4.  **Security Layer (`/security`):** Contains JWT filters, entry points, configuration, and custom user details implementations.
5.  **Entity Layer (`/entity`):** pure data mappings to MySQL tables.

## 🔒 Security & Implementation Constraints
When generating or modifying code, you must enforce the following:
* **Statelessness:** The application must be 100% stateless. Do NOT use `HttpSession`. Rely entirely on the `JwtAuthenticationFilter`.
* **Password Storage:** All passwords MUST be hashed using `BCryptPasswordEncoder` before hitting the database.
* **Token Lifecycle:** * Access Tokens should have a short lifespan (e.g., 15-30 mins).
    * Refresh Tokens should have a longer lifespan (e.g., 7 days) and must be persisted in the `refresh_tokens` MySQL table with revocation status (`revoked` boolean).
* **Authorization:** Use `@PreAuthorize` annotations on controllers to enforce Role-Based Access Control (RBAC) (e.g., `ROLE_USER`, `ROLE_ADMIN`).
* **Exception Handling:** Do not return raw stack traces to the client. Use a `@RestControllerAdvice` global exception handler to return structured, meaningful JSON error responses (e.g., 401 Unauthorized, 403 Forbidden, 400 Bad Request).

## 🧑‍💻 Coding Standards
* Follow standard Java naming conventions (camelCase for variables/methods, PascalCase for classes).
* Use standard RESTful route naming (e.g., `POST /api/auth/register`, `GET /api/users/profile`).
* Favour Constructor Injection over `@Autowired` field injection for better testability.
* Keep methods small and focused on a single responsibility.

## 🤖 Agent Interaction Guidelines
* **Plan First:** Before implementing complex security logic (like the filter chain or refresh token flow), outline your proposed changes for review.
* **Iterative Commits:** Do not attempt to build the entire system in one go. Focus on one logical phase at a time (e.g., Entities -> Basic Auth -> JWT Filter -> Refresh Tokens).
* **Verify DB:** Ensure all entity relationships (e.g., Many-to-Many for Users and Roles) are correctly mapped for Hibernate to auto-generate the schema accurately.

## 🗂️ Project Management (Agent Instructions)
* **Tracking Progress:** Always check `task.md` to see the current project phase. Update it automatically by checking off `[x]` when a phase is complete.
* **Architectural Memory:** Check the `/decision` folder before proposing major architectural changes. If we make a new system-level decision, write a new markdown file in that folder documenting the choice.