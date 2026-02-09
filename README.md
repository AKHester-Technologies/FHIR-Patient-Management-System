# FHIR Patient Management System

A complete Patient Management System built with **Spring Boot** and **Thymeleaf** that uses **HAPI FHIR library** for backend storage. The frontend looks like a traditional EMR system while using FHIR R4 standard resources in the backend.

## 📋 Features

### ✅ Core Functionality
- **Patient Management**: Create, view, update, delete patient records
- **Practitioner Management**: Manage doctors and healthcare providers
- **Organization Management**: Handle hospitals and clinics
- **Appointment Scheduling**: Book and manage patient appointments
- **Audit Trail**: Complete audit logging using FHIR AuditEvent resource

### ✅ FHIR Resources Used
- `Patient` - Patient demographics and medical records
- `Practitioner` - Healthcare providers
- `Organization` - Healthcare facilities
- `Appointment` - Scheduling
- `AuditEvent` - Audit trail

### ✅ User Interface
- Modern, professional EMR-style interface
- Bootstrap 5 responsive design
- Easy-to-use forms with validation
- Search and filter capabilities
- Mobile-friendly

## 🛠️ Technology Stack

- **Backend**: Spring Boot 3.2.1, Java 17
- **FHIR Library**: HAPI FHIR 6.10.1 (R4)
- **Frontend**: Thymeleaf, Bootstrap 5, Font Awesome
- **Build Tool**: Maven

## 📦 Prerequisites

1. **Java 17** or higher
2. **Maven 3.6+**
3. **FHIR Server** running at `http://localhost:8080/fhir`

### Setting up FHIR Server (HAPI FHIR JPA Server)

You can use the official HAPI FHIR JPA Server:

```bash
# Download HAPI FHIR JPA Server
wget https://github.com/hapifhir/hapi-fhir-jpaserver-starter/releases/download/v6.10.1/ROOT.war

# Run with Jetty (or deploy to Tomcat)
java -jar jetty-runner.jar --port 8080 ROOT.war
```

Or use Docker:

```bash
docker run -p 8080:8080 hapiproject/hapi:latest
```

The FHIR server will be available at: `http://localhost:8080/fhir`

## 🚀 Installation & Setup

### 1. Clone or Extract the Project

```bash
cd fhir-patient-management
```

### 2. Configure FHIR Server URL

Edit `src/main/resources/application.properties`:

```properties
# Change this if your FHIR server is at a different URL
fhir.server.base-url=http://localhost:8080/fhir
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

The application will start on: **http://localhost:8081**

## 📖 Usage Guide

### Accessing the Application

1. Open browser: `http://localhost:8081`
2. You'll see the Dashboard with quick access to all modules

### Managing Patients

1. Click **"Patients"** in navigation
2. Click **"Add New Patient"** to create
3. Fill in the form (required fields marked with *)
4. Click **"Create Patient"**
5. Patient data is stored in FHIR server as `Patient` resource

### Behind the Scenes

When you create a patient:

1. **Frontend**: Thymeleaf form collects data
2. **Controller**: `PatientController` receives `PatientDTO`
3. **Mapper**: `PatientMapper` converts DTO → FHIR `Patient` resource
4. **Service**: `PatientService` uses HAPI FHIR client
5. **FHIR Server**: Stores Patient resource with unique ID
6. **Audit**: `AuditService` creates `AuditEvent` resource

## 📂 Project Structure

```
fhir-patient-management/
├── src/main/java/com/healthcare/pms/
│   ├── FhirPatientManagementApplication.java   # Main class
│   ├── config/
│   │   └── FhirClientConfig.java                # FHIR client setup
│   ├── controller/
│   │   ├── HomeController.java
│   │   └── PatientController.java               # Patient CRUD
│   ├── dto/
│   │   ├── PatientDTO.java                      # Frontend data model
│   │   ├── PractitionerDTO.java
│   │   ├── OrganizationDTO.java
│   │   └── AppointmentDTO.java
│   ├── mapper/
│   │   ├── PatientMapper.java                   # DTO ↔ FHIR Resource
│   │   ├── PractitionerMapper.java
│   │   ├── OrganizationMapper.java
│   │   └── AppointmentMapper.java
│   └── service/
│       ├── PatientService.java                  # FHIR operations
│       ├── AuditService.java                    # Audit logging
│       └── [Other services to be created]
│
├── src/main/resources/
│   ├── application.properties                   # Configuration
│   └── templates/
│       ├── index.html                           # Dashboard
│       ├── layout.html                          # Base template
│       └── patients/
│           ├── list.html                        # Patient list
│           ├── form.html                        # Create/Edit form
│           └── view.html                        # Patient details
│
└── pom.xml                                      # Maven dependencies
```

