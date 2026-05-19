# Task: Add Spring Security Requirements to My Existing Hospital Management System Project

I already have an existing **Hospital Management System** project built with **Spring Boot**.  
Previously, I worked on **Spring Data**, and now I need to upgrade the same project by adding **Spring Security** requirements.

Before making any changes, please do the following:

## Step 1: First Read and Understand the Existing Project

Please carefully read the existing codebase first.  
Do not start adding security immediately.

You must first understand:

- The current project structure
- Existing entities
- Existing repositories
- Existing services
- Existing controllers
- Existing DTOs, if any
- Existing database relationships
- Existing endpoints
- Existing packages and naming style
- Current `pom.xml` dependencies
- Current application configuration
- How the Hospital Management System currently works

After understanding the project, add Spring Security in a way that fits the existing structure instead of rewriting everything unnecessarily.

---

# Main Goal

Add a complete **Spring Security implementation** to my existing Hospital Management System project using:

- Spring Boot 3.x
- Java 21
- Spring Security
- JWT Authentication
- Google OAuth2 Login
- Role-Based Access Control
- BCrypt Password Hashing
- CORS Configuration
- CSRF Configuration / Demonstration
- Security Event Logging
- DSA-related security features such as hashing, token validation, and token blacklist using a map
- OpenAPI documentation
- README documentation
- Postman testing support

---

# Required Security Features

## 1. Security Configuration and Access Policies

Please configure Spring Security properly.

### Requirements

- Create a proper `SecurityFilterChain`.
- Protect all private endpoints.
- Allow public access only to endpoints such as:
    - User registration
    - Login
    - Public authentication endpoints
    - Swagger/OpenAPI documentation
- Define restricted endpoints based on roles:
    - ADMIN
    - DOCTOR
    - NURSE
    - RECEPTIONIST
- Use `BCryptPasswordEncoder` to hash all passwords before saving them in the database.
- Make sure passwords are never stored as plain text.

### Expected files/classes may include:

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `AuthenticationController`
- `AuthService`
- `UserDetailsServiceImpl`
- `PasswordEncoder` bean

---

## 2. CORS Configuration

Add secure CORS configuration.

### Requirements

- Implement global CORS configuration.
- Allow only specific origins, methods, and headers.
- Allow testing with:
    - Postman
    - Web frontend
- Reject unauthorized origins.
- Configure allowed methods such as:
    - GET
    - POST
    - PUT
    - PATCH
    - DELETE
    - OPTIONS
- Configure allowed headers such as:
    - Authorization
    - Content-Type

---

## 3. JWT-Based Authentication

Implement full JWT authentication.

### Requirements

Create a login endpoint:

