
package it.pagopa.ecommerce.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EcommerceHelpdeskServiceClientTest {

    @Mock
    Logger mockLogger;

    @Mock
    ExecutionContext context;

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
