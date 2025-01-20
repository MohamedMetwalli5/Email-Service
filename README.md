![Frontend](https://img.shields.io/badge/Frontend-React.js%20-blue.svg)
![Backend](https://img.shields.io/badge/Backend-SpringBoot%20-green.svg)
![DBMS](https://img.shields.io/badge/DBMS-MySQL%20-orange.svg)
![License](https://img.shields.io/badge/License-GPL&ndash;3.0%20-yellow.svg)

<div align="center">
  <img src="https://github.com/user-attachments/assets/3438953d-9596-41fd-9570-2c0ec3713657" alt="The Website Logo" width="200" />
</div>

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

## Logo Idea
The Seamail logo combines an envelope with dynamic wave patterns, symbolizing seamless communication. It reflects the efficient, modern, and user-friendly email service.

![Seamail - Website Logo](https://github.com/user-attachments/assets/c791622c-62a6-4ac0-95da-13996c60020f)
