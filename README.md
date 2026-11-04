# Approval Workflow System

A Spring Boot REST API for managing requests through a configurable approval workflow. The application provides authentication, role-based authorization, request lifecycle management, approval history, validation, and centralized exception handling.

## Technology Stack

- Java 17
- Spring Boot 4.1.2
- Spring Web MVC
- Spring Data JPA
- Spring Security
- JWT authentication
- Jakarta Bean Validation
- H2 Database
- Maven
- JUnit 5 and Spring Boot Test
- JaCoCo
- SonarQube

## Features

- User authentication through a login endpoint.
- JWT-based authentication and secured API endpoints.
- Role-based access control for requesters, approvers, and administrators.
- Request creation and retrieval.
- Approval and rejection actions.
- Approval history retrieval.
- Request payload validation.
- Centralized REST exception handling.
- Automated tests and code coverage reporting.
- SonarQube static code analysis.

## Project Structure

```text
src/
├── main/
│   ├── java/com/workflow/approval_workflow/
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   ├── AuthController.java
│   │   │   └── RequestController.java
│   │   ├── domain/
│   │   │   ├── ActingMode.java
│   │   │   ├── ActionType.java
│   │   │   ├── ApprovalHistory.java
│   │   │   ├── ApprovalStep.java
│   │   │   ├── AppUser.java
│   │   │   ├── Request.java
│   │   │   ├── RequestStatus.java
│   │   │   └── Role.java
│   │   ├── dto/
│   │   │   ├── ActionRequest.java
│   │   │   ├── ApiError.java
│   │   │   ├── CreateRequestDto.java
│   │   │   ├── HistoryResponse.java
│   │   │   ├── LoginRequest.java
│   │   │   ├── LoginResponse.java
│   │   │   ├── RequestResponse.java
│   │   │   └── StepResponse.java
│   │   ├── exception/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   └── ApprovalWorkflowApplication.java
│   └── resources/
│       ├── static/
│       ├── templates/
│       ├── application.yml
│       └── data.sql
└── test/
    └── java/
```

The application follows a layered structure:

- **Controller:** exposes REST endpoints and handles HTTP requests.
- **Service:** contains authentication and workflow business logic.
- **Repository:** handles persistence through Spring Data JPA.
- **Domain:** contains entities and workflow-related types.
- **DTO:** defines request and response payloads.
- **Security:** contains JWT authentication and user-details components.
- **Exception:** contains application-specific exceptions and global exception handling.

## Prerequisites

Install the following before running the application:

- JDK 17
- Apache Maven
- Git (optional, for source control)
- Docker Desktop (optional, for running SonarQube locally)

## Getting Started

### 1. Clone the repository

```bash
git clone <YOUR_GITHUB_REPOSITORY_URL>
cd approval-workflow
```

### 2. Configure the application

Application configuration is maintained in:

```text
src/main/resources/application.yml
```

Review the database, server, and JWT-related configuration before running the application. Keep environment-specific secrets outside source control.

### 3. Run the application

```bash
mvn spring-boot:run
```

Alternatively, build the application:

```bash
mvn clean package
```

Then run the generated JAR:

```bash
java -jar target/approval-workflow-*.jar
```

The application starts on the port configured in `application.yml`.

## API Endpoints

The application exposes the following REST endpoints.

| Method | Endpoint | Description | Access |
|---|---|---|---|
| POST | `/auth/login` | Authenticate a user | Public |
| POST | `/requests` | Create a request | REQUESTER |
| GET | `/requests/{id}` | Retrieve a request | REQUESTER, APPROVER, ADMIN |
| POST | `/requests/{id}/approve` | Approve a request | APPROVER, ADMIN |
| POST | `/requests/{id}/reject` | Reject a request | APPROVER, ADMIN |
| GET | `/requests/history/{id}` | Retrieve request history | REQUESTER, ADMIN |

### 1. Login

**Request**

```http
POST /auth/login
Content-Type: application/json
```

```json
{
  "username": "requester",
  "password": "password"
}
```

Both fields are required.

Use the authentication response to obtain the JWT token. The exact response structure is defined by `LoginResponse`.

### 2. Create a request

