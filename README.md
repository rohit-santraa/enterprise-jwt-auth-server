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
   git clone https://github.com/rohit-santraa/enterprise-jwt-auth-server.git
   cd auth-server
```
2. **Build and start the infrastructure:**

```bash
   docker compose up --build
```
3. **Access the application:**
The application will be available at http://localhost:8080.



## 📸 API Demonstration

### 1. User Registration (`POST /api/auth/register`)
Successfully registering a new enterprise user account with role-based permissions.

![User Registration](images/register.png)

### 2. User Login & JWT Generation (`POST /api/auth/login`)
Exchanging valid credentials for secure, stateless Access and Refresh tokens.

![User Login](images/login.png)


## System Architecture

```mermaid
flowchart TB
    CLIENT(["Client\nPostman · Web App · Mobile"])

    subgraph COMPOSE["Docker Compose — auth-network"]

        subgraph APP["auth-server · rohit/auth-server:latest · :8080"]
            subgraph SPRING["Spring Boot 3 · Java 21"]

                subgraph FCHAIN["Spring Security 6 — Servlet Filter Chain"]
                    CF["CORS Filter\nAllowed Origins · Methods · Headers · Credentials"]
                    JF["JwtAuthenticationFilter  extends  OncePerRequestFilter\n① Extract Authorization: Bearer token\n② Decode + Verify HS256 Signature\n③ Validate exp Claim\n④ loadUserByUsername()\n⑤ Populate SecurityContext"]
                    subgraph EXH["Exception Handling Layer"]
                        AEP["AuthEntryPoint\nAuthenticationException\n→ HTTP 401 Unauthorized"]
                        ADH["AccessDeniedHandler\nAccessDeniedException\n→ HTTP 403 Forbidden"]
                        GEH["@RestControllerAdvice\nGlobalExceptionHandler\n→ 400 · 404 · 409 · 500"]
                    end
                end

                subgraph CTRLS["Controller Layer — RESTful API"]
                    subgraph PUB["Public — permitAll()"]
                        AUTHC["AuthController  /api/auth\nPOST /register\nPOST /login\nPOST /refresh\nPOST /logout"]
                    end
                    subgraph PROT["Protected — @PreAuthorize RBAC"]
                        UC["UserController  /api/users\nGET  /profile  → hasRole(ROLE_USER)\nPUT  /profile  → hasRole(ROLE_USER)"]
                        ADC["AdminController  /api/admin\nGET    /users           → hasRole(ROLE_ADMIN)\nPUT    /users/{id}/role  → hasRole(ROLE_ADMIN)\nDELETE /users/{id}      → hasRole(ROLE_ADMIN)"]
                    end
                end

                subgraph SVCS["Service Layer — Business Logic"]
                    AS["AuthService\nregister() · login() · logout() · validateCredentials()"]
                    RTS["RefreshTokenService\ncreateToken() · findByToken()\nverifyExpiration() · revokeToken() · revokeAllUserTokens()"]
                    US["UserService\ngetProfile() · updateProfile() · changeRole() · deleteUser()"]
                    UDSI["UserDetailsServiceImpl  implements  UserDetailsService\nloadUserByUsername() → builds GrantedAuthority list"]
                end

                subgraph SUTIL["Security Utilities"]
                    JU["JwtUtil · HMAC-SHA256\ngenerateAccessToken(userDetails)  exp: 15 min\ngenerateRefreshToken()  UUID-based  exp: 7 days\nvalidateToken(token, userDetails)\nextractUsername(token) · extractAllClaims(token)"]
                    BC["BCryptPasswordEncoder  strength = 12\nencode(rawPassword)  →  $2a$12$[22-char salt][31-char hash]\nmatches(rawPassword, encodedPassword)  →  boolean\n~250 ms per hash · brute-force resistant"]
                end

                subgraph REPOS["Repository Layer — Spring Data JPA / Hibernate"]
                    UREP["UserRepository\nfindByUsername() · findByEmail()\nexistsByUsername() · existsByEmail()"]
                    RREP["RoleRepository\nfindByName(ERole name)"]
                    TREP["RefreshTokenRepository\nfindByToken() · deleteByUser()"]
                end

            end
        end

        subgraph DBCNT["mysql · mysql:8.0 · :3306 · authdb"]
            UTBL[("users\nid · username · email\npassword_hash · enabled\ncreated_at")]
            RTBL[("roles\nid · name\nROLE_USER · ROLE_ADMIN")]
            URTBL[("user_roles\nuser_id FK · role_id FK\nCOMPOSITE PK")]
            RTTBL[("refresh_tokens\nid · token UUID\nuser_id FK · expiry_date\nrevoked · created_at")]
        end

    end

    CLIENT -->|"HTTPS :8080"| CF
    CF --> JF
    JF -.->|"Public Route / No Token"| PUB
    JF -.->|"Invalid / Expired Token"| AEP
    JF -->|"Token Valid · SecurityContext Populated"| PROT
    PROT -.->|"Insufficient Role"| ADH
    PUB -.->|"Business / Validation Error"| GEH
    PROT -.->|"Business / Validation Error"| GEH

    AUTHC --> AS
    UC --> US
    ADC --> US
    AS --> JU
    AS --> BC
    AS --> RTS
    AS --> UREP
    RTS --> TREP
    US --> UREP
    US --> RREP
    JF --> UDSI
    UDSI --> UREP

    UREP -->|"Hibernate ORM"| UTBL
    RREP -->|"Hibernate ORM"| RTBL
    TREP -->|"Hibernate ORM"| RTTBL
    UTBL -.-|"M:M"| URTBL
    RTBL -.-|"M:M"| URTBL
