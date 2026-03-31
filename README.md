# 🏥 MediCore HMS — Hospital Management System

A full-stack Hospital Management System built with Java Servlets, JDBC, MySQL, and a cinematic HTML/CSS/JS frontend, deployed on Apache Tomcat 10.

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|-----------|
| Frontend | HTML, CSS, JavaScript |
| Backend | Java Servlets (Jakarta EE) |
| Database | MySQL |
| Server | Apache Tomcat 10 |
| Connectivity | JDBC (mysql-connector-j-9.6.0) |

---

## 📁 Project Structure

```
MediCore-HMS/
├── src/
│   └── com/hospital/servlet/
│       ├── AuthServlet.java
│       ├── PatientServlet.java
│       ├── DoctorServlet.java
│       ├── AppointmentServlet.java
│       ├── BillingServlet.java
│       ├── DepartmentServlet.java
│       ├── ReportServlet.java
│       ├── SqlServlet.java
│       ├── CORSFilter.java
│       └── DBConnection.java
├── WebContent/
│   ├── index.html
│   ├── api.js
│   └── WEB-INF/
│       ├── web.xml
│       └── lib/
│           └── mysql-connector-j-9.6.0.jar
├── .env.example
└── .gitignore
```

---

## ⚙️ Features

- 🔐 Authentication (Login/Logout)
- 🧑‍⚕️ Patient Management (Add, View, Update, Delete)
- 👨‍⚕️ Doctor Management
- 📅 Appointment Scheduling
- 🏢 Department Management
- 💳 Billing System
- 📊 Reports Generation
- 🔒 CORS Filter for API Security

---

## 🛠️ Setup Instructions

### Prerequisites
- Java JDK 11+
- Apache Tomcat 10
- MySQL Server
- MySQL Connector JAR (included in `WebContent/WEB-INF/lib/`)

### Database Setup
1. Create a MySQL database named `hospital_db`
2. Import your SQL schema

### Environment Variables
Copy `.env.example` and set your database credentials:
```
DB_URL=jdbc:mysql://localhost:3306/hospital_db
DB_USER=your_mysql_username
DB_PASS=your_mysql_password
```

### Deployment
1. Compile the Java source files:
```cmd
javac -cp "path\to\tomcat\lib\servlet-api.jar;WebContent\WEB-INF\lib\mysql-connector-j-9.6.0.jar" -d "WebContent\WEB-INF\classes" src\com\hospital\servlet\*.java
```
2. Copy the `WebContent` folder to `tomcat/webapps/hospital/`
3. Start Tomcat and visit `http://localhost:8080/hospital/`

---

## 👨‍💻 Author

**Sivanandh Kondisetty**  
B.Tech AI Student — Vignan's Lara Institute of Technology & Science  
GitHub: [@SivanandhKondisetty](https://github.com/SivanandhKondisetty)

---

## 📄 License

This project is open source and available under the [MIT License](LICENSE).