## 🔑 Key Components Explained

### 1. DTOs (Data Transfer Objects)

Frontend models that represent form data. Example: `PatientDTO.java`

- Contains validation annotations
- User-friendly field names
- Calculated fields (age, full name)

### 2. Mappers

Convert between DTOs and FHIR resources. Example: `PatientMapper.java`

- `toFhirResource()` - DTO → FHIR Patient
- `toDTO()` - FHIR Patient → DTO
- Handles FHIR-specific structures (HumanName, ContactPoint, etc.)

### 3. Services

Business logic and FHIR operations. Example: `PatientService.java`

- Uses `IGenericClient` (HAPI FHIR client)
- CRUD operations: create, read, update, delete
- Search operations
- Calls AuditService for logging

### 4. Controllers

Handle HTTP requests and return views. Example: `PatientController.java`

- `@GetMapping` - Show forms/lists
- `@PostMapping` - Submit forms
- Returns Thymeleaf template names
- Validation handling

## 🎨 Frontend Features

### Forms
- Responsive 3-column layout
- Field validation with error messages
- Required field indicators (*)
- Bootstrap styling

### Lists/Tables
- Search functionality
- Status badges
- Action buttons (View/Edit/Delete)
- Empty state messages

### Views
- Clean detail pages
- Grouped information sections
- Quick actions (Book Appointment, Edit, Delete)

## 🔧 Customization

### Change FHIR Server

Edit `application.properties`:

```properties
fhir.server.base-url=https://your-fhir-server.com/fhir
```

### Add New Resource Type

1. Create DTO in `dto/` package
2. Create Mapper in `mapper/` package
3. Create Service in `service/` package
4. Create Controller in `controller/` package
5. Create Thymeleaf templates in `templates/`

### Modify UI

- Templates are in `src/main/resources/templates/`
- Using Bootstrap 5 classes
- Icons from Font Awesome 6

## 🐛 Troubleshooting

### Application won't start

**Error**: Connection refused to FHIR server

**Solution**: Make sure FHIR server is running at `http://localhost:8080/fhir`

### Validation errors

**Error**: Patient creation fails with validation

**Solution**: Check all required fields are filled and formats are correct
- Phone: 10 digits
- Aadhaar: 12 digits
- PAN: Format ABCDE1234F
- Postal Code: 6 digits

### Data not persisting

**Error**: Data disappears after restart

**Solution**: HAPI FHIR JPA Server uses H2 in-memory database by default. Configure PostgreSQL/MySQL for persistence.

## 📝 API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/` | GET | Dashboard |
| `/patients` | GET | List all patients |
| `/patients/new` | GET | New patient form |
| `/patients` | POST | Create patient |
| `/patients/{id}` | GET | View patient details |
| `/patients/{id}/edit` | GET | Edit patient form |
| `/patients/{id}` | POST | Update patient |
| `/patients/{id}/delete` | POST | Delete patient |

## 🔐 Security Notes

This is a POC without authentication/authorization. For production:

1. Add Spring Security
2. Implement user roles (Admin, Doctor, Receptionist)
3. Add HTTPS
4. Implement OAuth2 for FHIR server access
5. Add audit user tracking

## 📚 FHIR Resources

- [FHIR R4 Specification](https://hl7.org/fhir/R4/)
- [HAPI FHIR Documentation](https://hapifhir.io/hapi-fhir/docs/)
- [HAPI FHIR Client](https://hapifhir.io/hapi-fhir/docs/client/introduction.html)

## 🎯 Next Steps

To complete the POC, create similar implementations for:

1. **Practitioner Module**
   - Controllers, Services, Templates
   - Follow same pattern as Patient

2. **Organization Module**
   - Hospital/Clinic management
   - FHIR Organization resource

3. **Appointment Module**
   - Booking system
   - Calendar view
   - FHIR Appointment resource

4. **Enhanced Audit**
   - View audit logs
   - Filter by resource/action/date

## 📧 Support

For questions about:
- FHIR standard: https://chat.fhir.org
- HAPI FHIR library: https://github.com/hapifhir/hapi-fhir
- Spring Boot: https://spring.io/projects/spring-boot

## 📄 License

This is a Proof of Concept (POC) project for learning purposes.

---

**Built with ❤️ using Spring Boot, HAPI FHIR, and Thymeleaf**
