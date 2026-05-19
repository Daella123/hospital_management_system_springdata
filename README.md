# Hospital Management System — Spring Security Lab

> **Module 7 — Spring Security** continuation of the Spring Data JPA (Module 6) lab.

---

## Project Overview

A fully layered Spring Boot backend that manages patients, doctors, departments,
appointments, prescriptions, patient feedback, and medical inventory.

This module adds **complete Spring Security** on top of the existing Spring Data JPA layer:
- JWT-based authentication (HMAC-SHA256)
- Google OAuth2 login
- Role-Based Access Control (ADMIN · DOCTOR · NURSE · RECEPTIONIST)
- BCrypt password hashing
- Stateless session management
- CORS configuration
- CSRF disabled (explained below)
- Token blacklist (DSA: ConcurrentHashMap)
- Security event logging
- OpenAPI/Swagger with JWT Authorize button
- Admin security audit report endpoint

---

## Technology Stack

| Area              | Technology                            |
|-------------------|---------------------------------------|
| Language          | Java 21                               |
| Framework         | Spring Boot 3.4.5                     |
| Persistence       | Spring Data JPA / Hibernate           |
| Database          | PostgreSQL                            |
| Caching           | Spring Cache (ConcurrentMapCache)     |
| Validation        | Jakarta Bean Validation               |
| AOP / Logging     | Spring AOP                            |
| GraphQL           | Spring for GraphQL                    |
| API Docs          | Springdoc OpenAPI / Swagger UI 2.8.6  |
| Build Tool        | Maven                                 |

---

## Architecture

```
HTTP Request
     │
     ▼
Controller  (REST, @RequestMapping)
     │
     ▼
Service     (business logic, @Transactional, @Cacheable)
     │
     ▼
Repository  (JpaRepository — derived queries, @Query JPQL, native SQL)
     │
     ▼
PostgreSQL Database
```

---

## Database Setup

1. Install PostgreSQL and create the database:
   ```sql
   CREATE DATABASE hospital_db_dev;
   ```
2. The default credentials in `application-dev.yml` are:
   - **URL**: `jdbc:postgresql://localhost:5432/hospital_db_dev`
   - **Username**: `postgres`
   - **Password**: `12345`
3. Edit `src/main/resources/application-dev.yml` to match your local credentials.
4. Hibernate DDL is set to `update` — tables and indexes are created automatically on first run.

---

## Running the Application

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- GraphiQL:   http://localhost:8080/graphiql
- API Docs:   http://localhost:8080/api-docs

---

## Repository Structure & Query Types

### All 8 Repositories extend `JpaRepository<Entity, Long>`

| Repository                   | Key Query Features                                              |
|------------------------------|-----------------------------------------------------------------|
| `PatientRepository`          | `findByEmail`, `findByPhone`, native monthly registration count |
| `DoctorRepository`           | `findByDepartmentId`, JPQL available-doctors-by-dept-at-time   |
| `DepartmentRepository`       | `findByNameIgnoreCase`, `findByNameContainingIgnoreCase`        |
| `AppointmentRepository`      | JPQL by-department, patient-history; native count-by-dept, count-by-month |
| `PrescriptionRepository`     | JPQL full medical history (JOIN FETCH); native count-by-doctor  |
| `PatientFeedbackRepository`  | JPQL feedback-by-department                                     |
| `MedicalInventoryRepository` | JPQL low-stock; native expiring-items, low-stock-report         |
| `PrescriptionItemRepository` | `findByPrescriptionId`                                          |

---

## Derived Query Methods

Spring Data JPA generates SQL automatically from method names:

```java
// PatientRepository
Optional<Patient> findByEmail(String email);
Optional<Patient> findByPhone(String phone);
Page<Patient> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(...);

// DoctorRepository
List<Doctor> findByDepartmentId(Long departmentId);
Page<Doctor> findBySpecializationContainingIgnoreCase(String spec, Pageable pageable);

// AppointmentRepository
Page<Appointment> findByPatientId(Long patientId, Pageable pageable);
Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
Page<Appointment> findByAppointmentDateTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
```

---

## JPQL Custom Queries (`@Query`)

Used when entity relationships must be traversed or N+1 must be avoided:

