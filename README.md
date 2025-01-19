![Frontend](https://img.shields.io/badge/Frontend-Vue.js%20-green.svg)
![Backend](https://img.shields.io/badge/Backend-SpringBoot%20-blue.svg)
![DBMS](https://img.shields.io/badge/DBMS-MySQL%20-orange.svg)
![License](https://img.shields.io/badge/License-GPL&ndash;3.0%20-yellow.svg)


# Seamail: An Email Service
Seamail is an email service designed to enhance user interactions with their email system. It provides secure, efficient, and user-friendly functionalities for managing emails through an intuitive interface.

## Features
- User Registration and Sign-in
- JWT Authentication for secure access
- Manage Emails:
  - Load inbox, outbox, and trashbox
  - Send emails
  - Move emails to trash
  - Delete emails
  - Sort emails by priority or date
  - Filter emails by subject or sender

## Testing
Seamail includes comprehensive unit tests to ensure reliability and functionality. These tests are built using JUnit and Mockito.

### Test Structure
- **Service Tests**: Tests for the business logic in the service layer.
  - `UserServiceTest`: Validates user creation, finding users, and checking non-existent users.
  - `EmailServiceTest`: Verifies email creation, loading inbox/outbox/trashbox, filtering, sorting, and moving emails to trash.

- **Controller Tests**: Tests for the API layers.
  - `AccessControllerTest`: Tests for user authentication endpoints like sign-in and sign-up.
  - `EmailsControllerTest`: Tests for email endpoints like sending, deleting, and moving emails to trash.