```

---

## Access Token Flow (includes BCrypt)

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant F as JwtAuthFilter
    participant AC as AuthController
    participant AS as AuthService
    participant BC as BCryptPasswordEncoder
    participant JU as JwtUtil
    participant UDS as UserDetailsService
    participant SC as SecurityContext
    participant CTRL as Protected Controller
    participant DB as MySQL

    rect rgb(200, 228, 255)
        Note over C,DB: Phase 1 · Registration — BCrypt Hashing
        C->>+AC: POST /api/auth/register
        Note right of C: {username, email, password:"Secret123!"}
        AC->>+AS: register(RegisterRequest)
        AS->>DB: existsByEmail("user@company.com") → false
        AS->>+BC: encode("Secret123!")
        Note right of BC: ① Generate 22-char cryptographic salt<br/>② Apply 2^12 = 4,096 Blowfish rounds<br/>③ Output: $2a$12$[salt][hash]<br/>④ ~250 ms — intentional brute-force delay
        BC-->>-AS: "$2a$12$rA9bSmKpLqXt..."
        AS->>DB: INSERT INTO users {username, email, password_hash}
        DB-->>AS: User{id=1}
        AS-->>-AC: "Registration successful"
        AC-->>C: 201 Created
        deactivate AC
    end

    rect rgb(200, 255, 210)
        Note over C,DB: Phase 2 · Login — BCrypt Verify + JWT Generation
        C->>+AC: POST /api/auth/login
        Note right of C: {username:"rohit", password:"Secret123!"}
        AC->>+AS: login(LoginRequest)
        AS->>DB: SELECT users JOIN roles WHERE username=?
        DB-->>AS: User{hash="$2a$12$...", roles=[ROLE_USER]}
        AS->>+BC: matches("Secret123!", "$2a$12$rA9bSmKpLqXt...")
        Note right of BC: Re-hash input using stored salt<br/>Compare with constant-time equals
        BC-->>-AS: true ✓
        AS->>+JU: generateAccessToken(userDetails)
        Note right of JU: Header:  {alg:"HS256", typ:"JWT"}<br/>Payload: {sub:"rohit",<br/>          roles:["ROLE_USER"],<br/>          iat:1749859200,<br/>          exp:1749860100}<br/>Signature: HMAC-SHA256(secret, header+payload)
        JU-->>-AS: eyJhbGciOiJIUzI1NiJ9... [exp: +15 min]
        AS-->>-AC: AuthResponse{accessToken, refreshToken, roles}
        AC-->>C: 200 OK
        Note left of C: {accessToken: "eyJ...",<br/>refreshToken: "a3f9b2c8-...",<br/>tokenType: "Bearer",<br/>roles: ["ROLE_USER"]}
        deactivate AC
    end

    rect rgb(255, 255, 200)
        Note over C,DB: Phase 3 · Access Token Used on Protected Endpoint
        C->>+F: GET /api/users/profile
        Note right of C: Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
        F->>+JU: extractUsername(token)
        JU-->>-F: "rohit"
        F->>+UDS: loadUserByUsername("rohit")
        UDS->>DB: SELECT * FROM users JOIN user_roles JOIN roles
        DB-->>UDS: User + GrantedAuthority[ROLE_USER]
        UDS-->>-F: UserDetails
        F->>+JU: validateToken(token, userDetails)
        JU->>JU: verifySignature(HMAC-SHA256)
        JU->>JU: check exp(1749860100) > Instant.now()
        JU-->>-F: valid = true
        F->>SC: setAuthentication(UsernamePasswordAuthToken)
        F->>+CTRL: forward(authenticated request)
        CTRL->>CTRL: @PreAuthorize("hasRole('ROLE_USER')") → PASS
        CTRL-->>-C: 200 OK {profile data}
        deactivate F
    end

    rect rgb(255, 210, 210)
        Note over C,DB: Phase 4 · Access Token Expired — Client Must Refresh
        C->>+F: GET /api/users/profile
        Note right of C: Authorization: Bearer <expired token>
        F->>+JU: validateToken(token, userDetails)
        JU->>JU: check exp < Instant.now() → EXPIRED
        JU-->>-F: throw ExpiredJwtException
        F-->>C: 401 Unauthorized
        Note right of C: Client triggers POST /api/auth/refresh
        deactivate F
    end
```

