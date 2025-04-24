package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SetEnvironmentVariable(key = "ECOMMERCE_CLIENTS_LIST", value = "[\"CLIENT_1\",\"CLIENT2\"]")
@SetEnvironmentVariable(key = "ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST", value = "[\"PAY_1\",\"PAY_2\"]")
@SetEnvironmentVariable(
        key = "ECOMMERCE_PAYMENT_METHODS_PSP_LIST", value = "{\"PAY_1\":[\"PSP_1\",\"PSP_2\"],\"PAY_2\":[\"PSP_3\",\"PSP_2\"]}"
)
public class ReadDataServiceTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @Mock
    private WriteDataService writeDataService;

    @Mock
    private ReadDataService readDataService;

    @Test
    public void instanceTest() {
        assertNotNull(ReadDataService.getInstance(mockLogger));
    }
    /*
     * @Test public void readTest() { // readDataService =
     * ReadDataService.getInstance(mockLogger); JsonNode result = new
     * TextNode("mockedValue");
     * doReturn(writeDataService).when(readDataService).getWriteDataService();
     * doCallRealMethod().when(readDataService).readAndWriteData(any());
     * doReturn(ecommerceHelpdeskServiceClient).when(readDataService).
     * getEcommerceHelpdeskServiceClient(mockLogger);
     * when(ecommerceHelpdeskServiceClient.fetchTransactionMetrics(any(), any(),
     * any(), any(), any())) .thenReturn(result);
     * doNothing().when(writeDataService).writeStateMetricsInTableStorage(any(),
     * any(), any(), any(), any()); readDataService.readAndWriteData("IO"); try {
     * Thread.sleep(5000); } catch (InterruptedException e) { throw new
     * RuntimeException(e); } verify(ecommerceHelpdeskServiceClient,
     * times(8)).fetchTransactionMetrics(any(), any(), any(), any(), any()); //
     * assertEquals(8, resultList.size()); }
     */
}
