# API Orchestrator (Postman-Lite) — Full Stack Project

A lightweight **Postman-like API client platform** with a **Spring Boot backend** and **React UI**.  
The system allows users to execute HTTP requests, capture full responses (status, headers, body), handle timeouts/errors, and persist request history.

---

## 🧩 Architecture

React UI (Vite)
|
v
Spring Boot API (WebClient Engine)
|
v
External APIs (3rd-party URLs)

Persistence:

H2 (dev) / PostgreSQL-ready (prod)


---

## 🛠 Tech Stack

**Backend**
- Java 17  
- Spring Boot  
- WebClient (Reactor Netty)  
- Spring Data JPA  
- H2 (dev)  
- Maven  

**Frontend**
- React (Vite)  
- Fetch API  
- Simple CSS  

---

## 🚀 How to Run Locally

### 1️⃣ Run Backend

```bash
cd api-orchestrator-backend
mvn spring-boot:run


Backend runs on:

http://localhost:8080


Run Frontend

cd api-orchestrator-ui
npm install
npm run dev


UI runs on

http://localhost:5173


Core Features

Execute HTTP requests (GET, POST, PUT, DELETE)
Custom headers & request body
Timeout configuration
Capture status, headers, body, response time
Error handling (4xx/5xx, timeouts)
Persist request history
Click history to re-run requests
Clean UI


<img width="1894" height="919" alt="image" src="https://github.com/user-attachments/assets/cef59c32-1ebb-4b52-86ac-d772a1a7d768" />