---

## Refresh Token Lifecycle

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant AC as AuthController
    participant RTS as RefreshTokenService
    participant JU as JwtUtil
    participant TR as RefreshTokenRepository
    participant UR as UserRepository
    participant DB as MySQL

    rect rgb(200, 228, 255)
        Note over C,DB: Stage 1 · Issuance — Created at Login
        C->>+AC: POST /api/auth/login
        AC->>+RTS: createRefreshToken(user)
        RTS->>RTS: token = UUID.randomUUID().toString()
        RTS->>RTS: expiry = Instant.now().plus(7, DAYS)
        RTS->>+TR: save(RefreshToken{token, user, expiry, revoked=false})
        TR->>DB: INSERT INTO refresh_tokens
        DB-->>TR: RefreshToken{id=42}
        TR-->>-RTS: saved entity
        RTS-->>-AC: RefreshToken
        AC-->>C: 200 OK
        Note left of C: {accessToken: "eyJ...",<br/>refreshToken: "a3f9b2c8-d1e4-..."}
        deactivate AC
    end

    rect rgb(200, 255, 210)
        Note over C,DB: Stage 2 · Renewal — Rotate Expired Access Token
        C->>+AC: POST /api/auth/refresh
        Note right of C: {"refreshToken":"a3f9b2c8-d1e4-..."}
        AC->>+RTS: verifyAndRefresh(tokenString)
        RTS->>+TR: findByToken("a3f9b2c8-...")
        TR->>DB: SELECT * FROM refresh_tokens WHERE token=?
        DB-->>TR: RefreshToken{expiry=+6d, revoked=false}
        TR-->>-RTS: RefreshToken entity

        alt Token valid — not expired AND not revoked
            RTS->>UR: findById(token.user.id)
            UR->>DB: SELECT * FROM users
            DB-->>UR: User entity
            UR-->>RTS: User
            RTS-->>AC: User
            AC->>JU: generateAccessToken(user)
            JU-->>AC: new eyJhbGc... [+15 min]
            AC-->>C: 200 OK
            Note left of C: {newAccessToken: "eyJ...",<br/>refreshToken: "a3f9b2c8-..." (same)}
        else Token expired — expiry < Instant.now()
            RTS->>TR: delete(expiredToken)
            TR->>DB: DELETE FROM refresh_tokens WHERE id=42
            RTS-->>-AC: throw RefreshTokenExpiredException
            AC-->>C: 401 Unauthorized "Refresh token expired. Login required."
        else Token revoked — revoked = true
            RTS-->>AC: throw TokenRevokedException
            AC-->>C: 401 Unauthorized "Token was revoked. Login required."
        end
        deactivate AC
    end

    rect rgb(255, 230, 200)
        Note over C,DB: Stage 3 · Revocation — On Explicit Logout
        C->>+AC: POST /api/auth/logout
        Note right of C: {refreshToken: "a3f9b2c8-d1e4-..."}
        AC->>+RTS: revokeToken("a3f9b2c8-...")
        RTS->>+TR: findByToken(tokenString)
        TR->>DB: SELECT * FROM refresh_tokens WHERE token=?
        DB-->>TR: RefreshToken{revoked=false}
        TR-->>-RTS: entity
        RTS->>RTS: token.setRevoked(true)
        RTS->>+TR: save(token)
        TR->>DB: UPDATE refresh_tokens SET revoked=true WHERE id=42
        DB-->>TR: 1 row updated
        TR-->>-RTS: saved
        RTS-->>-AC: void
        AC-->>C: 200 OK "Logged out successfully"
        deactivate AC
    end
