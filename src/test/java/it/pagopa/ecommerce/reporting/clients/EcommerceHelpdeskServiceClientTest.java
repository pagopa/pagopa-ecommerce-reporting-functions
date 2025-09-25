package it.pagopa.ecommerce.reporting.clients;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "API_KEY")
@SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "API_KEY")
@SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "API_KEY")
@ExtendWith(MockitoExtension.class)
public class EcommerceHelpdeskServiceClientTest {

    @Mock
    Logger mockLogger;

    @Mock
    private CloseableHttpClient httpClientMock;

    @Mock
    private CloseableHttpResponse httpResponseMock;

    @Mock
    private StatusLine statusLineMock;

    private MockedStatic<HttpClients> mockStatic;

    EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @BeforeEach
    void setUp() {
        resetSingleton();
        reset(mockLogger, httpClientMock, httpResponseMock, statusLineMock);
    }

    @AfterEach
    void tearDown() {
        // Needed because of the singleton
        if (mockStatic != null) {
            try {
                mockStatic.close();
            } finally {
                mockStatic = null;
            }
        }

        resetSingleton();
    }

    /**
     * Reset the singleton instance using reflection to ensure test isolation
     */
    private void resetSingleton() {
        try {
            Field instanceField = EcommerceHelpdeskServiceClient.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, null);
        } catch (Exception e) {
            System.err.println("Failed to reset singleton: " + e.getMessage());
        }
    }

    @Test
    public void instanceTest() {
        assertNotNull(EcommerceHelpdeskServiceClient.getInstance(mockLogger));
    }

    @Test
    public void fetchTransactionMetricsTestNoClientHttp() {
        ecommerceHelpdeskServiceClient = EcommerceHelpdeskServiceClient.getInstance(mockLogger);
        JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                "clientId",
                "pspId",
                "paymentTypeCode",
                OffsetDateTime.now(),
                OffsetDateTime.now().minusHours(1)
        );
        assertNotNull(node);
        assertTrue(node.isEmpty());
    }

    @Test
    public void fetchTransactionMetricsTestNoValidData() {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        ecommerceHelpdeskServiceClient = EcommerceHelpdeskServiceClient.getInstance(mockLogger);
        JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                null,
                "pspId",
                "paymentTypeCode",
                OffsetDateTime.now(),
                OffsetDateTime.now().minusHours(1)
        );
        assertNotNull(node);
        assertTrue(node.isEmpty());
    }

    @Test
    public void testNodeFetch() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);

        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(
                httpResponseMock
        );
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("{\"field\":\"1\"}", StandardCharsets.UTF_8));

        ecommerceHelpdeskServiceClient = EcommerceHelpdeskServiceClient.getInstance(mockLogger);
        JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                "clientId",
                "pspId",
                "paymentTypeCode",
                OffsetDateTime.now(),
                OffsetDateTime.now().minusHours(1)
        );
        assertNotNull(node);
        assertFalse(node.isEmpty());
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testInvalidParametersReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testHttpPostLinesCovered() throws Exception {
        // Mock static HttpClients
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);

        // Mock HTTP response
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("{\"status\":\"ok\"}", StandardCharsets.UTF_8));

        ecommerceHelpdeskServiceClient = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                "clientId",
                "pspId",
                "paymentTypeCode",
                OffsetDateTime.now(),
                OffsetDateTime.now().minusHours(1)
        );

        // Verify response
        assertNotNull(node);
        assertEquals("ok", node.get("status").asText());

        // Capture the HttpPost to verify URI and header
        ArgumentCaptor<HttpPost> httpPostCaptor = ArgumentCaptor.forClass(HttpPost.class);
        verify(httpClientMock).execute(httpPostCaptor.capture());
        HttpPost capturedPost = httpPostCaptor.getValue();

        assertEquals("http://localhost:8080/transactions", capturedPost.getURI().toString());
        assertEquals("test-key", capturedPost.getFirstHeader("ocp-apim-subscription-key").getValue());
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "")
    void testMissingApiKeyReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    void testNullApiKeyReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testEmptyPspIdReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testEmptyPaymentTypeCodeReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testNullStartDateReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                null,
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testNullEndDateReturnEmptyJson() {
        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                null
        );

        assertEquals("{}", result.toString());
        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testHttpExecutionExceptionReturnsEmptyJson() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenThrow(new IOException("Connection failed"));

        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger).log(eq(Level.SEVERE), eq("Failed to fetch transaction details"), any(IOException.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testJsonParsingExceptionReturnsEmptyJson() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("invalid-json", StandardCharsets.UTF_8));

        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertEquals("{}", result.toString());
        verify(mockLogger).log(eq(Level.SEVERE), eq("Failed to fetch transaction details"), any(Exception.class));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testResponseStatusLineLogging() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("{\"status\":\"ok\"}", StandardCharsets.UTF_8));

        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        JsonNode result = client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        assertNotNull(result);
        assertEquals("ok", result.get("status").asText());

        verify(mockLogger, atLeastOnce()).warning(any(Supplier.class));
    }

    @Test
    void testSingletonPattern() {
        EcommerceHelpdeskServiceClient instance1 = EcommerceHelpdeskServiceClient.getInstance(mockLogger);
        EcommerceHelpdeskServiceClient instance2 = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testJsonPayloadFormatting() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity())
                .thenReturn(new StringEntity("{\"result\":\"success\"}", StandardCharsets.UTF_8));

        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        OffsetDateTime startDate = OffsetDateTime.parse("2023-01-01T00:00:00Z");
        OffsetDateTime endDate = OffsetDateTime.parse("2023-01-02T00:00:00Z");

        JsonNode result = client.fetchTransactionMetrics(
                "testClient",
                "testPsp",
                "CARD",
                startDate,
                endDate
        );

        assertNotNull(result);

        ArgumentCaptor<String> logCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockLogger, atLeastOnce()).info(logCaptor.capture());

        String loggedPayload = logCaptor.getValue();
        assertTrue(loggedPayload.contains("testClient"));
        assertTrue(loggedPayload.contains("testPsp"));
        assertTrue(loggedPayload.contains("CARD"));
    }

    @Test
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost:8080")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/transactions")
    @SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "test-key")
    void testHttpClientResourceCleanup() throws IOException {
        mockStatic = mockStatic(HttpClients.class);
        when(HttpClients.createDefault()).thenReturn(httpClientMock);
        when(httpClientMock.execute(any(HttpPost.class))).thenReturn(httpResponseMock);
        when(httpResponseMock.getEntity()).thenReturn(new StringEntity("{\"data\":\"test\"}", StandardCharsets.UTF_8));

        EcommerceHelpdeskServiceClient client = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        client.fetchTransactionMetrics(
                "clientId",
                "psp123",
                "PTC",
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );

        verify(httpClientMock).close();
        verify(httpResponseMock).close();
    }
}
