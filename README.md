# Buy-01

Buy-01 is a full-stack marketplace application. The frontend is an Angular application, and the backend is a set of Spring Boot services that communicate through Eureka service discovery, an API gateway, MongoDB, and Kafka.

## Features

- User registration, login, and profile management
- JWT-protected API requests through the gateway
- Product listing, creation, editing, deletion, and ownership views
- Product image upload and media management
- MongoDB persistence separated by service
- Event-based product/media communication through Kafka

## Architecture

```text
Angular frontend (:4200)
          |
          v
API gateway (:8080)
     |       |       |
     v       v       v
  User    Product   Media
 (:8081)  (:8082)  (:8083)
     \       |       /
      +-- Eureka (:8761)

MongoDB (:27017)       Kafka broker (:9092)
```

The gateway is the public backend entry point. The frontend uses these API prefixes:

- `/api/v1/auth` for authentication
- `/api/v1/users` for user profiles
- `/api/v1/products` for products
- `/api/v1/media` for media operations

## Repository layout

```text
frontend/                 Angular client
backend/
  gateway/                API gateway and JWT filtering
  server/                 Eureka service registry
  userService/            Authentication and user profiles
  productService/         Product management
  mediaService/           Image/media management
  docker-compose.yml      Local backend infrastructure
```

## Prerequisites

- Docker and Docker Compose
- Node.js compatible with Angular 21
- npm 11.8.0 or a compatible npm version
- Java 17 or newer for running services outside Docker

## Quick start with Docker

1. Start the backend services and infrastructure:

   ```bash
   cd backend
   docker compose up --build
   ```

2. In a second terminal, install frontend dependencies and start Angular:

   ```bash
   cd frontend
   npm install
   npx ng serve
   ```

3. Open <http://localhost:4200/>.

The backend gateway is available at <http://localhost:8080/>. Eureka is available at <http://localhost:8761/> and MongoDB is exposed on port `27017`.

To stop the stack:

```bash
cd backend
docker compose down
```

The MongoDB files are stored in `backend/data/db`, so `docker compose down` does not remove local database data. To remove the local database volume data as well, delete that directory only after confirming that the data is no longer needed.

## Backend configuration

Docker Compose supports these environment variables:

| Variable | Default | Purpose |
| --- | --- | --- |
| `MONGO_INITDB_ROOT_USERNAME` | `admin` | MongoDB root username |
| `MONGO_INITDB_ROOT_PASSWORD` | `password` | MongoDB root password |
| `SPRING_MONGODB_URI` | `mongodb://admin:password@mongodb:27017/?authSource=admin` | MongoDB connection URI for user, product, and media services |
| `SPRING_MONGODB_DATABASE` | Service-specific database | Database name override for the service containers |

Example:

```bash
cd backend
MONGO_INITDB_ROOT_PASSWORD='change-this-password' \
SPRING_MONGODB_URI='mongodb://admin:change-this-password@mongodb:27017/?authSource=admin' \
docker compose up --build
```

The media service also requires valid Cloudinary credentials for image uploads. Configure `cloudinary.cloud_name`, `cloudinary.api_key`, and `cloudinary.api_secret` through a local, untracked configuration mechanism before using media uploads. Do not commit credentials or reuse credentials that have been exposed in source control; rotate them if necessary.

## Running backend services without Docker

MongoDB, Kafka, and a reachable Eureka server are still required. Each service contains a Maven wrapper, so a service can be started with:

```bash
cd backend/<service-directory>
./mvnw spring-boot:run
```

Replace `<service-directory>` with `server`, `gateway`, `userService`, `productService`, or `mediaService`. To build a service:

```bash
./mvnw clean package
```

Service ports are configured as follows:

| Service | Port | Database |
| --- | ---: | --- |
| Eureka server | 8761 | None |
| Gateway | 8080 | None |
| User service | 8081 | `userDB` |
| Product service | 8082 | `productDB` |
| Media service | 8083 | `mediaDB` |

In the normal Docker Compose setup, the internal services are reached through the gateway and service discovery rather than directly from the host.

## Frontend development

From `frontend/`:

```bash
npm install
npm start                  # development server at http://localhost:4200/
npm run build              # production build in dist/
npm run watch              # development build in watch mode
npm test                   # unit tests with Vitest through Angular CLI
```

The frontend currently targets `http://localhost:8080/api/v1`. If the gateway is deployed elsewhere, update the API base URLs in the frontend service classes or introduce an Angular environment configuration before deployment.

## Testing

Run backend tests for an individual service with:

```bash
cd backend/<service-directory>
./mvnw test
```

Run frontend unit tests with:

```bash
cd frontend
npm test
```

## Troubleshooting

- If the frontend cannot reach the API, confirm that the gateway is running on port `8080` and that the browser is using `http://localhost:4200`.
- If services cannot register or discover one another, confirm that all containers are on the Compose network and that Eureka started successfully on port `8761`.
- If product or media operations fail, check that Kafka is running and that MongoDB is accepting connections.
- If image uploads fail while the rest of the application works, verify the Cloudinary configuration.
- To inspect backend logs, use `docker compose logs -f <service>` from `backend/`.

## Security notes

The checked-in development configuration contains default MongoDB credentials, a development JWT secret, TLS keystore credentials, and Cloudinary settings. Treat these values as development-only. Before sharing or deploying the application, rotate exposed credentials and move secrets to environment variables or a secrets manager.