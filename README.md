# 🧠 AI Study Planner — Backend

<div align="center">

![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)
![OpenAI](https://img.shields.io/badge/OpenAI-GPT--4-412991?style=for-the-badge&logo=openai&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**An intelligent, AI-powered study planning backend that generates personalized, adaptive study schedules using the latest LLM APIs.**

[Features](#-features) · [Architecture](#-architecture) · [Getting Started](#-getting-started) · [API Docs](#-api-documentation) · [Contributing](#-contributing)

</div>

---

## 📌 Overview

**AI Study Planner Backend** is a production-ready RESTful API built with **Java 21** and **Spring Boot 3**, designed to power intelligent study planning applications. It leverages **OpenAI GPT-4** and to dynamically generate personalized study plans based on a student's goals, available time, learning pace, and subject complexity.

All study data is persisted in **PostgreSQL**, with a clean, modular service architecture that makes it easy to extend, test, and deploy.

---

## ✨ Features

- 🤖 **AI-Generated Study Plans** — Calls OpenAI / Claude API to produce structured, topic-by-topic study schedules tailored to each user
- 📅 **Smart Scheduling** — Considers available hours per day, deadlines, and subject difficulty to distribute workload optimally
- 🔄 **Adaptive Replanning** — Regenerates plans dynamically when the user updates goals or misses sessions
- 🗃️ **PostgreSQL Persistence** — All plans, sessions, and user preferences stored relationally with full CRUD support
- 🔐 **Secure API** — JWT-based authentication with role-based access control
- 📄 **OpenAPI / Swagger UI** — Auto-generated interactive API documentation
- 🧪 **Comprehensive Testing** — Unit + integration tests with JUnit 5 and Mockito
- 🐳 **Docker Ready** — Fully containerized with Docker Compose for easy local development

---

## 🏗️ Architecture

```
ai-study-planner-backend/
├── src/
│   └── main/
│       ├── java/com/aistudyplanner/
│       │   ├── config/             # App, Security, AI client config
│       │   ├── controller/         # REST controllers (StudyPlan, User, Session)
│       │   ├── dto/                # Request & Response DTOs
│       │   ├── entity/             # JPA Entities (User, StudyPlan, Topic, Session)
│       │   ├── exception/          # Global exception handling
│       │   ├── repository/         # Spring Data JPA repositories
│       │   ├── service/
│       │   │   ├── ai/             # OpenAI & Claude API integration
│       │   │   ├── plan/           # Study plan generation logic
│       │   │   └── user/           # User management
│       │   └── AiStudyPlannerApplication.java
│       └── resources/
│           ├── application.yml
│           └── db/migration/       # Flyway SQL migrations
├── src/test/                       # Unit & integration tests
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## 🚀 Getting Started

### Prerequisites

| Tool | Version |
|------|---------|
| Java (JDK) | 21+     |
| Maven | 3.9+    |
| PostgreSQL | 14+     |
| Docker (optional) | 24+     |

### 1. Clone the Repository

```bash
git clone https://github.com/YOUR_USERNAME/ai-study-planner-backend.git
cd ai-study-planner-backend
```

### 2. Configure Environment Variables

Create a `.env` file in the project root (never commit this):

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=study_planner
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password

# AI APIs
OPENAI_API_KEY=sk-...
ANTHROPIC_API_KEY=sk-ant-...
AI_MODEL=gpt-4o          # or claude-3-5-sonnet-20241022

# JWT
JWT_SECRET=your_super_secret_key_here
JWT_EXPIRATION_MS=86400000
```


## 🤝 Contributing

Contributions are welcome! Please follow these steps:

1. **Fork** the repository
2. Create a feature branch: `git checkout -b feature/your-feature-name`
3. Commit your changes: `git commit -m 'feat: add your feature'`
4. Push to your branch: `git push origin feature/your-feature-name`
5. Open a **Pull Request**

Please follow the [Conventional Commits](https://www.conventionalcommits.org/) specification for commit messages.

---

## 📋 Roadmap

- [ ] Multi-AI provider support (switch between OpenAI & Claude dynamically)
- [ ] Progress tracking & session completion marking
- [ ] Quiz/assessment generation per topic
- [ ] Email reminders for study sessions
- [ ] Analytics dashboard API
- [ ] Rate limiting & API key management

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

## 👤 Author

**Your Name**
- GitHub: [@saifahmad01](https://github.com/saifahmad01)
- LinkedIn: [saif-ahmad8018](https://linkedin.com/in/saif-ahmad8018)

---

<div align="center">
  <sub>Built with ☕ Java, 🌱 Spring Boot, and 🤖 AI</sub>
</div>
