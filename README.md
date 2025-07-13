# SS API

A Web API project built with Play Framework (Scala 3).

## 🚀 Setup

### Prerequisites

- Java 21+
- sbt 1.11.3+
- Docker & Docker Compose
- [direnv](https://direnv.net/) (recommended)

### 1. Environment Variables Setup

#### Using direnv (Recommended)

1. Install direnv:
   ```bash
   # macOS
   brew install direnv
   
   # Ubuntu/Debian
   sudo apt install direnv
   
   # For other systems: https://direnv.net/docs/installation.html
   ```

2. Add direnv to your shell configuration:
   ```bash
   # bash
   echo 'eval "$(direnv hook bash)"' >> ~/.bashrc
   
   # zsh
   echo 'eval "$(direnv hook zsh)"' >> ~/.zshrc
   
   # fish
   echo 'direnv hook fish | source' >> ~/.config/fish/config.fish
   ```

3. Copy the environment template file:
   ```bash
   cp .env.example .envrc
   ```

4. Edit the values as needed:
   ```bash
   nano .envrc
   ```

5. Allow direnv to load the environment:
   ```bash
   direnv allow
   ```

#### Manual Setup

Set environment variables manually:

```bash
# Database
export DB_URL="jdbc:postgresql://localhost:5432/ss_api"
export DB_USERNAME="postgres"
export DB_PASSWORD="postgres"

# Redis
export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

# JWT
export JWT_SECRET="your-secure-jwt-secret-key"

# Play Framework
export PLAY_SECRET_KEY="your-secure-play-secret-key"
```

### 2. Start Database Services

```bash
docker-compose up -d
```

### 3. Run the Application

```bash
sbt run
```

## 🛠️ Development

### Running Tests

```bash
sbt test
```

### Compilation

```bash
sbt compile
```

### Code Formatting

```bash
sbt scalafmt
```

## 📚 API Documentation

After starting the application, you can access the Swagger UI at the following URLs:

- Swagger UI: http://localhost:9000/docs
- OpenAPI Specification (YAML): http://localhost:9000/docs/openapi.yaml
- OpenAPI Specification (JSON): http://localhost:9000/docs/openapi.json

## 🗃️ Database

This project uses PostgreSQL. Database evolutions (migrations) are automatically applied on startup.

### Manual Migration Execution

```bash
sbt "runMain play.core.server.ProdServerStart"
```

## 🔐 Authentication

This project implements a JWT-based authentication system:

- Access tokens: Valid for 15 minutes
- Refresh tokens: Valid for 30 days
- Redis-based token blacklist functionality

## 🔧 Configuration

### Environment Variables

| Variable | Description | Default Value |
|----------|-------------|---------------|
| `DB_URL` | Database connection URL | `jdbc:postgresql://localhost:5432/ss_api` |
| `DB_USERNAME` | Database username | `postgres` |
| `DB_PASSWORD` | Database password | `postgres` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `REDIS_PASSWORD` | Redis password | empty |
| `JWT_SECRET` | JWT signing secret key | Required to set |
| `PLAY_SECRET_KEY` | Play Framework secret key | Required to set |

### Production Considerations

- Set `JWT_SECRET` and `PLAY_SECRET_KEY` to sufficiently long (32+ characters) random values
- Configure appropriate passwords for database and Redis
- Use HTTPS communication in production environments

## 🐳 Docker

A Docker Compose configuration is provided for development:

```yaml
# PostgreSQL (port 5432)
# Redis (port 6379)
```

## 📁 Project Structure

```
/
├── app/
│   ├── controllers/     # API controllers
│   ├── models/         # Data models
│   ├── service/        # Business logic
│   └── api/           # API definitions (Tapir)
├── conf/
│   ├── application.conf # Configuration file
│   ├── routes          # Route definitions
│   └── evolutions/     # Database migrations
├── test/              # Test files
├── .envrc             # Environment variables (direnv)
├── .env.example       # Environment variables template
└── docker-compose.yml # Docker setup for development
```

## 🤝 Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the [MIT License](LICENSE).

## 🆘 Troubleshooting

### Common Issues

- **"direnv: error .envrc is blocked"**: Run `direnv allow` in the project directory
- **Database connection failed**: Ensure PostgreSQL is running via `docker-compose up -d`
- **Redis connection failed**: Check if Redis container is running and accessible
- **JWT verification fails**: Verify that `JWT_SECRET` is properly set and consistent

### Getting Help

If you encounter issues not covered here, please:
1. Check the [GitHub Issues](../../issues) for existing solutions
2. Create a new issue with detailed error information
3. Include your environment details (OS, Java version, sbt version)