```http
POST /auth/login

When a user logs in with valid credentials, the system must return a signed JWT token.

The JWT must include:

Username or email as subject
User roles
Issued time
Expiration time

Also implement:

JWT generation
JWT validation
JWT extraction from request header
JWT signature verification
JWT expiration check
Rejection of expired tokens with 401 Unauthorized
Rejection of tampered tokens with 401 Unauthorized

Use one of the following algorithms:

HMAC SHA-256
RSA

Preferred: HMAC SHA-256 unless the current project structure suggests otherwise.

4. Authentication Endpoints

Please create authentication endpoints such as:

POST /auth/register
POST /auth/login
GET /auth/me
POST /auth/logout
Register endpoint should:
Register a new user
Hash the password using BCrypt
Assign a default role where appropriate
Save the user in the database
Login endpoint should:
Validate credentials
Return JWT token
Return user role information
Me endpoint should:
Return currently authenticated user information
Logout endpoint should:
Add the current JWT token to a blacklist map so it cannot be reused
5. CSRF and Session Security

Configure CSRF correctly.

Requirements
Disable CSRF for stateless JWT APIs.
Use stateless session management for JWT authentication.
Add a short demonstration endpoint or explanation showing when CSRF should be enabled.
Add documentation in the README explaining:
Why CSRF is disabled for JWT APIs
When CSRF should be enabled
Difference between CSRF and CORS

Example explanation to include in README:

CORS controls which external origins can access the backend.
CSRF protects stateful browser-based sessions from unauthorized form submissions.
Since JWT APIs are stateless and tokens are sent manually in the Authorization header, CSRF is usually disabled.
6. Google OAuth2 Login

Integrate Google OAuth2 login.

Requirements
Add OAuth2 login using Google provider.
Fetch user details from Google after successful login.
Save OAuth2 users in the existing database.
Assign roles after successful OAuth2 authentication.
If the Google user already exists, update or reuse the existing user.
If the Google user does not exist, create a new user record.
Map OAuth2 users to the existing user system.
Expected configuration

Add necessary properties in application.properties or application.yml, but use placeholders for secrets:

spring.security.oauth2.client.registration.google.client-id=YOUR_GOOGLE_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_GOOGLE_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=email,profile

Do not hardcode real credentials.

7. Role-Based Access Control

Implement RBAC properly.

Required roles

Use the following roles:

ADMIN
DOCTOR
NURSE
RECEPTIONIST
Requirements
Extend the existing database with User and Role entities if they do not already exist.
Use @PreAuthorize or @Secured where necessary.
Enable method-level security using:
@EnableMethodSecurity
Suggested access control

Please adapt this to the existing endpoints after reading the project:

ADMIN can access all user management and system management endpoints.
DOCTOR can access doctor-related and patient medical endpoints.
NURSE can access nurse-related and patient care endpoints.
RECEPTIONIST can access appointment, patient registration, and front desk endpoints.
Public users can only access authentication/public endpoints.
8. DSA and Security Optimization

Add security-related DSA concepts.

Requirements

Implement:

Hashing for password security using BCrypt
Secure token validation logic
Map-based token blacklist for revoked/logout tokens

Example:

Map<String, LocalDateTime> blacklistedTokens = new ConcurrentHashMap<>();

Use this blacklist to:

Store logged-out or revoked tokens
Check every incoming JWT token
Reject blacklisted tokens
Remove expired tokens where necessary

Also explain this DSA usage in the README.

9. Security Event Logging

Add logging for security events.

Requirements

Log the following:

Successful login
Failed login
Unauthorized access attempt
Token expiration
Token blacklist/logout
Endpoint access frequency where possible

Use proper logging, for example:

private static final Logger logger = LoggerFactory.getLogger(ClassName.class);

Also add a simple security report or service if possible to show:

Login attempts
Failed login attempts
Token usage
Endpoint access frequency
10. OpenAPI / Swagger Documentation

Update OpenAPI documentation.

Requirements
Document public endpoints.
Document secured endpoints.
Show which endpoints require JWT.
Add JWT bearer token configuration to Swagger.
Make Swagger accessible without authentication.

Suggested public Swagger paths:

/swagger-ui/**
/v3/api-docs/**
11. Testing Requirements

Please add or explain Postman testing steps for:

Register user
Login user
Copy JWT token
Access secured endpoint with token
Access secured endpoint without token
Access ADMIN endpoint with non-admin role
Test expired or invalid token
Test logout and blacklisted token
Test CORS with allowed origin
Test CORS with unauthorized origin
Test Google OAuth2 login flow

If possible, provide a Postman collection structure or sample requests in the README.

Technical Requirements

Please make sure the final project follows these requirements:

Area	Requirement
Framework	Spring Boot 3.x with Spring Security, JWT, OAuth2 Client, Validation, Cache
Language	Java 21
Authentication	Username/password with JWT and OAuth2 login with Google
Authorization	Role-Based Access Control
Security Features	CORS setup, CSRF demonstration, password hashing
Password Encryption	BCryptPasswordEncoder
Database	Existing database extended with user and role entities
Testing	Postman or web frontend testing
Documentation	OpenAPI documentation for secured and public endpoints
DSA Integration	Hashing, token validation, map-based blacklisting, secure lookup design
Required Deliverables

Please make sure the final code includes:

Spring Security integration
JWT authentication system
CORS and CSRF configuration
Google OAuth2 login
RBAC enforcement
Security event logging
DSA implementation using hashing, token validation, and blacklist map
README documentation
OpenAPI/Swagger documentation
Postman testing instructions
Important Coding Instructions

Please follow these rules:

Do not delete my existing functionality.
Do not rewrite the whole project unless absolutely necessary.
Add security in a clean and professional way.
Keep the project package structure consistent.
Use clear class names.
Add comments where needed, but do not over-comment.
Make sure the project compiles successfully.
Use DTOs for authentication requests and responses.
Do not expose passwords in API responses.
Do not hardcode secret keys directly in code.
Put JWT secrets and OAuth credentials in configuration files or environment variables.
Use proper exception handling.
Return meaningful HTTP status codes.
Make sure the code is production-ready as much as possible.
Suggested Classes to Add

Please add these only if they fit the project structure:

config/
  SecurityConfig.java
  CorsConfig.java
  OpenApiConfig.java

security/
  JwtService.java
  JwtAuthenticationFilter.java
  CustomUserDetailsService.java
  TokenBlacklistService.java
  OAuth2SuccessHandler.java

auth/
  AuthController.java
  AuthService.java
  dto/
    LoginRequest.java
    RegisterRequest.java
    AuthResponse.java

entity/
  User.java
  Role.java

repository/
  UserRepository.java
  RoleRepository.java

logging/
  SecurityEventLogger.java
  SecurityAuditService.java

If my project already has some of these classes, update them instead of creating duplicates.

README Requirements

Please update the README with:

Project overview
Spring Security features added
How JWT authentication works
How Google OAuth2 login works
How RBAC works
Role permissions
CORS explanation
CSRF explanation
DSA explanation:
BCrypt hashing
JWT validation
Token blacklist map
How to run the project
How to test with Postman
How to test with Swagger
Example requests and responses
Environment variables needed
Final Output Expected From You

After modifying the project, please provide:

A summary of what you changed
List of new files created
List of existing files updated
How to run the project
How to test authentication
How to test authorization
Any environment variables I need to add
Any database changes or migrations needed
Any assumptions you made
Any parts I need to manually configure, especially Google OAuth2 credentials

