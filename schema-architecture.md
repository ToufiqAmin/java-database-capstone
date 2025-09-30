# Schema Architecture Document

## Section 1: Architecture Summary

This Spring Boot application is designed using a layered architecture that integrates both MVC and REST paradigms. The Admin and Doctor modules utilize Thymeleaf templates for dynamic HTML rendering, while other modules—such as Patient, 
Appointment, and Prescription—are served via RESTful APIs. The system connects to two databases: **MySQL** for structured relational data (patients, doctors, appointments, admins) and **MongoDB** for unstructured document data (prescriptions).

All incoming requests are handled by controllers (either MVC or REST), which delegate processing to a centralized service layer. This service layer encapsulates business logic and interacts with the persistence layer 
through Spring Data JPA (for MySQL) and Spring Data MongoDB (for MongoDB). Security is enforced using Spring Security with role-based access control, and data validation is performed using Jakarta Bean Validation.

## Section 2: Numbered Flow of Data and Control

1. A user accesses a page (e.g., AdminDashboard or Appointment) via browser or frontend client.
2. The request is routed to the appropriate controller:
   - Thymeleaf controller for MVC views.
   - REST controller for API endpoints.
3. The controller invokes the corresponding method in the service layer.
4. The service layer applies business rules and validates input.
5. Based on the data type:
   - For relational entities (e.g., Doctor, Appointment), the service calls JPA repositories.
   - For document entities (e.g., Prescription), the service calls MongoDB repositories.
6. Repositories query the respective databases:
   - MySQL via Hibernate ORM.
   - MongoDB via native document queries.
7. - Retrieved data is returned to the service layer which prepares the response:
      - JSON for REST APIs.
      - Model attributes for Thymeleaf views.
   - Then, The controller sends the response back to the client:
      - Thymeleaf renders HTML pages.
      - REST APIs return JSON payloads. 
8. Errors and exceptions are handled via global exception handlers, returning appropriate error views or JSON responses.
