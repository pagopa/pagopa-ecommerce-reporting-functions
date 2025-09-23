
package it.pagopa.ecommerce.reporting.clients;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_KEY", value = "API_KEY")
@SetEnvironmentVariable(key = "HELPDESK_SERVICE_URI", value = "http://localhost")
@SetEnvironmentVariable(key = "HELPDESK_SERVICE_API_ENDPOINT", value = "/mock")
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

    @BeforeEach
    public void setUp() throws Exception {
        // Reset the singleton before each test
        Field instance = EcommerceHelpdeskServiceClient.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
    }

    private static void setStaticFinalField(
                                            Field field,
                                            Object newValue
    ) throws Exception {
        field.setAccessible(true);

        // Remove final modifier
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~java.lang.reflect.Modifier.FINAL);

        // Set the new value
        field.set(null, newValue);
    }

    @Test
    public void testNodeFetch() throws Exception {
        mockStatic = mockStatic(HttpClients.class);
        setStaticFinalField(EcommerceHelpdeskServiceClient.class.getDeclaredField("API_KEY"), "API_KEY");

        CloseableHttpClient workingHttpClient = mock(CloseableHttpClient.class);
        CloseableHttpResponse workingResponse = mock(CloseableHttpResponse.class);

        StringEntity entity = new StringEntity("{\"field\":\"1\"}", StandardCharsets.UTF_8);
        entity.setContentType("application/json");
        when(workingResponse.getEntity()).thenReturn(entity);

        when(workingHttpClient.execute(any(HttpPost.class))).thenReturn(workingResponse);

        when(HttpClients.createDefault()).thenReturn(workingHttpClient);

        ecommerceHelpdeskServiceClient = EcommerceHelpdeskServiceClient.getInstance(mockLogger);

        try {
            JsonNode node = ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
                    "clientId",
                    "pspId",
                    "paymentTypeCode",
                    OffsetDateTime.now(),
                    OffsetDateTime.now().minusHours(1)
            );

            // Print the node
            System.out.println("Node: " + node);
            System.out.println("Node is empty: " + node.isEmpty());
            assertNotNull(node);
            assertFalse(node.isEmpty());
            // Add this right after the fetchTransactionMetrics call
            verify(workingHttpClient).execute(any(HttpPost.class));

        } catch (Exception e) {
            System.out.println("Exception during fetchTransactionMetrics: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            mockStatic.close();
        }
    }

}