```

---

## RBAC Authorization Decision Flow

```mermaid
flowchart TD
    REQ(["Incoming HTTP Request"])

    EXTRACT["Extract JWT\nfrom Authorization: Bearer header"]
    HAS_TKN{"Token\nPresent?"}
    VERIFY["Verify HS256 Signature\n+ Decode exp Claim"]
    TKN_OK{"Signature Valid\n& Not Expired?"}
    LOAD_U["UserDetailsServiceImpl\nloadUserByUsername()"]
    BUILD_A["Build GrantedAuthority[]\nfrom user_roles JOIN roles table"]
    SET_SC["Set SecurityContext\nUsernamePasswordAuthToken"]
    PRE_AUTH["@PreAuthorize SpEL Expression\nhasRole() · hasAnyRole()\nhasAuthority() · isAuthenticated()"]
    ROLE_CHK{"Required Role\nGranted?"}

    subgraph ROLE_MATRIX["Role → Endpoint Access Matrix"]
        direction LR
        RU["ROLE_USER\nGET  /api/users/profile\nPUT  /api/users/profile"]
        RA["ROLE_ADMIN\nGET    /api/admin/users\nPUT    /api/admin/users/{id}/role\nDELETE /api/admin/users/{id}"]
    end

    E401A(["401 Unauthorized\nAuthEntryPoint\nMissing or malformed token"])
    E401B(["401 Unauthorized\nExpiredJwtException\nClient → POST /api/auth/refresh"])
    E403(["403 Forbidden\nAccessDeniedHandler\nInsufficient authority"])
    OK(["Controller Executes\n200 OK"])

    REQ --> EXTRACT
    EXTRACT --> HAS_TKN
    HAS_TKN -->|"No"| E401A
    HAS_TKN -->|"Yes"| VERIFY
    VERIFY --> TKN_OK
    TKN_OK -->|"Invalid Signature"| E401A
    TKN_OK -->|"Expired"| E401B
    TKN_OK -->|"Valid"| LOAD_U
    LOAD_U --> BUILD_A
    BUILD_A --> SET_SC
    SET_SC --> PRE_AUTH
    PRE_AUTH --> ROLE_CHK
    ROLE_CHK -->|"Authorized"| OK
    ROLE_CHK -->|"Denied"| E403
    OK -.->|"matches ROLE_USER"| RU
    OK -.->|"matches ROLE_ADMIN"| RA
