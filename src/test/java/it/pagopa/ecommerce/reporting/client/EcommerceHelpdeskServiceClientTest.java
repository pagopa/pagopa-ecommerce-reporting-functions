package it.pagopa.ecommerce.reporting.client;

import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Spy;

import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
public class EcommerceHelpdeskServiceClientTest {

    @SystemStub
    private EnvironmentVariables variables = new EnvironmentVariables(
            "API_HOST",
            "apiHost",
            "HELPDESK_SERVICE_API_KEY",
            "apiKey",
            "HELPDESK_SERVICE_API_ENDPOINT",
            "/apiEndpoint"
    );

    private Logger mockLogger;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        // service = new TransactionStatusAggregationService(mockTableClient);
    }

    @Mock
    ExecutionContext context;

    @Spy
    EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @Test
    public void instanceTest() {
        assertNotNull(EcommerceHelpdeskServiceClient.getInstance(mockLogger));
    }

    /*
     * @Test public void fetchTransactionMetricsTest() { JsonNode node =
     * ecommerceHelpdeskServiceClient.fetchTransactionMetrics( "clientId", "pspId",
     * "paymentTypeCode", OffsetDateTime.now(), OffsetDateTime.now().minusHours(1),
     * mockLOG ); assertNotNull(node); }
     */

}
