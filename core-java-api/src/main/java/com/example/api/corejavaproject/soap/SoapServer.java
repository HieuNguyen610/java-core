package com.example.api.corejavaproject.soap;

import com.example.api.corejavaproject.model.Product;
import com.example.api.corejavaproject.service.ProductService;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Simple SOAP Server using pure Java SE.
 *
 * Demonstrates how SOAP works under the hood:
 * - Parses SOAP XML envelope manually using DOM/XPath
 * - Extracts method name and parameters from SOAP Body
 * - Invokes the appropriate service method
 * - Wraps response in SOAP envelope
 *
 * This is the manual approach - in production you might use JAX-WS which
 * handles all this automatically.
 */
public class SoapServer {

    private static final int DEFAULT_PORT = 8081;
    private final HttpServer server;
    private final ProductService productService;
    private final DocumentBuilder documentBuilder;

    public SoapServer(int port) throws Exception {
        this.productService = new ProductService();

        // Create XML parser (built into Java SE)
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        this.documentBuilder = factory.newDocumentBuilder();

        // Create HTTP server
        this.server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/ws/products", new SoapHandler());
    }

    public void start() {
        server.start();
        int port = server.getAddress().getPort();
        System.out.println("=".repeat(60));
        System.out.println("  SOAP API Server - Pure Java SE Implementation");
        System.out.println("=".repeat(60));
        System.out.println("🚀 SOAP Server started on http://localhost:" + port);
        System.out.println("📍 SOAP Endpoint: http://localhost:" + port + "/ws/products");
        System.out.println("📄 WSDL: http://localhost:" + port + "/ws/products?wsdl");
        System.out.println();
        System.out.println("Available operations:");
        System.out.println("   getProduct(id)         - Get product by ID");
        System.out.println("   getAllProducts()       - List all products");
        System.out.println("   createProduct(name, price, description) - Create product");
        System.out.println("   deleteProduct(id)      - Delete product");
    }

    public void stop() {
        server.stop(0);
    }

    /**
     * SOAP Handler - processes incoming SOAP requests
     */
    private class SoapHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            String query = exchange.getRequestURI().getQuery();

            // Handle GET with ?wsdl - return WSDL
            if ("GET".equals(method) && "wsdl".equals(query)) {
                handleWSDL(exchange);
                return;
            }

            // Handle POST - SOAP request
            if ("POST".equals(method) && path.endsWith("/ws/products")) {
                handleSOAPRequest(exchange);
                return;
            }

