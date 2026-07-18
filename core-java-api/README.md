# Core Java API with Oracle 21c XE

Pure Java SE REST API without frameworks, connected to Oracle 21c XE database.

> **Note:** Using Oracle 21c XE (gvenzl/oracle-xe:21-slim) instead of 19c because the 19c slim variant is not available on Docker Hub. Oracle 21c XE is also FREE and fully compatible.

## Oracle XE - Licensing

**Oracle XE (Express Edition) is FREE** - no license required.

### XE Limitations (free):
- 12GB user data max
- 2GB RAM max
- 2 CPU threads max

## Quick Start

### 1. Start Oracle 21c XE (FREE - no license required)

```powershell
cd core-java-api

# Pull image and start container
docker-compose up -d

# Wait 5-10 minutes for Oracle to initialize
docker exec oracle19c healthcheck.sh
```

### 2. Create Database Schema

```powershell
# Run schema automatically (mounted via docker-compose)
# Or manually:

# Connect using SQL*Plus
docker exec -it oracle19c sqlplus system/oracle19c_password@//localhost:1521/ORCLPDB1

# Run schema inside SQL*Plus:
# SQL> @/opt/oracle/scripts/startup/schema.sql
```

### 3. Build & Run

**Using Maven:**
```powershell
mvn compile exec:java -Dexec.mainClass="com.example.api.corejavaproject.server.Main"
```

**Using build.bat (no Maven):**
```powershell
.\build.bat compile
.\build.bat run
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | localhost | Oracle host |
| DB_PORT | 1521 | Oracle port |
| DB_NAME | ORCLPDB1 | Service Name (Pluggable Database) |
| DB_USER | system | Username |
| DB_PASSWORD | oracle19c_password | Password |

Set before running:
```powershell
$env:DB_PASSWORD = "oracle19c_password"
.\build.bat run
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/products | List all products |
| GET | /api/products/{id} | Get product by ID |
| POST | /api/products | Create product |
| PUT | /api/products/{id} | Update product |
| DELETE | /api/products/{id} | Delete product |
| GET | /api/products/search?name=keyword | Search by name |

## Example API Usage

```bash
# List all products
curl http://localhost:8080/api/products

# Get product by ID
curl http://localhost:8080/api/products/1

# Create product
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"iPhone","price":999.99,"description":"Smartphone"}'

# Search products
curl "http://localhost:8080/api/products/search?name=laptop"
```

## Project Structure

```
core-java-api/
├── lib/                        # External JARs (Oracle JDBC)
│   └── ojdbc8.jar
├── scripts/                    # Helper scripts
│   └── setup-oracle.bat
├── sql/                        # Database scripts
│   └── schema.sql
├── src/main/java/
│   └── com/example/api/corejavaproject/
│       ├── db/                 # Database config
│       ├── model/              # Product model
│       ├── repository/         # JDBC repository
│       ├── server/             # HTTP server
│       └── service/            # Business logic
├── docker-compose.yml           # Oracle 21c XE Docker
├── build.bat                   # Build script (no Maven)
├── pom.xml                     # Maven config (optional)
└── MANIFEST.MF                 # JAR manifest
```

## Docker Commands

```powershell
# Start Oracle
docker-compose up -d

# Stop Oracle (data persists)
docker-compose down

# Stop and remove data
docker-compose down -v

# View logs
docker logs -f oracle19c

# Connect to SQL*Plus
docker exec -it oracle19c sqlplus system/oracle19c_password@//localhost:1521/ORCLPDB1
```

## Troubleshooting

### Oracle container won't start
```powershell
# Check Docker resources
docker system df

# View Oracle logs
docker logs oracle19c
```

### Connection refused
- Oracle takes 5-10 minutes on first start
- Check if port 1521 is available: `netstat -an | findstr 1521`

### JDBC Driver not found
- Ensure `lib/ojdbc8.jar` exists
- For manual build: `.\build.bat compile`
- For Maven: `mvn dependency:copy-dependencies`

## Available Oracle XE Docker Images

| Tag | Description |
|-----|-------------|
| 21-slim | Oracle 21c XE (recommended) |
| 21-full | Oracle 21c XE with full features |
| 18-slim | Oracle 18c XE |
| 18-full | Oracle 18c XE with full features |