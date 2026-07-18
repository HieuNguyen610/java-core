# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Pure Java SE REST API without frameworks (no Spring, Quarkus, or Jakarta EE). Uses `com.sun.net.httpserver` for HTTP, raw JDBC for Oracle 19c, and manual dependency management.

## Build Commands

### Option 1: Using Maven (recommended for dependency management)
```powershell
cd core-java-api
mvn compile                                    # Compile
mvn package -DskipTests                        # Build JAR
mvn compile exec:java -Dexec.mainClass="com.example.api.corejavaproject.server.Main"  # Run directly
```

### Option 2: Manual build (without Maven)
```powershell
cd core-java-api
.\build.bat compile    # Compile Java sources
.\build.bat package    # Build JAR with dependencies
.\build.bat run         # Run directly
.\build.bat run 9090   # Run on custom port
```

## Architecture

```
Main.java → ProductHttpServer → ProductService → ProductRepository → Oracle 19c
                (com.sun.net.httpserver)
```

- **HTTP Layer** (`ProductHttpServer.java`): Manual HTTP routing, manual JSON serialization using string formatting
- **Service Layer** (`ProductService.java`): Business logic, manual dependency instantiation
- **Repository Layer** (`ProductRepository.java`): Raw JDBC with PreparedStatement (no JPA/Hibernate)
- **Model** (`Product.java`): Plain POJO, no annotations
- **DB Layer** (`DbConfig.java`, `DatabaseConnection.java`): JDBC connection management for Oracle

## Dependencies

External JARs are stored in `lib/` directory:
- `lib/ojdbc8.jar` - Oracle JDBC Driver (19.8.0.0)

## Database Configuration (Oracle 19c)

Oracle connection configured via environment variables:
- `DB_HOST` - defaults to `localhost`
- `DB_PORT` - defaults to `1521` (Oracle default)
- `DB_NAME` - defaults to `ORCL` (Service Name or SID)
- `DB_USER` - defaults to `system`
- `DB_PASSWORD` - defaults to empty

Or create a `DbConfig` directly:
```java
DbConfig config = new DbConfig("localhost", 1521, "ORCL", "system", "password");
ProductRepository repository = new ProductRepository(config);
```

### Oracle Connection String Format
```
jdbc:oracle:thin:@//host:port/serviceName
jdbc:oracle:thin:@host:port:SID
```

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

Located at `sql/schema.sql`. Run against Oracle 19c before starting the app:
```bash
sqlplus username/password@//host:1521/ORCL @sql/schema.sql
```

Or use Docker for Oracle XE:
```bash
docker run -d --name oracle19c -p 1521:1521 -e ORACLE_PASSWORD=password gvenzl/oracle-xe:19
```

## Key Implementation Notes

- No annotations - request routing is handled manually in `ProductHttpServer` using `exchange.getRequestURI().getPath()`
- JSON is manually formatted using `String.format()` in `toJson()` method
- Dependency injection is manual - `ProductService` instantiates `ProductRepository` directly in constructor
- `PreparedStatement` is used for all SQL to prevent injection
- `DbConfig.fromEnvironment()` provides the standard environment variable convention
- Oracle uses IDENTITY columns (Oracle 12c+) for auto-increment, not AUTO_INCREMENT
- Sequence `products_seq` is created to support ID generation