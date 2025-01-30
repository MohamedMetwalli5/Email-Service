![Frontend](https://img.shields.io/badge/Frontend-React.js%20-blue.svg)
![Backend](https://img.shields.io/badge/Backend-SpringBoot%20-green.svg)
![DBMS](https://img.shields.io/badge/DBMS-MySQL%20-orange.svg)
![License](https://img.shields.io/badge/License-GPL&ndash;3.0%20-yellow.svg)

<div align="center">
  <img src="https://github.com/user-attachments/assets/3438953d-9596-41fd-9570-2c0ec3713657" alt="The Website Logo" width="200" />
</div>

# Seamail: An Email Service
Seamail is an email service designed to enhance user interactions with their email system. It provides secure, efficient, and user-friendly functionalities for managing emails through an intuitive interface.


# Features
- **User Registration & Sign-in:** Secure registration and login process for users.
- **JWT Authentication:** Ensures secure access to the platform with token-based authentication.
- **Multi-language Support:** Enhances accessibility by offering the platform in multiple languages.
- **Email Management:** View and manage inbox, outbox, and trashbox for efficient email organization.
- **Email Actions:** Send, move to trash, and delete emails with ease.
- **Email Sorting & Filtering:** Sort emails by priority or date, and filter them by subject or sender.
- **Account Deletion:** Allows users to permanently delete their accounts if desired.
- **Password Management:** Option to securely change user passwords to maintain account security.


# Setup Instructions
If you want to replicate the project on your local environment, follow these steps:
## Database Setup
Run the `Tables.sql` script in the "SQL Scripts" folder to set up your database tables.

## Backend Setup
1. Navigate to the Backend Directory 
```cd backendemailservice```
2. Navigate using `cd src\main\resources`, and Configure the `application.properties` file like:
   - Setting up the MySQL connection details (URL, username, password).
   - Configuring JPA settings for database interaction and Hibernate.
   - Setting a secret key for JWT-based authentication.
3. Navigate using `cd src\test\resources`, and Configure the `application-test.properties` file.
4. Clean and Install dependencies
```mvn clean install```
5. Navigate to the `target` folder using ```cd target```
6. Run the JAR file ```java -jar backendemailservice-0.0.1-SNAPSHOT.jar```
7. The backend will start on ```http://localhost:8081```

## Frontend Setup
1. Navigate to the Frontend Directory
``` cd frontend-email-service ```
2. Install Dependencies
``` npm install ```
3. Configure Environment Variables, 
Create a ```.env``` file in the frontend directory with the following content:
```
VITE_BACKEND_API_URL=http://localhost:8081
```
4. Start the Frontend
``` npm run dev ```
5. The frontend will start on http://localhost:8080


# Testing
Seamail includes comprehensive unit tests to ensure reliability and functionality. These tests are built using JUnit and Mockito.

## Test Structure
- **Service Tests**: Tests for the business logic in the service layer.
  - `UserServiceTest`: Validates user creation, finding users, and checking non-existent users.
  - `EmailServiceTest`: Verifies email creation, loading inbox/outbox/trashbox, filtering, sorting, and moving emails to trash.

- **Controller Tests**: Tests for the API layers.
  - `AccessControllerTest`: Tests for user authentication endpoints like sign-in and sign-up.
  - `EmailsControllerTest`: Tests for email endpoints like sending, deleting, and moving emails to trash.

# Screenshot
![image](https://github.com/user-attachments/assets/55aa11a3-e7f1-4eb8-92cf-c3c7083b3525)

# Logo Idea
The Seamail logo combines an envelope with dynamic wave patterns, symbolizing seamless communication. It reflects the efficient, modern, and user-friendly email service.

![Seamail - Website Logo](https://github.com/user-attachments/assets/c791622c-62a6-4ac0-95da-13996c60020f)
