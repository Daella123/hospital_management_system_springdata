# Hospital Management System — Spring Data JPA Lab

> **Module 6 — Spring Data JPA** continuation of the Spring Web (Module 5) lab.

---

## Project Overview

A fully layered Spring Boot backend that manages patients, doctors, departments,
appointments, prescriptions, patient feedback, and medical inventory.

This module adds:
- Spring Data JPA repositories with derived queries, JPQL, and native SQL
- Pagination and sorting on all major list endpoints
- Explicit transaction management with propagation, isolation, and rollback scenarios
- Spring Cache (`@EnableCaching`) with `@Cacheable`, `@CachePut`, `@CacheEvict`
- Database indexes on frequently queried columns
- Operational reporting endpoints backed by native SQL aggregations

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
