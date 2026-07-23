# SOAP API Implementation Plan

## Goal
Create a simple SOAP API endpoint to learn how SOAP works in pure Java SE (no Spring/Quarkus).

## Approach
- Use **JAX-WS** (`javax.xml.ws`) - built-in Java SOAP API
- Use **JAXB** for XML serialization (added as dependency - removed from JDK since Java 11)
- Use `Endpoint.publish()` to expose SOAP service on a separate port
- Leverage existing `ProductService` for business logic

## Files to Create

### 1. `src/main/java/com/example/api/corejavaproject/soap/ProductSoapEndpoint.java`
- `@WebService` annotated class
- `@WebMethod` for operations: `getProduct(int id)` and `getAllProducts()`
- Uses existing `ProductService` internally
- Manual JAXB annotations for XML binding

### 2. `src/main/java/com/example/api/corejavaproject/soap/SoapServer.java`
- Simple launcher class that publishes the SOAP endpoint
- Uses `javax.xml.ws.Endpoint.publish(address, implementor)`
- Runs on port 8081 (separate from REST API on 8080)

## Files to Modify

### 1. `pom.xml`
Add JAXB dependencies (removed from JDK since Java 11):
```xml
<dependency>
    <groupId>javax.xml.bind</groupId>
    <artifactId>jaxb-api</artifactId>
    <version>2.3.1</version>
</dependency>
<dependency>
    <groupId>com.sun.xml.bind</groupId>
    <artifactId>jaxb-impl</artifactId>
    <version>2.3.1</version>
</dependency>
```

### 2. `Main.java` (optional)
- Could integrate SOAP server startup, but keep it separate for clarity

## SOAP Operations
| Operation | Input | Output |
|-----------|-------|--------|
| getProduct | id: int | Product (XML) |
| getAllProducts | none | List<Product> (XML) |

## Testing
- SOAP request to `http://localhost:8081/ws/products`
- WSDL available at `http://localhost:8081/ws/products?wsdl`

## Example SOAP Request (POST)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:prod="http://product.example.api/">
   <soapenv:Header/>
   <soapenv:Body>
      <prod:getProduct>
         <id>1</id>
      </prod:getProduct>
   </soapenv:Body>
</soapenv:Envelope>
```

## Implementation Order
1. Add JAXB dependencies to pom.xml
2. Create ProductSoapEndpoint.java with JAX-WS annotations
3. Create SoapServer.java to publish the endpoint
4. Test with SOAP UI or curl