**Request**

```http
POST /requests
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "type": "PURCHASE",
  "description": "Request for new office equipment"
}
```

Validation rules:

- `type` is mandatory and must not be blank.
- `type` must not exceed 50 characters.
- `type` may contain only letters, digits, underscores, and hyphens.
- `description` may be up to 500 characters.

A successfully created request returns HTTP `201 Created`.

### 3. Retrieve a request

```http
GET /requests/{id}
Authorization: Bearer <JWT_TOKEN>
```

Retrieves a request by its ID. Access is restricted according to the authenticated user's role and the service-layer authorization rules.

### 4. Approve a request

```http
POST /requests/{id}/approve
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "comments": "Approved after review"
}
```

The request body is optional. If supplied, `comments` must not exceed 500 characters.

### 5. Reject a request

```http
POST /requests/{id}/reject
Authorization: Bearer <JWT_TOKEN>
Content-Type: application/json
```

```json
{
  "comments": "Additional information is required"
}
```

The request body is optional. If supplied, `comments` must not exceed 500 characters.

### 6. Retrieve approval history

```http
GET /requests/history/{id}
Authorization: Bearer <JWT_TOKEN>
```

Retrieves the approval history associated with the specified request.

## Security

The application uses Spring Security and JWT-based authentication.

- Users authenticate through `POST /auth/login`.
- Secured endpoints require a valid JWT bearer token.
- Role-based authorization is applied to controller endpoints.
- The application defines the roles `REQUESTER`, `APPROVER`, and `ADMIN`.
- Additional workflow-level authorization is handled by the service layer.

Example authorization header:

```http
Authorization: Bearer <JWT_TOKEN>
```

## Validation and Exception Handling

Request DTOs use Jakarta Bean Validation annotations to validate incoming payloads.

The application includes a centralized `GlobalExceptionHandler` to handle application exceptions and return consistent API error responses.

Custom exceptions cover cases such as:

- Resource not found.
- Forbidden actions.
- Invalid request types.
- Invalid workflow states.
- Workflow configuration problems.

## Testing and Code Coverage

Run the automated test suite:

```bash
mvn clean test
```

Run the full verification build, including coverage checks:

```bash
mvn clean verify
```

JaCoCo generates coverage reports at:

```text
target/site/jacoco/index.html
target/site/jacoco/jacoco.xml
```

The project is configured with a minimum **80% line coverage requirement**.

Latest verified test execution:

- Tests run: 74
- Failures: 0
- Errors: 0
- Skipped: 0
- Line coverage: approximately 99.1%
- Branch coverage: 100%

## SonarQube Integration

The project supports static code analysis using SonarQube.

### Run SonarQube locally using Docker

```bash
docker run -d --name sonarqube -p 9000:9000 ^
  -v sonarqube_data:/opt/sonarqube/data ^
  -v sonarqube_extensions:/opt/sonarqube/extensions ^
  -v sonarqube_logs:/opt/sonarqube/logs ^
  sonarqube:community
```

Open the SonarQube dashboard:

```text
http://localhost:9000
```

Create a project and generate an analysis token through the SonarQube interface.

### Configure the token

In PowerShell, set the token as an environment variable:

```powershell
$env:SONAR_TOKEN = "YOUR_SONAR_TOKEN"
```

Do not commit the token to source control.

### Run analysis

```powershell
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:5.5.0.6356:sonar "-Dsonar.projectKey=Approval-Workflow" "-Dsonar.projectName=Approval Workflow" "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=$env:SONAR_TOKEN" "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml"
```

View the project analysis:

```text
http://localhost:9000/dashboard?id=Approval-Workflow
```

## Design Decisions

- **Layered architecture:** separates controllers, business logic, persistence, security, and domain models.
- **DTO-based API:** separates external request/response contracts from persistence entities.
- **JWT authentication:** supports stateless authentication for secured REST endpoints.
- **Role-based authorization:** restricts endpoint access according to user roles.
- **Centralized exception handling:** provides consistent error responses.
- **Bean Validation:** validates incoming request payloads at the API boundary.
- **Automated testing:** uses JaCoCo to measure coverage and enforce the configured minimum.

