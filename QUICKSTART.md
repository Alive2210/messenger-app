# 🚀 SECURE MESSENGER — QUICK START GUIDE

## ⚡ One-Command Start (Recommended)

### For 30 Users (Recommended for Testing)
```bash
start-30-users.bat
```

### For 2-3 Users (Minimal Resources)
```bash
start-2-3-users.bat
```

---

## 📋 PREREQUISITES

| Software | Version | Download |
|----------|---------|----------|
| Java | 17+ | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) |
| Node.js | 18+ | [Node.js](https://nodejs.org/) |
| Docker | 20+ | [Docker Desktop](https://www.docker.com/products/docker-desktop) |
| Maven | 3.9+ | [Apache Maven](https://maven.apache.org/download.cgi) |

---

## 🛠️ INSTALLATION

### Step 1: Clone Repository
```bash
git clone <repository-url>
cd messenger-app
```

### Step 2: Configure Environment
Copy environment file:
```bash
copy .env.example .env
```

Edit `.env` with your values:
```env
DB_PASSWORD=your_secure_password
JWT_SECRET=your_jwt_secret_min_64_chars
MINIO_SECRET_KEY=your_minio_secret
```

### Step 3: Start Infrastructure (Docker)
```bash
docker-compose up -d postgres rabbitmq minio redis
```

### Step 4: Start Backend
```bash
mvn spring-boot:run
```

### Step 5: Start Frontend (Development)
```bash
cd messenger-client
npm install
npm run dev
```

---

## 🧪 TESTING API

### Using CURL Test Suite
```bash
test-api.bat
```

### Manual CURL Tests

#### Health Check
```bash
curl -X GET http://localhost:8080/api/auth/health
```

#### Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"email\":\"test@example.com\",\"password\":\"testpass123\"}"
```

#### Login User
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"testuser\",\"password\":\"testpass123\"}"
```

### Load Testing
```bash
load-test.bat
```

---

## 📁 PROJECT STRUCTURE

```
messenger-app/
├── src/main/java/com/messenger/    # Backend source
├── messenger-client/src/           # Frontend source
├── docker-compose.yml              # Docker configuration
├── nginx/                          # Nginx configs
├── scripts/                        # Utility scripts
├── test-api.bat                    # API test suite
├── load-test.bat                   # Load test suite
└── README.md                       # This file
```

---

## 🔧 TROUBLESHOOTING

### Backend Won't Start
1. Check Java version: `java -version` (should be 17+)
2. Check port 8080: `netstat -ano | findstr :8080`
3. Check logs: `backend.log`

### Frontend Won't Start
1. Check Node.js version: `node -v` (should be 18+)
2. Clear node_modules: `rm -rf node_modules && npm install`
3. Check port 5173: `netstat -ano | findstr :5173`

### Database Connection Failed
1. Check PostgreSQL is running: `docker ps | grep postgres`
2. Check credentials in `.env`
3. Restart PostgreSQL: `docker restart messenger-postgres`

### 400 Error on Registration
Check password requirements:
- Minimum 8 characters
- Valid email format
- Username 3-50 characters

---

## 📊 API ENDPOINTS

### Authentication
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/auth/health` | GET | Health check |
| `/api/auth/register` | POST | Register user |
| `/api/auth/login` | POST | Login |
| `/api/auth/refresh` | POST | Refresh token |
| `/api/auth/logout` | POST | Logout |

### Chats
| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/chats` | GET | Get user's chats |
| `/api/chats` | POST | Create chat |
| `/api/chats/{id}` | GET | Get chat by ID |
| `/api/chats/{id}/messages` | GET | Get messages |
| `/api/chats/{id}/messages` | POST | Send message |

---

## 🔐 SECURITY

- **Password Hashing**: BCrypt
- **Authentication**: JWT tokens
- **Encryption**: End-to-end (RSA-2048 + AES-256-GCM)
- **Rate Limiting**: 1000 requests/minute for auth endpoints
- **CORS**: Configured for all origins (development)

---

## 📈 MONITORING

### Health Checks
- Backend: `http://localhost:8080/actuator/health`
- Auth Service: `http://localhost:8080/api/auth/health`

### Logs
- Backend: `backend.log`
- Application: `logs/application.log`
- Errors: `logs/error.log`

### Management Endpoints
- Health: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/metrics`

---

## 🎯 DEVELOPMENT MODES

### Development (H2 Database)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Production (PostgreSQL)
```bash
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

---

## 📝 ADDITIONAL DOCUMENTATION

- [Architecture](ARCHITECTURE.md) — Full architecture documentation
- [Deployment](DEPLOY.md) — Production deployment guide
- [API Documentation](DOCUMENTATION.md) — Complete API reference
- [User Guide](USER_GUIDE.md) — End-user documentation

---

## 🤝 CONTRIBUTING

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add amazing feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

---

## 📄 LICENSE

This project is licensed under the MIT License.

---

## 📞 SUPPORT

For issues and questions:
- GitHub Issues: [Create an issue]
- Email: support@example.com

---

*Generated by AI Engineering Swarm System*
