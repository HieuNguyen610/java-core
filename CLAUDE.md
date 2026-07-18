# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pure Java SE REST API without frameworks (no Spring, Quarkus, or Jakarta EE). Uses `com.sun.net.httpserver` for HTTP, raw JDBC for MySQL, and manual dependency management.

## Build Commands

```powershell
cd core-java-api
mvn compile                                    # Compile
mvn package -DskipTests                        # Build JAR
mvn compile exec:java -Dexec.mainClass="com.example.api.corejavaproject.server.Main"  # Run directly
```

Run with custom port: `java -jar target/core-java-api-1.0.0.jar 9090`

## Architecture

```
Main.java → ProductHttpServer → ProductService → ProductRepository → MySQL
                (com.sun.net.httpserver)
```

- **HTTP Layer** (`ProductHttpServer.java`): Manual HTTP routing, manual JSON serialization using string formatting
- **Service Layer** (`ProductService.java`): Business logic, manual dependency instantiation
- **Repository Layer** (`ProductRepository.java`): Raw JDBC with PreparedStatement (no JPA/Hibernate)
- **Model** (`Product.java`): Plain POJO, no annotations
- **DB Layer** (`DatabaseConnection.java`, `DbConfig.java`): JDBC connection management

## Database Configuration

MySQL connection configured via environment variables:
- `DB_HOST` - defaults to `localhost`
- `DB_PORT` - defaults to `3306`
- `DB_NAME` - defaults to `core_java_db`
- `DB_USER` - defaults to `root`
- `DB_PASSWORD` - defaults to empty

Or create a `DbConfig` directly and pass to `ProductRepository(DbConfig)`.

## API Endpoints

Server starts on port 8080 (configurable via args):

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/products | List all products |
| GET | /api/products/{id} | Get product by ID |
| POST | /api/products | Create product (JSON body) |
| PUT | /api/products/{id} | Update product |
| DELETE | /api/products/{id} | Delete product |
| GET | /api/products/search?name=keyword | Search by name |

## SQL Schema

Located at `sql/schema.sql`. Run against MySQL before starting the app:
```bash
mysql -u root -p < sql/schema.sql
```

Or use Docker:
```bash
docker run -d --name mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password -e MYSQL_DATABASE=core_java_db mysql:8
```

## Key Implementation Notes

- No annotations - request routing is handled manually in `ProductHttpServer` using `exchange.getRequestURI().getPath()`
- JSON is manually formatted using `String.format()` in `toJson()` method
- Dependency injection is manual - `ProductService` instantiates `ProductRepository` directly in constructor
- `PreparedStatement` is used for all SQL to prevent injection
- `DbConfig.fromEnvironment()` provides the standard environment variable convention