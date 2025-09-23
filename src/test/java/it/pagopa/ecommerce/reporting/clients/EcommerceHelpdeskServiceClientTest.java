
package it.pagopa.ecommerce.reporting.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
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
    ExecutionContext context;

    @Mock
    private CloseableHttpClient httpClientMock;

    @Mock
    private CloseableHttpResponse httpResponseMock;

    @Mock
    private StatusLine statusLineMock;

    private MockedStatic<HttpClients> mockStatic;

    EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

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
        mockStatic.close();
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
        mockStatic.close();
    }

}
