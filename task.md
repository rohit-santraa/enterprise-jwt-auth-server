# Enterprise JWT Auth Server — Project Roadmap

## Phase 1: Project Setup & Dependencies ✅
- [x] Initialize Spring Boot 3.x Maven project with Java 21
- [x] Add dependencies: `spring-boot-starter-security`, `spring-boot-starter-data-jpa`, `jjwt`, `mysql-connector-j`, `spring-boot-starter-web`, `lombok`
- [x] Configure `application.properties`: datasource, JPA/Hibernate DDL auto, JWT secret, token expiry values
- [x] Verify the application starts without errors against a running MySQL instance

## Phase 2: Entity Layer ✅
- [x] Create `User` entity (`id`, `username`, `email`, `password`, `enabled`, many-to-many `roles`)
- [x] Create `Role` entity (`id`, `name` — e.g. `ROLE_USER`, `ROLE_ADMIN`)
- [x] Create `RefreshToken` entity (`id`, `token`, `user`, `expiryDate`, `revoked`)
- [x] Verify Hibernate auto-generates `users`, `roles`, `user_roles`, `refresh_tokens` tables correctly

## Phase 3: Repository Layer ✅
- [x] Create `UserRepository` (find by username, find by email)
- [x] Create `RoleRepository` (find by name)
- [x] Create `RefreshTokenRepository` (find by token, delete by user)

## Phase 4: Security Foundation ✅
- [x] Implement `CustomUserDetailsService` loading users from `UserRepository`
- [x] Expose `BCryptPasswordEncoder` bean in security config
- [x] Expose `AuthenticationManager` bean

## Phase 5: JWT Utilities ✅
- [x] Implement `JwtService`: generate access token (15–30 min TTL), extract claims, validate token
- [x] Implement `RefreshTokenService`: create, verify, and revoke refresh tokens (7-day TTL, stored in DB)

## Phase 6: JWT Filter & Security Configuration ✅
- [x] Implement `JwtAuthenticationFilter` (extends `OncePerRequestFilter`) — parse header, validate token, set `SecurityContext`
- [x] Configure `SecurityFilterChain`: stateless session (`STATELESS`), disable CSRF, permit `/api/auth/**`, authenticate all others
- [x] Wire `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`
- [x] Implement `JwtAuthenticationEntryPoint` for 401 responses on unauthenticated access

## Phase 7: Authentication Endpoints ✅
- [x] Create `AuthController` with `POST /api/auth/register` — validate input, hash password, assign default role, return 201
- [x] Create `POST /api/auth/login` — authenticate credentials, return access token + refresh token
- [x] Create `POST /api/auth/refresh` — validate refresh token from DB, issue new access token
- [x] Create `POST /api/auth/logout` — revoke refresh token in DB

## Phase 8: RBAC & Protected Endpoints ✅
- [x] Enable `@EnableMethodSecurity` in security config
- [x] Create `UserController` with `GET /api/users/profile` — secured with `@PreAuthorize("hasRole('USER')")`
- [x] Create `AdminController` with `GET /api/admin/users` — secured with `@PreAuthorize("hasRole('ADMIN')")`
- [x] Seed at least one `ROLE_ADMIN` and one `ROLE_USER` account for manual testing

## Phase 9: Exception Handling & Production Hardening ✅
- [x] Implement `GlobalExceptionHandler` (`@RestControllerAdvice`) — handle `UsernameNotFoundException`, `BadCredentialsException`, `AccessDeniedException`, validation errors
- [x] Ensure no raw stack traces are ever returned to the client
- [x] Write `Dockerfile` and `docker-compose.yml` (app + MySQL)
- [x] Smoke-test all endpoints end-to-end via Postman or curl
