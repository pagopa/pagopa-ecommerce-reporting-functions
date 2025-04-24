
package it.pagopa.ecommerce.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Spy;

import java.time.OffsetDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class EcommerceHelpdeskServiceClientTest {

    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
    }

    @Mock
    ExecutionContext context;

    @Spy
    EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @Test
    public void instanceTest() {
        assertNotNull(EcommerceHelpdeskServiceClient.getInstance(mockLogger));
    }

    @Test
    public void fetchTransactionMetricsTest() {
        ecommerceHelpdeskServiceClient = new EcommerceHelpdeskServiceClient(mockLogger);
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

}
