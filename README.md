# EduTrack - 1-on-1 Mentoring Platform

## Overview

EduTrack is a comprehensive online mentoring platform designed to facilitate personalized, 1-on-1 educational experiences between mentees and mentors. Built with a focus on flexible schedule management and seamless user experience, EduTrack empowers students to connect with expert educators for customized learning journeys while providing robust administrative tools for platform management.

## Key Features

### For Mentees
- **User Registration & Profile Management**: Create and maintain detailed student profiles
- **Course & Mentor Discovery**: Browse and search through available courses and mentor profiles
- **1-on-1 Course Enrollment**: Enroll in personalized courses with automatic session scheduling
- **Schedule Management**: Request schedule changes and manage upcoming sessions
- **Real-time Notifications**: Receive updates on schedule changes and course activities

### For Mentors
- **Mentor Dashboard**: Comprehensive interface for managing courses and student interactions
- **Schedule Request Management**: Review, approve, or reject mentee schedule change requests
- **Email Notifications**: Automated alerts for schedule requests and platform updates
- **Course Management**: Create and manage 1-on-1 course offerings
- **Conflict Prevention**: Built-in validation to prevent scheduling conflicts

### For Managers
- **Administrative Dashboard**: Complete oversight of platform operations
- **User Management**: Manage all user accounts, roles, and permissions
- **Course Lifecycle Management**: Oversee course creation, modification, and completion
- **Application Management**: Handle course applications and enrollment processes
- **Course Protection**: Lock active courses to prevent unauthorized modifications

## Tech Stack

- **Backend Framework**: Java with Spring Boot
- **Data Access**: Spring Data JPA
- **Database**: MySQL
- **Template Engine**: Thymeleaf
- **Build Tool**: Maven
- **Authentication**: Spring Security
- **Email Services**: Spring Boot Mail

## Architecture

EduTrack follows a layered, feature-driven architecture implementing the Controller-Service-Repository pattern. This design ensures clear separation of concerns, with Controllers handling HTTP requests and responses, Services containing business logic and transaction management, and Repositories managing data persistence. The modular structure promotes maintainability and scalability while supporting the complex scheduling and user management requirements of the mentoring platform.

## Setup & Installation

1. **Clone the Repository**
   ```bash
   git clone https://github.com/vinhnguyen2005/EduTrack.git
   cd EduTrack
   ```

2. **Configure Database Connection**
   - Create a MySQL database named `edutrack`
   - Update `src/main/resources/application.properties` with your database credentials:
   ```properties
   spring.datasource.url=jdbc:mysql://localhost:3306/edutrack
   spring.datasource.username=your_username
   spring.datasource.password=your_password
   ```

3. **Configure Email Settings**
   - Update email configuration in `application.properties`:
   ```properties
   spring.mail.host=your_smtp_host
   spring.mail.username=your_email
   spring.mail.password=your_email_password
   ```

4. **Install Dependencies and Build**
   ```bash
   mvn clean install
   ```

5. **Run Database Migrations**
   ```bash
   mvn spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.hibernate.ddl-auto=create
   ```

6. **Start the Application**
   ```bash
   mvn spring-boot:run
   ```

7. **Access the Application**
   - Open your browser and navigate to `http://localhost:8080`
   - The application will be running with the default Spring Boot configuration

## Team Members

- **Bùi Việt Dũng** - *Project Manager*
- **Phạm Tùng Dương** - *Developer*
- **Nguyễn Thành Vinh** - *Developer*
- **Nguyễn Ngọc Hải Hoàn** - *Developer*
- **Lê Phương Linh Nga** - *Developer*

## Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/edutrack/
│   │       ├── controller/     # REST Controllers
│   │       ├── service/        # Business Logic Layer
│   │       ├── repository/     # Data Access Layer
│   │       ├── model/          # Entity Classes
│   │       └── config/         # Configuration Classes
│   └── resources/
│       ├── templates/          # Thymeleaf Templates
│       ├── static/             # CSS, JS, Images
│       └── application.properties
└── test/                       # Unit and Integration Tests
```

## Contributing

This project was developed as part of a university software engineering course. For any questions or contributions, please contact the development team.

## License

This project is developed for educational purposes as part of a university coursework.