```java
// Appointments belonging to a department (via doctor → department)
@Query("SELECT a FROM Appointment a JOIN a.doctor d WHERE d.department.id = :departmentId ...")
Page<Appointment> findByDepartmentId(@Param("departmentId") Long departmentId, Pageable pageable);

// Patient medical history — eagerly fetches all items to avoid N+1
@Query("SELECT DISTINCT p FROM Prescription p JOIN FETCH p.appointment a JOIN FETCH a.doctor d " +
       "LEFT JOIN FETCH p.items WHERE a.patient.id = :patientId ORDER BY p.issuedDate DESC")
List<Prescription> findMedicalHistoryByPatient(@Param("patientId") Long patientId);

// Available doctors by department at a given time
@Query("SELECT d FROM Doctor d WHERE d.department.id = :departmentId " +
       "AND d.id NOT IN (SELECT a.doctor.id FROM Appointment a WHERE a.appointmentDateTime = :dateTime ...)")
List<Doctor> findAvailableDoctorsByDepartment(...);

// Feedback for a department (via doctor → department)
@Query("SELECT f FROM PatientFeedback f JOIN f.doctor d WHERE d.department.id = :departmentId ...")
Page<PatientFeedback> findByDepartmentId(...);
```

---

## Native SQL Queries (`nativeQuery = true`)

Used for aggregation reports where JPQL cannot express GROUP BY projections cleanly:

```java
// Appointment count by department
@Query(value = "SELECT d.name, COUNT(a.id) FROM appointments a JOIN doctors doc ... GROUP BY d.name", nativeQuery = true)
List<Object[]> countAppointmentsByDepartmentNative();

// Monthly appointment/registration count
@Query(value = "SELECT EXTRACT(MONTH FROM appointment_date_time), COUNT(*) ... GROUP BY month", nativeQuery = true)
List<Object[]> countAppointmentsByMonthNative(@Param("year") int year);

// Prescription count by doctor
@Query(value = "SELECT doc.id, CONCAT(first_name,' ',last_name), COUNT(p.id) FROM prescriptions p ... GROUP BY doc.id", nativeQuery = true)
List<Object[]> countPrescriptionsByDoctorNative();

// Expiring inventory items
@Query(value = "SELECT * FROM medical_inventory WHERE expiry_date < :beforeDate ORDER BY expiry_date", nativeQuery = true)
List<MedicalInventory> findExpiringBefore(@Param("beforeDate") LocalDate beforeDate);
```

---

## Pagination and Sorting

All major list endpoints support `page`, `size`, `sortBy`, `direction` parameters.

**Response shape:**
```json
{
  "content":       [...],
  "pageNumber":    0,
  "pageSize":      10,
  "totalElements": 42,
  "totalPages":    5,
  "last":          false
}
```

**Example requests:**
```
GET /api/v1/patients?page=0&size=10&sortBy=lastName&direction=asc
GET /api/v1/appointments?page=0&size=10&sortBy=appointmentDateTime&direction=desc
GET /api/v1/doctors?page=0&size=5&sortBy=specialization&direction=asc
GET /api/v1/inventory?page=0&size=10&sortBy=name&direction=asc
```

**Before pagination:** the entire table was loaded into memory and serialised.  
**After pagination:** only the requested page is fetched from PostgreSQL — significantly lower memory and response time for large datasets.

---

## Transaction Management

### Class-level default
All service classes are annotated `@Transactional` with Spring defaults
(`REQUIRED` propagation, default isolation). Read methods override with `readOnly = true`.

### Explicit critical workflows

| Method                               | Propagation | Isolation        | rollbackFor       |
|--------------------------------------|-------------|------------------|-------------------|
| `AppointmentServiceImpl.createAppointment`  | REQUIRED | READ_COMMITTED | Exception.class |
| `AppointmentServiceImpl.cancelAppointment`  | REQUIRED | DEFAULT        | Exception.class |
| `PrescriptionServiceImpl.createPrescription` | REQUIRED | READ_COMMITTED | Exception.class |
| `MedicalInventoryServiceImpl.adjustStock`   | REQUIRED | REPEATABLE_READ | Exception.class |

### Rollback Scenarios

| Scenario                                        | Exception                   | Result    |
|-------------------------------------------------|-----------------------------|-----------|
| Invalid patient ID when booking                 | `ResourceNotFoundException` | Rollback  |
| Invalid doctor ID when booking                  | `ResourceNotFoundException` | Rollback  |
| Doctor's slot already taken                     | `InvalidOperationException` | Rollback  |
| Cancelling a COMPLETED appointment              | `InvalidOperationException` | Rollback  |
| Prescription for non-CONFIRMED/COMPLETED appt   | `InvalidOperationException` | Rollback  |
| Duplicate prescription for same appointment     | `DuplicateResourceException` | Rollback |
| Stock deduction below zero                      | `InvalidOperationException` | Rollback  |

