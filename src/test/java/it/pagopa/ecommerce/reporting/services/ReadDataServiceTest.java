package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(SystemStubsExtension.class)
@ExtendWith(MockitoExtension.class)
public class ReadDataServiceTest {

    @SystemStub
    private EnvironmentVariables variables = new EnvironmentVariables(
            "ECOMMERCE_CLIENTS_LIST",
            "[\"CLIENT_1\",\"CLIENT2\"]",
            "ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST",
            "[\"PAY_1\",\"PAY_2\"]",
            "ECOMMERCE_PAYMENT_METHODS_PSP_LIST",
            "{\"PAY_1\":[\"PSP_1\",\"PSP_2\"],\"PAY_2\":[\"PSP_3\",\"PSP_2\"]}"
    );

    private Logger mockLogger;

    @Mock
    EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @BeforeEach
    void setUp() {
        mockLogger = mock(Logger.class);
        // service = new TransactionStatusAggregationService(mockTableClient);
    }

    @Test
    public void instanceTest() {
        assertNotNull(ReadDataService.getInstance(mockLogger));
    }
    /*
     * @Test public void readTest() { ReadDataService readDataService =
     * ReadDataService.getInstance(mockLogger); JsonNode result = new
     * TextNode("mockedValue");
     * when(readDataService.getEcommerceHelpdeskServiceClient(mockLogger))
     * .thenReturn(ecommerceHelpdeskServiceClient); when(
     * ecommerceHelpdeskServiceClient .fetchTransactionMetrics(any(), any(), any(),
     * any(), any()) ).thenReturn(result); readDataService.readAndWriteData("IO");
     * verify(ecommerceHelpdeskServiceClient, times(8))
     * .fetchTransactionMetrics(any(), any(), any(), any(), any()); assertEquals(8,
     * resultList.size()); }
     */
}