```

---

## Exception Handling Matrix

```mermaid
flowchart LR
    REQ(["HTTP Request"])

    subgraph LAYER1["Spring Security Layer — Pre-Controller"]
        EX_AUTH["AuthenticationException\nUsernameNotFoundException\nBadCredentialsException\nExpiredJwtException\nMalformedJwtException\nSignatureException"]
        EX_ACCESS["AccessDeniedException\nInsufficientAuthenticationException"]
    end

    subgraph LAYER2["Application Layer — Controller & Service"]
        EX_VAL["MethodArgumentNotValidException\n@Valid Bean Validation Failures\nConstraintViolationException"]
        EX_NF["ResourceNotFoundException\nUserNotFoundException\nRoleNotFoundException"]
        EX_DUP["UserAlreadyExistsException\nEmailAlreadyInUseException"]
        EX_TOK["RefreshTokenExpiredException\nRefreshTokenRevokedException"]
        EX_INT["RuntimeException\nNullPointerException\nUnhandled Throwable"]
    end

    subgraph HANDLERS["Exception Handlers"]
        H_AEP["AuthEntryPoint\ncommence(request, exception)\nSpring Security managed"]
        H_ADH["AccessDeniedHandler\nhandle(request, exception)\nSpring Security managed"]
        H_GEH["@RestControllerAdvice\nGlobalExceptionHandler\n@ExceptionHandler(...)"]
    end

    subgraph RESPONSES["Structured JSON Error Responses — No Raw Stack Traces"]
        R400["400 Bad Request\n{status:400, error:'Validation Failed',\nmessage:[{field, rejected, reason}],\ntimestamp:'2026-06-14T...'"]
        R401["401 Unauthorized\n{status:401, error:'Unauthorized',\nmessage:'Invalid or expired token',\ntimestamp:'2026-06-14T...'"]
        R403["403 Forbidden\n{status:403, error:'Forbidden',\nmessage:'Insufficient privileges',\ntimestamp:'2026-06-14T...'"]
        R404["404 Not Found\n{status:404, error:'Not Found',\nmessage:'User not found',\ntimestamp:'2026-06-14T...'"]
        R409["409 Conflict\n{status:409, error:'Conflict',\nmessage:'Email already in use',\ntimestamp:'2026-06-14T...'"]
        R500["500 Internal Server Error\n{status:500, error:'Internal Server Error',\nmessage:'An unexpected error occurred'}"]
    end

    REQ --> LAYER1
    REQ --> LAYER2

    EX_AUTH --> H_AEP
    EX_ACCESS --> H_ADH
    EX_VAL --> H_GEH
    EX_NF --> H_GEH
    EX_DUP --> H_GEH
    EX_TOK --> H_GEH
    EX_INT --> H_GEH

    H_AEP --> R401
    H_ADH --> R403
    H_GEH -->|"@Valid failures"| R400
    H_GEH -->|"Not found"| R404
    H_GEH -->|"Duplicate resource"| R409
    H_GEH -->|"Token errors"| R401
    H_GEH -->|"Unhandled"| R500
```

---

## API Reference

| Method | Endpoint | Auth | Role | Description |
|--------|----------|------|------|-------------|
| POST | `/api/auth/register` | None | — | Create account, BCrypt hash password |
| POST | `/api/auth/login` | None | — | Authenticate, receive token pair |
| POST | `/api/auth/refresh` | None | — | Exchange refresh token for new access token |
| POST | `/api/auth/logout` | Bearer | Any | Revoke refresh token |
| GET | `/api/users/profile` | Bearer | ROLE_USER | Read own profile |
| PUT | `/api/users/profile` | Bearer | ROLE_USER | Update own profile |
| GET | `/api/admin/users` | Bearer | ROLE_ADMIN | List all users |
| PUT | `/api/admin/users/{id}/role` | Bearer | ROLE_ADMIN | Change user role |
| DELETE | `/api/admin/users/{id}` | Bearer | ROLE_ADMIN | Delete user |

## Token Specifications

| Token | Algorithm | Expiry | Storage | Transport |
|-------|-----------|--------|---------|-----------|
| Access Token | JWT · HS256 | 15 minutes | Client memory | Authorization: Bearer header |
| Refresh Token | UUID string | 7 days | MySQL `refresh_tokens` table | Request body |

## Error Response Schema

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired",
  "timestamp": "2026-06-14T10:30:00Z",
  "path": "/api/users/profile"
}
```