---

## Caching Configuration

**Config class:** `CacheConfig.java` — `@EnableCaching` + `ConcurrentMapCacheManager`

| Cache name    | Cached on                | Evicted on                        |
|---------------|--------------------------|-----------------------------------|
| `patients`    | `getPatientById(id)`     | `deletePatient`, `updatePatient`  |
| `doctors`     | `getDoctorById(id)`      | `deleteDoctor`, `updateDoctor`    |
| `departments` | `getDepartmentById(id)`, `getAllDepartments()` | create, update, delete |
| `inventory`   | `getItemById(id)`        | `deleteItem`, `adjustStock`       |

**Annotations used:**
- `@Cacheable(value = "patients", key = "#id")` — serves from cache on hit
- `@CachePut(value = "patients", key = "#id")` — updates cache on write
- `@CacheEvict(value = "patients", key = "#id")` — removes from cache on delete
- `@Caching(evict = {...})` — combined evictions (used for department's `all` + `id` entries)

**Before caching:** every `GET /api/v1/patients/{id}` hit the database.  
**After caching:** second and subsequent requests for the same ID are served in < 1ms from heap memory.

---

## Database Indexes

Indexes are declared with `@Index` in `@Table` annotations and created by Hibernate:

| Table              | Index columns                                              |
|--------------------|------------------------------------------------------------|
| `patients`         | `email`, `phone`                                           |
| `doctors`          | `email`, `department_id`                                   |
| `appointments`     | `appointment_date_time`, `doctor_id`, `patient_id`, `status` |
| `medical_inventory`| `name`, `quantity_in_stock`, `expiry_date`                 |
| `prescriptions`    | `issued_date`                                              |

**Before indexes:** date-range appointment queries perform full table scans.  
**After indexes:** queries on `appointment_date_time` use B-tree index lookups — order-of-magnitude faster on large tables.

---

## Reporting Endpoints

All reports are under `/api/v1/reports` and use native SQL aggregations:

| Endpoint                                      | Description                              |
|-----------------------------------------------|------------------------------------------|
| `GET /api/v1/reports/appointments-by-department` | Appointment count per department      |
| `GET /api/v1/reports/monthly-registrations?year=2025` | Patient registrations per month   |
| `GET /api/v1/reports/prescription-stats`     | Prescription count per doctor             |
| `GET /api/v1/reports/low-stock`              | Items at or below minimum stock level     |

---

## Testing with Postman

### 1. Create a Department
```
POST /api/v1/departments
{ "name": "Cardiology", "description": "Heart unit", "headOfDepartment": "Dr. Smith" }
```

### 2. Create a Doctor
```
POST /api/v1/doctors
{ "firstName": "John", "lastName": "Doe", "email": "john@hospital.com",
  "phone": "0512345678", "gender": "MALE", "specialization": "Cardiologist",
  "licenseNumber": "LIC-001", "yearsOfExperience": 10, "departmentId": 1 }
```

### 3. Create a Patient
```
POST /api/v1/patients
{ "firstName": "Jane", "lastName": "Smith", "email": "jane@email.com",
  "phone": "0598765432", "gender": "FEMALE", "bloodType": "A_POSITIVE" }
```

### 4. Book an Appointment (rollback demo)
```
POST /api/v1/appointments
{ "patientId": 1, "doctorId": 1, "appointmentDateTime": "2025-06-01T09:00:00", "reason": "Check-up" }
```
Book the same slot again → expect **409 Conflict** (slot taken, transaction rolled back).

### 5. Test Pagination
```
GET /api/v1/patients?page=0&size=5&sortBy=lastName&direction=asc
```
Response includes `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `last`.

### 6. Test Caching
```
GET /api/v1/patients/1   ← DB hit (check logs: "Hibernate: select ...")
GET /api/v1/patients/1   ← Cache hit (no SQL in logs)
PUT /api/v1/patients/1   ← Cache updated (@CachePut)
GET /api/v1/patients/1   ← Cache hit with new data
DELETE /api/v1/patients/1 ← Cache evicted
GET /api/v1/patients/1   ← DB hit again
```

### 7. Reports
```
GET /api/v1/reports/appointments-by-department
GET /api/v1/reports/monthly-registrations?year=2025
GET /api/v1/reports/prescription-stats
GET /api/v1/reports/low-stock
```

---

## Performance Notes

| Optimization           | Before                          | After                                    |
|------------------------|---------------------------------|------------------------------------------|
| Pagination             | All rows loaded into memory     | Only requested page fetched from DB      |
| Caching                | Every GET hits the database     | Repeated GETs served from heap, < 1ms   |
| DB Indexes             | Full table scans on date/id cols| B-tree index lookups, orders of magnitude faster |
| JPQL JOIN FETCH        | N+1 queries for collections     | Single optimised query with eager join   |
| Native SQL aggregation | Application-side counting loops | DB-engine GROUP BY, minimal data transfer|

---

## Deliverables Summary

| # | Deliverable                    | Status |
|---|-------------------------------|--------|
| 1 | Spring Data JPA Integration    | ✅ All 8 repositories with CRUD, pagination, sorting |
| 2 | Custom Query Implementation    | ✅ 7+ JPQL queries, 6+ native SQL queries |
| 3 | Transaction Management         | ✅ Explicit propagation, isolation, rollback scenarios |
| 4 | Caching Implementation         | ✅ @EnableCaching, @Cacheable, @CachePut, @CacheEvict |
| 5 | Performance Report             | ✅ Documented above |
| 6 | Updated API Documentation      | ✅ Swagger UI with caching/transaction descriptions |
| 7 | README File                    | ✅ This file |

---

---

# 🔐 Spring Security Module

---

## Technology Stack (Security additions)

| Area                 | Technology                                      |
|----------------------|-------------------------------------------------|
| Authentication       | Spring Security + JWT (HMAC-SHA256, JJWT 0.12.6) |
| OAuth2 Login         | Spring OAuth2 Client — Google provider          |
| Password Hashing     | BCryptPasswordEncoder                           |
| Authorization        | Role-Based Access Control (`@PreAuthorize`)     |
| Session Strategy     | Stateless (no HTTP session)                     |
| Token Revocation     | ConcurrentHashMap-based blacklist               |
| Security Logging     | SLF4J via SecurityEventLogger                   |
| API Docs             | Springdoc OpenAPI with JWT Bearer Authorize     |

---

## New Entities

| Entity | Table       | Purpose                                          |
|--------|-------------|--------------------------------------------------|
| `User` | `users`     | App user — supports LOCAL and GOOGLE providers   |
| `Role` | `roles`     | Role record — ADMIN, DOCTOR, NURSE, RECEPTIONIST |

Join table `user_roles` (many-to-many).

---

## Authentication Endpoints

| Method | Path              | Auth Required | Description                         |
|--------|-------------------|---------------|-------------------------------------|
| POST   | `/auth/register`  | ❌ Public     | Register new user (default: RECEPTIONIST) |
| POST   | `/auth/login`     | ❌ Public     | Login → returns JWT token           |
| GET    | `/auth/me`        | ✅ Bearer JWT | Get current user profile            |
| POST   | `/auth/logout`    | ✅ Bearer JWT | Blacklist token (revoke)            |

---

## How JWT Authentication Works

```
1. Client → POST /auth/login { email, password }
2. Server validates credentials via AuthenticationManager
3. Server generates JWT (HMAC-SHA256 signed, contains email + roles + expiry)
4. Client stores JWT and sends it on every request:
       Authorization: Bearer <token>
5. JwtAuthenticationFilter intercepts every request:
   a. Extracts token from header
   b. Checks token against blacklist (ConcurrentHashMap lookup)
   c. Validates signature + expiry via JwtService
   d. Loads user from DB (CustomUserDetailsService)
   e. Sets SecurityContext → request is authenticated
6. @PreAuthorize annotations check roles before controller methods execute
```

**Token lifetime:** 24 hours (configurable via `app.jwt.expiration-ms`)

---

## How Google OAuth2 Login Works

```
1. Browser → GET /oauth2/authorization/google
2. Google prompts for consent
3. Google → redirects to /login/oauth2/code/google with auth code
4. OAuth2SuccessHandler fires:
   a. Extracts email + name from Google profile
   b. Finds existing user by email, OR creates new user (RECEPTIONIST role)
   c. Issues a JWT token
   d. Redirects to: http://localhost:3000/oauth2/callback?token=<jwt>
5. Frontend stores token and uses it as Bearer on all requests
```

> ⚠️ You must configure real credentials in `application-dev.yml`:
> ```yaml
> spring.security.oauth2.client.registration.google:
>   client-id: YOUR_GOOGLE_CLIENT_ID
>   client-secret: YOUR_GOOGLE_CLIENT_SECRET
> ```
> Get credentials from: https://console.cloud.google.com/apis/credentials

---

## Role-Based Access Control (RBAC)

### Roles

| Role           | Access Level                                                    |
|----------------|-----------------------------------------------------------------|
| `ADMIN`        | Full access to all endpoints + admin security report           |
| `DOCTOR`       | Read all; write prescriptions; update appointment status; reports |
| `NURSE`        | Read all; write/adjust medical inventory                        |
| `RECEPTIONIST` | Register/update patients; schedule/cancel appointments; read all |

### Endpoint Permissions

| Resource             | GET (read) | POST/PUT (write) | PATCH (update) | DELETE      |
|----------------------|------------|------------------|----------------|-------------|
| `/api/v1/patients`   | All auth   | ADMIN, RECEPTIONIST | ADMIN, RECEPTIONIST | ADMIN   |
| `/api/v1/doctors`    | All auth   | ADMIN            | ADMIN          | ADMIN       |
| `/api/v1/departments`| All auth   | ADMIN            | ADMIN          | ADMIN       |
| `/api/v1/appointments`| All auth  | ADMIN, RECEPTIONIST | ADMIN, RECEPTIONIST, DOCTOR | ADMIN |
| `/api/v1/prescriptions`| All auth | ADMIN, DOCTOR    | ADMIN, DOCTOR  | ADMIN       |
| `/api/v1/inventory`  | All auth   | ADMIN, NURSE     | ADMIN, NURSE   | ADMIN       |
| `/api/v1/feedbacks`  | All auth   | All auth         | —              | ADMIN       |
| `/api/v1/reports`    | ADMIN, DOCTOR | —             | —              | —           |
| `/api/v1/admin/**`   | ADMIN only | ADMIN only       | —              | —           |

---

## CORS Configuration

**CORS (Cross-Origin Resource Sharing)** is a browser mechanism that restricts which
external origins can make requests to your API.

- Allowed origins are configured in `app.cors.allowed-origins` (comma-separated)
- Default: `http://localhost:3000`, `http://localhost:4200`, `http://localhost:8080`
- Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
- Allowed headers: Authorization, Content-Type, Accept
- Credentials: allowed (needed for cookies/auth headers)

> Postman and server-to-server calls are **not** restricted by CORS — it is browser-only.

---

## CSRF Configuration & Explanation

**CSRF (Cross-Site Request Forgery)** attacks trick a browser into submitting requests
using the user's existing authenticated session (cookies).

| Mechanism | CORS                                     | CSRF                                          |
|-----------|------------------------------------------|-----------------------------------------------|
| Purpose   | Controls which origins can access API    | Prevents forged requests using session cookies |
| Applies to| Browser fetch/XHR calls                  | Browser form submissions with session cookies  |

**Why CSRF is disabled here:**

This API is **stateless** — no cookies, no sessions. Clients send the JWT manually in
the `Authorization` header. Since CSRF attacks rely on automatic cookie sending by the
browser, they are not applicable when tokens are sent explicitly in headers.

**When to enable CSRF:**
- Server-rendered HTML forms (Thymeleaf, JSP)
- Session-based (cookie) authentication
- Any traditional web app that uses cookie sessions

---

## DSA Concepts in Security

### 1. BCrypt Hashing
Passwords are stored as BCrypt hashes (cost factor 10 by default).
BCrypt is a one-way adaptive hashing function — it cannot be reversed, and
increasing the cost factor makes brute-force attacks exponentially slower.

```java
// Store:
user.setPassword(passwordEncoder.encode(rawPassword)); // → $2a$10$...

// Verify:
passwordEncoder.matches(rawPassword, storedHash); // → true/false
```

### 2. JWT Signature Verification
JWT tokens are signed with HMAC-SHA256. On every request:
- The server recomputes the HMAC signature using its secret key
- Compares with the signature in the token
- Any tampering → signature mismatch → 401 Unauthorized
- Time complexity: O(1) — no DB call needed for validation

### 3. Token Blacklist — ConcurrentHashMap
When a user logs out, the JWT is added to a `ConcurrentHashMap<String, LocalDateTime>`:

```java
// O(1) average insert
blacklistedTokens.put(token, expiresAt);

// O(1) average lookup — checked on every request
blacklistedTokens.containsKey(token);
```

A scheduled task runs every 30 minutes and removes entries whose expiry has passed,
keeping memory bounded.

---

## Security Event Logging

The following events are logged via `SecurityEventLogger` using SLF4J:

| Event                   | Log Level | Example                                        |
|-------------------------|-----------|------------------------------------------------|
| Login success           | INFO      | `[SECURITY] LOGIN SUCCESS — user='a@b.com'`   |
| Login failure           | WARN      | `[SECURITY] LOGIN FAILED — user='a@b.com'`    |
| Logout / blacklist      | INFO      | `[SECURITY] LOGOUT — user='a@b.com' token blacklisted` |
| Unauthorized access     | WARN      | `[SECURITY] UNAUTHORIZED ACCESS — uri='/api/v1/patients'` |
| Token expired           | WARN      | `[SECURITY] TOKEN EXPIRED — uri='/api/v1/doctors'` |
| Blacklisted token reuse | WARN      | `[SECURITY] BLACKLISTED TOKEN USED — uri='...'` |

Admin security statistics are available at:
```
GET /api/v1/admin/security-report    (ADMIN only)
GET /api/v1/admin/blacklist-size     (ADMIN only)
```

---

## Testing with Postman

### Step 1 — Seed roles (run once via SQL or first startup)
The application will auto-create the `roles` table. Seed the 4 roles:
```sql
INSERT INTO roles (name) VALUES ('ADMIN'), ('DOCTOR'), ('NURSE'), ('RECEPTIONIST')
ON CONFLICT DO NOTHING;
```

### Step 2 — Register a user
```http
POST /auth/register
Content-Type: application/json

{
  "firstName": "Admin",
  "lastName": "User",
  "email": "admin@hospital.com",
  "password": "securePass123",
  "role": "ADMIN"
}
```

### Step 3 — Login
```http
POST /auth/login
Content-Type: application/json

{ "email": "admin@hospital.com", "password": "securePass123" }
```
Response:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "type": "Bearer", "roles": ["ADMIN"] }
```

### Step 4 — Use JWT token
Copy the token. In Postman: **Authorization → Bearer Token → paste token**

### Step 5 — Access secured endpoint
```http
GET /api/v1/patients
Authorization: Bearer <your-token>
```
→ 200 OK with patient list

### Step 6 — Access without token
```http
GET /api/v1/patients
```
→ 401 Unauthorized

### Step 7 — Access with wrong role
```http
POST /api/v1/doctors    (using a RECEPTIONIST token)
```
→ 403 Forbidden

### Step 8 — Logout (blacklist token)
```http
POST /auth/logout
Authorization: Bearer <your-token>
```

### Step 9 — Reuse blacklisted token
```http
GET /auth/me
Authorization: Bearer <same-token>
```
→ 401 Unauthorized — "Token has been revoked"

### Step 10 — Test CORS (requires browser/frontend)
Requests from `http://localhost:3000` → ✅ Allowed (in allowed-origins list)
Requests from `http://evil.com` → ❌ Blocked by CORS

---

## Testing with Swagger UI

1. Go to: http://localhost:8080/swagger-ui.html
2. Expand `Authentication` → `POST /auth/login` → Execute → Copy token
3. Click **Authorize** (top right) → enter token → **Authorize**
4. All secured endpoints now show 🔒 and automatically include the token

---

## Environment Variables (Production)

In production, override these via environment variables instead of hardcoding:

| Variable                          | Description                    |
|-----------------------------------|--------------------------------|
| `APP_JWT_SECRET`                  | JWT signing secret (≥256 bit)  |
| `APP_JWT_EXPIRATION_MS`           | Token lifetime in milliseconds |
| `APP_CORS_ALLOWED_ORIGINS`        | Comma-separated frontend URLs  |
| `SPRING_DATASOURCE_URL`           | PostgreSQL connection string   |
| `SPRING_DATASOURCE_USERNAME`      | DB username                    |
| `SPRING_DATASOURCE_PASSWORD`      | DB password                    |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID`     | Google OAuth2 Client ID     |
| `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET` | Google OAuth2 Client Secret |

---

## Assumptions

1. Roles must be seeded in the `roles` table before registration works.
2. Google OAuth2 requires real credentials — the placeholders in config will fail.
3. The JWT secret in `application-dev.yml` is for development only — change it in production.
4. The token blacklist is in-memory — it resets on application restart. For production, use Redis.
5. The OAuth2 redirect URL (`http://localhost:3000/oauth2/callback`) assumes a React/Angular frontend — adjust as needed.

