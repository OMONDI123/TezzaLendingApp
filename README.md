# Tezza Lending Application

A Spring Boot-based lending application that automates loan management, including product creation, loan disbursement, repayment handling, and notifications.

---

## Table of Contents

- [Prerequisites](#prerequisites)
- [Clone the Project](#clone-the-project)
- [Database Setup](#database-setup)
- [Application Configuration](#application-configuration)
- [Running the Application](#running-the-application)
- [Testing the Endpoints](#testing-the-endpoints)
- [Architecture Overview](#architecture-overview)
- [Troubleshooting](#troubleshooting)

---

## Prerequisites

Make sure the following are installed before you begin:

- java 11
- Maven (or use the included `mvnw` / `mvnw.cmd` wrapper — no separate install needed)
- PostgreSQL 14+, with access to a superuser account
- Git

---

## Clone the Project

```bash
git clone https://github.com/OMONDI123/TezzaLendingApp.git
cd TezzaLendingApp
```

The database backup file `tezzaapp.dmp` is located in the project root directory. You will use it in the next step.

---

## Database Setup

### Linux / macOS

Switch to the PostgreSQL system user:

```bash
sudo su postgres
```

Open the PostgreSQL prompt:

```bash
psql
```

Create the user and database:

```sql
CREATE USER tezzauser WITH PASSWORD 'tezza#$2026test';
CREATE DATABASE tezzaapp WITH OWNER tezzauser;
```

Expected output:

```
CREATE ROLE
CREATE DATABASE
```

Exit PostgreSQL, then exit the postgres user:

```sql
\q
```

```bash
exit
```

Restore the database dump from the project root:

```bash
psql -U tezzauser -d tezzaapp -f tezzaapp.dmp
```

Enter the password `tezza#$2026test` when prompted.

If you get a syntax error (the dump may be in binary format), try:

```bash
pg_restore -U tezzauser -d tezzaapp tezzaapp.dmp
```

---

### Windows

Open Command Prompt or PowerShell as Administrator (`Windows + X` → "Terminal (Admin)").

Connect to PostgreSQL as the admin user:

```cmd
psql -U postgres
```

Enter your PostgreSQL admin password when prompted.

Create the user and database:

```sql
CREATE USER tezzauser WITH PASSWORD 'tezza#$2026test';
CREATE DATABASE tezzaapp WITH OWNER tezzauser;
```

Expected output:

```
CREATE ROLE
CREATE DATABASE
```

Exit PostgreSQL:

```sql
\q
```

Restore the dump from the project root:

```cmd
psql -U tezzauser -d tezzaapp -f tezzaapp.dmp
```

If you get a syntax error, try:

```cmd
pg_restore -U tezzauser -d tezzaapp tezzaapp.dmp
```

---

## Application Configuration

Create the file `src/main/resources/application.properties` in the project directory and paste in the following configuration. Adjust any paths marked with a comment to match your local environment.

```properties
# ─── JWT ───────────────────────────────────────────────────────────────────
bezkoder.app.jwtSecret=baksksksYAt5511514Ajsqeweweoqpq00922892
bezkoder.app.jwtExpirationMs=711200000

# ─── Server ────────────────────────────────────────────────────────────────
server.port=8083
server.servlet.context-path=/tezza
server.max-http-header-size=64KB

# ─── Database ──────────────────────────────────────────────────────────────
spring.datasource.url=jdbc:postgresql://localhost:5432/tezzaapp
spring.datasource.username=tezzauser
spring.datasource.password=tezza#$2026test

# ─── JPA / Hibernate ───────────────────────────────────────────────────────
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQL95Dialect
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.format_sql=true

# ─── File Storage ──────────────────────────────────────────────────────────
# Linux/macOS: use the path below as-is
# Windows: change to a Windows path, e.g. C:/uploads/documents/
documentsUploadDir=/var/www/html/documents/uploads

# The public URL used to serve uploaded files
# Update the host/port to match your environment
filePath=http://localhost/documents/uploads/

# ─── Spring MVC ────────────────────────────────────────────────────────────
spring.mvc.pathmatch.matching-strategy=ant_path_matcher

# ─── Domain ────────────────────────────────────────────────────────────────
# Update this to your server's public IP or domain
domain.url=http://84.8.129.26

# ─── Scheduler ─────────────────────────────────────────────────────────────
# Delay in milliseconds before the loan processor runs (30 seconds)
scheduler.loan.process.delay=30000
scheduler.loan.pageSize=50
scheduler.loan.calculatorReturnsDelta=true

# ─── Google Drive Backup ────────────────────────────────────────────────────
# Path to your Google Drive service account credentials JSON file
# Linux/macOS example: ~/driven-binder-491315-t8-ffb53bfdd5b3.json
# Windows example:     C:/Users/YourName/driven-binder-491315-t8-ffb53bfdd5b3.json
google.drive.credentials.file.path=~/driven-binder-491315-t8-ffb53bfdd5b3.json
google.drive.folder.name=DB_BACKUPS
```

### File path notes

| Property | Linux / macOS | Windows |
|---|---|---|
| `documentsUploadDir` | `/var/www/html/documents/uploads` | `C:/uploads/documents/` |
| `google.drive.credentials.file.path` | `~/driven-binder-491315-t8-ffb53bfdd5b3.json` | `C:/Users/YourName/driven-binder-491315-t8-ffb53bfdd5b3.json` |

Make sure the `documentsUploadDir` folder exists and the application has write permission to it:

**Linux / macOS:**

```bash
sudo mkdir -p /var/www/html/documents/uploads
sudo chown -R $USER /var/www/html/documents/uploads
```

**Windows:** Create the folder manually in File Explorer or via PowerShell:

```powershell
New-Item -ItemType Directory -Path "C:\uploads\documents" -Force
```

---

## Running the Application

Choose whichever method you are comfortable with.

### Command Line (Maven Wrapper)

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

**Windows:**

```cmd
mvnw.cmd spring-boot:run
```

### Eclipse

1. File → Import → Existing Maven Projects
2. Browse to the project root, click Finish
3. Right-click the project → Run As → Spring Boot App

### IntelliJ IDEA

1. File → Open → select the project root
2. Wait for Maven to finish pulling dependencies
3. Open the main application class and click Run

---

## Testing the Endpoints

Once the application is running, open Swagger UI in your browser:

```
http://localhost:8083/tezza/swagger-ui.html
```

From there you can run GET, POST, PUT, and DELETE requests directly against the live API.
Get the the access token by using the SUPER ADMIN ROLE WITH ALL ACCESS CONTROL BELOW
{
  "password": "bunde123@#Kenya",
  "userName": "omondiaustinebunde@gmail.com"
}

---

## Architecture Overview

The application is structured around four core modules:

**Product Module** — manages loan product configurations including tenure types (fixed/flexible), fee structures (service fees, daily fees, late fees), and configurable grace periods.

**Loan Management** — handles loan lifecycle including single lump-sum and installment-based loans, consolidated billing cycles, automated sweep jobs for overdue detection, and state transitions: Open → Overdue → Written Off / Closed / Cancelled.

**Customer Profile Module** — stores customer personal details, financial history, and manages per-customer loan limits based on creditworthiness and repayment behavior.

**Notification Module** — event-driven notifications for loan creation, due date reminders, repayment confirmations, and overdue alerts, with support for multiple channels (email, SMS, push) and configurable templates.

The scheduler (`scheduler.loan.process.delay`) runs background sweep jobs at a configurable interval to identify overdue loans, apply late fees, and trigger appropriate state transitions and notifications.

---

## Troubleshooting

**Database `tezzaapp` does not exist**

```bash
psql -U postgres -c "CREATE DATABASE tezzaapp WITH OWNER tezzauser;"
```

**Role/user `tezzauser` does not exist**

```bash
psql -U postgres -c "CREATE USER tezzauser WITH PASSWORD 'tezza#\$2026test';"
```

**Permission denied for database**

```bash
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE tezzaapp TO tezzauser;"
```

**Port 8083 already in use**

Linux / macOS:

```bash
sudo lsof -i :8083
sudo kill -9 <PID>
```

Windows:

```cmd
netstat -ano | findstr :8083
taskkill /PID <PID> /F
```

**Maven can't resolve dependencies**

```bash
mvn clean install -U
```

**`documentsUploadDir` write errors** — ensure the directory exists and the process running the app has write permissions to it (see [File path notes](#file-path-notes) above).

**App won't start** — check the console output first; it almost always says exactly what's wrong. The usual suspects are PostgreSQL not running, a credentials mismatch in `application.properties`, or the port already being in use.

---

## Verification Checklist

- [ ] Database restored without errors
- [ ] `application.properties` created with correct values
- [ ] Upload directory exists and is writable
- [ ] Application starts cleanly on port 8083
- [ ] Swagger UI loads at `http://localhost:8083/tezza/swagger-ui.html`
- [ ] All endpoints appear in Swagger
- [ ] GET / POST / PUT / DELETE requests work from Swagger

---

## Support

Still stuck? Check in this order:

1. Is PostgreSQL actually running?
2. Do the credentials match `tezzauser` / `tezza#$2026test`?
3. Are you on java 11? (`java -version`)
4. Does the upload directory exist and have correct permissions?
5. What does the console log actually say?