            // Method not allowed
            sendResponse(exchange, 405, "Method not allowed");
        }
    }

    /**
     * Handle incoming SOAP request
     */
    private void handleSOAPRequest(HttpExchange exchange) throws IOException {
        try {
            // Read request body
            String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("\n📥 SOAP Request received:");
            System.out.println(requestBody);

            // Parse SOAP XML
            Document requestDoc = documentBuilder.parse(new ByteArrayInputStream(requestBody.getBytes(StandardCharsets.UTF_8)));

            // Extract method name and parameters using XPath
            XPath xpath = XPathFactory.newInstance().newXPath();

            // Find the first child element inside SOAP Body (the operation)
            String expression = "//*[local-name()='Body']/*[1]/*[local-name()='GetProduct' or local-name()='GetAllProducts' or local-name()='CreateProduct' or local-name()='DeleteProduct']";
            NodeList operations = (NodeList) xpath.evaluate(expression, requestDoc, XPathConstants.NODESET);

            if (operations.getLength() == 0) {
                sendSOAPFault(exchange, "Client", "Could not understand the request");
                return;
            }

            Element operation = (Element) operations.item(0);
            String operationName = operation.getLocalName();
            System.out.println("🔧 Operation: " + operationName);

            // Invoke the appropriate operation
            String soapResponse = switch (operationName) {
                case "GetProduct" -> {
                    int id = Integer.parseInt(getElementText(operation, "Id"));
                    Optional<Product> product = productService.getProduct(id);
                    String productXml = product.isPresent() ?
                        "<Product>" +
                            "<Id>" + product.get().getId() + "</Id>" +
                            "<Name>" + escapeXml(product.get().getName()) + "</Name>" +
                            "<Price>" + product.get().getPrice() + "</Price>" +
                            "<Description>" + escapeXml(product.get().getDescription()) + "</Description>" +
                        "</Product>" :
                        "<Product xsi:nil=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
                    yield createSOAPResponse("GetProductResponse", productXml);
                }
                case "GetAllProducts" -> {
                    var products = productService.getAllProducts();
                    StringBuilder sb = new StringBuilder("<Products>");
                    for (Product p : products) {
                        sb.append("<Product>")
                          .append("<Id>").append(p.getId()).append("</Id>")
                          .append("<Name>").append(escapeXml(p.getName())).append("</Name>")
                          .append("<Price>").append(p.getPrice()).append("</Price>")
                          .append("<Description>").append(escapeXml(p.getDescription())).append("</Description>")
                          .append("</Product>");
                    }
                    sb.append("</Products>");
                    yield createSOAPResponse("GetAllProductsResponse", sb.toString());
                }
                case "CreateProduct" -> {
                    String name = getElementText(operation, "Name");
                    double price = Double.parseDouble(getElementText(operation, "Price"));
                    String description = getElementText(operation, "Description");
                    Product created = productService.createProduct(new Product(0, name, price, description));
                    String productXml = "<Product>" +
                        "<Id>" + created.getId() + "</Id>" +
                        "<Name>" + escapeXml(created.getName()) + "</Name>" +
                        "<Price>" + created.getPrice() + "</Price>" +
                        "<Description>" + escapeXml(created.getDescription()) + "</Description>" +
                        "</Product>";
                    yield createSOAPResponse("CreateProductResponse", productXml);
                }
                case "DeleteProduct" -> {
                    int id = Integer.parseInt(getElementText(operation, "Id"));
                    boolean deleted = productService.deleteProduct(id);
                    yield createSOAPResponse("DeleteProductResponse", "<Success>" + deleted + "</Success>");
                }
                default -> {
                    sendSOAPFault(exchange, "Client", "Unknown operation: " + operationName);
                    yield null;
                }
            };

            if (soapResponse != null) {
                System.out.println("📤 SOAP Response:");
                System.out.println(soapResponse);
                sendResponse(exchange, 200, soapResponse);
            }

        } catch (Exception e) {
            System.err.println("❌ Error processing SOAP request: " + e.getMessage());
            e.printStackTrace();
            sendSOAPFault(exchange, "Server", "Internal error: " + e.getMessage());
        }
    }

    /**
     * Create a SOAP envelope with the given body content
     */
    private String createSOAPResponse(String operationName, String bodyContent) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n");
        sb.append("               xmlns:prod=\"http://product.example.api/\"\n");
        sb.append("               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n");
        sb.append("               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n");
        sb.append("   <soap:Header/>\n");
        sb.append("   <soap:Body>\n");
        sb.append("      <prod:").append(operationName).append(">\n");
        sb.append(bodyContent);
        sb.append("\n      </prod:").append(operationName).append(">\n");
        sb.append("   </soap:Body>\n");
        sb.append("</soap:Envelope>");
        return sb.toString();
    }

    /**
     * Send a SOAP fault response
     */
    private void sendSOAPFault(HttpExchange exchange, String faultCode, String faultString) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n");
        sb.append("   <soap:Body>\n");
        sb.append("      <soap:Fault>\n");
        sb.append("         <faultcode>soap:").append(faultCode).append("</faultcode>\n");
        sb.append("         <faultstring>").append(escapeXml(faultString)).append("</faultstring>\n");
        sb.append("      </soap:Fault>\n");
        sb.append("   </soap:Body>\n");
        sb.append("</soap:Envelope>");

        String faultResponse = sb.toString();
        exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
        byte[] bytes = faultResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(500, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    /**
     * Handle WSDL request - returns a simple WSDL document
     */
    private void handleWSDL(HttpExchange exchange) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\"\n");
        sb.append("                  xmlns:soap=\"http://schemas.xmlsoap.org/wsdl/soap/\"\n");
        sb.append("                  xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\"\n");
        sb.append("                  xmlns:prod=\"http://product.example.api/\"\n");
        sb.append("                  targetNamespace=\"http://product.example.api/\"\n");
        sb.append("                  name=\"ProductService\">\n");
        sb.append("\n");
        sb.append("    <wsdl:types>\n");
        sb.append("        <xsd:schema targetNamespace=\"http://product.example.api/\">\n");
        sb.append("            <xsd:element name=\"GetProduct\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Id\" type=\"xsd:int\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"GetProductResponse\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Product\" type=\"prod:Product\" minOccurs=\"0\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:complexType name=\"Product\">\n");
        sb.append("                <xsd:sequence>\n");
        sb.append("                    <xsd:element name=\"Id\" type=\"xsd:int\"/>\n");
        sb.append("                    <xsd:element name=\"Name\" type=\"xsd:string\"/>\n");
        sb.append("                    <xsd:element name=\"Price\" type=\"xsd:double\"/>\n");
        sb.append("                    <xsd:element name=\"Description\" type=\"xsd:string\"/>\n");
        sb.append("                </xsd:sequence>\n");
        sb.append("            </xsd:complexType>\n");
        sb.append("            <xsd:element name=\"GetAllProducts\">\n");
        sb.append("                <xsd:complexType/>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"GetAllProductsResponse\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Product\" type=\"prod:Product\" maxOccurs=\"unbounded\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"CreateProduct\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Name\" type=\"xsd:string\"/>\n");
        sb.append("                        <xsd:element name=\"Price\" type=\"xsd:double\"/>\n");
        sb.append("                        <xsd:element name=\"Description\" type=\"xsd:string\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"CreateProductResponse\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Product\" type=\"prod:Product\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"DeleteProduct\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Id\" type=\"xsd:int\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("            <xsd:element name=\"DeleteProductResponse\">\n");
        sb.append("                <xsd:complexType>\n");
        sb.append("                    <xsd:sequence>\n");
        sb.append("                        <xsd:element name=\"Success\" type=\"xsd:boolean\"/>\n");
        sb.append("                    </xsd:sequence>\n");
        sb.append("                </xsd:complexType>\n");
        sb.append("            </xsd:element>\n");
        sb.append("        </xsd:schema>\n");
        sb.append("    </wsdl:types>\n");
        sb.append("\n");
        sb.append("    <wsdl:message name=\"GetProductInput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:GetProduct\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"GetProductOutput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:GetProductResponse\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"GetAllProductsInput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:GetAllProducts\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"GetAllProductsOutput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:GetAllProductsResponse\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"CreateProductInput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:CreateProduct\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"CreateProductOutput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:CreateProductResponse\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"DeleteProductInput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:DeleteProduct\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("    <wsdl:message name=\"DeleteProductOutput\">\n");
        sb.append("        <wsdl:part name=\"parameters\" element=\"prod:DeleteProductResponse\"/>\n");
        sb.append("    </wsdl:message>\n");
        sb.append("\n");
        sb.append("    <wsdl:portType name=\"ProductServicePortType\">\n");
        sb.append("        <wsdl:operation name=\"GetProduct\">\n");
        sb.append("            <wsdl:input message=\"prod:GetProductInput\"/>\n");
        sb.append("            <wsdl:output message=\"prod:GetProductOutput\"/>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"GetAllProducts\">\n");
        sb.append("            <wsdl:input message=\"prod:GetAllProductsInput\"/>\n");
        sb.append("            <wsdl:output message=\"prod:GetAllProductsOutput\"/>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"CreateProduct\">\n");
        sb.append("            <wsdl:input message=\"prod:CreateProductInput\"/>\n");
        sb.append("            <wsdl:output message=\"prod:CreateProductOutput\"/>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"DeleteProduct\">\n");
        sb.append("            <wsdl:input message=\"prod:DeleteProductInput\"/>\n");
        sb.append("            <wsdl:output message=\"prod:DeleteProductOutput\"/>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("    </wsdl:portType>\n");
        sb.append("\n");
        sb.append("    <wsdl:binding name=\"ProductServiceSoapBinding\" type=\"prod:ProductServicePortType\">\n");
        sb.append("        <soap:binding style=\"document\" transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n");
        sb.append("        <wsdl:operation name=\"GetProduct\">\n");
        sb.append("            <soap:operation soapAction=\"GetProduct\"/>\n");
        sb.append("            <wsdl:input><soap:body use=\"literal\"/></wsdl:input>\n");
        sb.append("            <wsdl:output><soap:body use=\"literal\"/></wsdl:output>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"GetAllProducts\">\n");
        sb.append("            <soap:operation soapAction=\"GetAllProducts\"/>\n");
        sb.append("            <wsdl:input><soap:body use=\"literal\"/></wsdl:input>\n");
        sb.append("            <wsdl:output><soap:body use=\"literal\"/></wsdl:output>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"CreateProduct\">\n");
        sb.append("            <soap:operation soapAction=\"CreateProduct\"/>\n");
        sb.append("            <wsdl:input><soap:body use=\"literal\"/></wsdl:input>\n");
        sb.append("            <wsdl:output><soap:body use=\"literal\"/></wsdl:output>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("        <wsdl:operation name=\"DeleteProduct\">\n");
        sb.append("            <soap:operation soapAction=\"DeleteProduct\"/>\n");
        sb.append("            <wsdl:input><soap:body use=\"literal\"/></wsdl:input>\n");
        sb.append("            <wsdl:output><soap:body use=\"literal\"/></wsdl:output>\n");
        sb.append("        </wsdl:operation>\n");
        sb.append("    </wsdl:binding>\n");
        sb.append("\n");
        sb.append("    <wsdl:service name=\"ProductService\">\n");
        sb.append("        <wsdl:port name=\"ProductPort\" binding=\"prod:ProductServiceSoapBinding\">\n");
        sb.append("            <soap:address location=\"http://localhost:8081/ws/products\"/>\n");
        sb.append("        </wsdl:port>\n");
        sb.append("    </wsdl:service>\n");
        sb.append("</wsdl:definitions>");

        sendResponse(exchange, 200, sb.toString());
    }

    // Utility methods

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "text/xml; charset=utf-8");
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String getElementText(Element parent, String childName) {
        NodeList children = parent.getElementsByTagName(childName);
        if (children.getLength() > 0) {
            return children.item(0).getTextContent();
        }
        return "";
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    // Main entry point
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        try {
            SoapServer soapServer = new SoapServer(port);
            soapServer.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n🛑 Shutting down SOAP server...");
                soapServer.stop();
            }));

            Thread.currentThread().join();

        } catch (Exception e) {
            System.err.println("❌ Failed to start SOAP server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}