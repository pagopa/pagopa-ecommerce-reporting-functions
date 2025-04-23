package it.pagopa.ecommerce.reporting.client;

import com.fasterxml.jackson.databind.JsonNode;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.logging.LoggerFactory;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.time.OffsetDateTime;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
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

    @Test
    public void instanceTest() {
        assertNotNull(EcommerceHelpdeskServiceClient.getInstance());
    }

    /*
     * @Test public void fetchTransactionMetricsTest() { JsonNode node =
     * ecommerceHelpdeskServiceClient.fetchTransactionMetrics( "clientId", "pspId",
     * "paymentTypeCode", OffsetDateTime.now(), OffsetDateTime.now().minusHours(1),
     * mockLOG ); assertNotNull(node); }
     */

}
