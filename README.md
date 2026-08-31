# Bookfair Reservation System

A full-stack web application designed for managing bookfair stall reservations, vendor profiles, and automated payment processing. Built with Spring Boot (Backend) and React (Frontend).

## Prerequisites

- **Java**: 17 or higher
- **Node.js**: v16 or higher (v18+ recommended)
- **Database**: MySQL 8.0+
- **Maven**: Included via Maven Wrapper (`mvnw`)

---

## 1. Database Setup

The application uses MySQL as the relational database. A database must be created before starting the backend application. Spring Boot's Hibernate will automatically generate the schema tables on startup (`spring.jpa.hibernate.ddl-auto=update`).

Execute the following script in your MySQL client (e.g., MySQL Workbench or CLI) to create the database:

```sql
-- Create the database
CREATE DATABASE IF NOT EXISTS book_reservation;

-- Use the database
USE book_reservation;

-- (Optional) If you are using a specific user, grant privileges:
-- GRANT ALL PRIVILEGES ON book_reservation.* TO 'your_username'@'localhost';
-- FLUSH PRIVILEGES;
```

---

## 2. Configuration & Sensitive Data

For security purposes, sensitive data (such as Database Credentials, OAuth2 Secrets, PayPal API Keys, and Email Passwords) **must not be hardcoded or committed to version control**. 

To run the application locally, you must create a `.env.properties` file in the root of the `backend/` directory. The application is configured to automatically import these values.

**Create a file named `backend/.env.properties` and configure it with your credentials:**

```properties
# Database Configuration
spring.datasource.username=root
spring.datasource.password=your_mysql_password

# JWT Configuration
JWT_SECRET=your_secure_256_bit_random_string_here

# Google OpenID Connect (OIDC) Credentials
GOOGLE_CLIENT_ID=your_google_client_id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your_google_client_secret

# PayPal API Credentials (Sandbox/Live)
paypal.client.id=your_paypal_client_id
PAYPAL_CLIENT_SECRET=your_paypal_client_secret

# Email Service (SMTP)
spring.mail.username=your_email@gmail.com
SPRING_MAIL_PASSWORD=your_gmail_app_password
```

*(Note: Never commit `.env.properties` to a public repository! Ensure it is listed in your `.gitignore`)*

---

## 3. Running the Backend (Spring Boot)

The backend exposes the REST API and manages authentication via Google OIDC. 

1. Open a terminal and navigate to the `backend` folder:
   ```bash
   cd backend
   ```
2. Build and start the Spring Boot application using the Maven wrapper:
   ```bash
   # On Windows
   .\mvnw.cmd spring-boot:run

   # On macOS/Linux
   ./mvnw spring-boot:run
   ```
3. The backend server will start on **`http://localhost:8080`**.

---

## 4. Running the Frontend (React / Vite)

The frontend is a modern React application.

1. Open a new terminal and navigate to the `frontend` folder:
   ```bash
   cd frontend
   ```
2. Install the necessary Node.js dependencies:
   ```bash
   npm install
   ```
3. Start the Vite development server:
   ```bash
   npm run dev
   ```
4. The frontend will be accessible at **`http://localhost:5173`**.

---

## Deployment Instructions

To deploy the application to a production environment:

### Backend Deployment
1. Package the backend into a runnable JAR file:
   ```bash
   cd backend
   .\mvnw.cmd clean package -DskipTests
   ```
2. The generated JAR will be located at `backend/target/backend-0.0.1-SNAPSHOT.jar`.
3. Deploy this JAR to a cloud provider (e.g., AWS Elastic Beanstalk, Heroku, or a standard VPS). Execute it using:
   ```bash
   java -jar backend-0.0.1-SNAPSHOT.jar
   ```
4. *Remember to configure the production environment variables (OIDC, DB credentials) on your cloud host.*

### Frontend Deployment
1. Build the production-ready static files:
   ```bash
   cd frontend
   npm run build
   ```
2. The built static files will be generated in the `frontend/dist/` directory.
3. Deploy the `dist/` directory to a static hosting service like Vercel, Netlify, GitHub Pages, or an Nginx web server.
4. Ensure you configure your hosting provider to handle client-side routing (e.g., redirecting all 404 traffic to `index.html`).
