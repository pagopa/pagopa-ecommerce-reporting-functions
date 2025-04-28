package it.pagopa.ecommerce.reporting.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SetEnvironmentVariable(key = "ECOMMERCE_CLIENTS_LIST", value = "[\"CLIENT_1\",\"CLIENT2\"]")
@SetEnvironmentVariable(key = "ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST", value = "[\"PAY_1\",\"PAY_2\"]")
@SetEnvironmentVariable(
        key = "ECOMMERCE_PAYMENT_METHODS_PSP_LIST", value = "{\"PAY_1\":[\"PSP_1\",\"PSP_2\"],\"PAY_2\":[\"PSP_3\",\"PSP_2\"]}"
)
@ExtendWith(MockitoExtension.class)
public class ReadDataServiceTest {

    @Mock
    private Logger mockLogger;

    @Mock
    private EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    @Mock
    private WriteDataService writeDataService;

    @Captor
    private ArgumentCaptor<String> clientIdCaptor;

    @Captor
    private ArgumentCaptor<String> paymentTypeCodeCaptor;

    @Captor
    private ArgumentCaptor<String> pspIdCaptor;

    @Captor
    private ArgumentCaptor<OffsetDateTime> startDate;

    @Captor
    private ArgumentCaptor<OffsetDateTime> endDate;

    private MockedStatic<EcommerceHelpdeskServiceClient> ecommerceHelpdeskServiceClientMockedStatic;
    private MockedStatic<WriteDataService> writeDataServiceMockedStatic;

    @BeforeEach
    public void setUp() {
        ecommerceHelpdeskServiceClientMockedStatic = mockStatic(EcommerceHelpdeskServiceClient.class);
        writeDataServiceMockedStatic = mockStatic(WriteDataService.class);
        when(EcommerceHelpdeskServiceClient.getInstance(any(Logger.class))).thenReturn(ecommerceHelpdeskServiceClient);
        when(WriteDataService.getInstance()).thenReturn(writeDataService);
    }

    @AfterEach
    public void tearDown() {
        ecommerceHelpdeskServiceClientMockedStatic.close();
        writeDataServiceMockedStatic.close();
    }

    @Test
    public void readTest() {
        ReadDataService readDataService = ReadDataService.getInstance(mockLogger);
        JsonNode result = new TextNode("mockedValue");
        doReturn(result).when(
                ecommerceHelpdeskServiceClient
        )
                .fetchTransactionMetrics(
                        clientIdCaptor.capture(),
                        pspIdCaptor.capture(),
                        paymentTypeCodeCaptor.capture(),
                        startDate.capture(),
                        endDate.capture()
                );
        readDataService.readAndWriteData("IO");
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<String> clientIdValues = clientIdCaptor.getAllValues();
        List<String> pspIdValues = pspIdCaptor.getAllValues();
        List<String> paymentTypeCodeValues = paymentTypeCodeCaptor.getAllValues();
        List<OffsetDateTime> startDateValues = startDate.getAllValues();
        List<OffsetDateTime> endDateValues = endDate.getAllValues();
        for (int i = 0; i < clientIdValues.size(); i++) {
            verify(ecommerceHelpdeskServiceClient, times(1)).fetchTransactionMetrics(
                    clientIdValues.get(i),
                    pspIdValues.get(i),
                    paymentTypeCodeValues.get(i),
                    startDateValues.get(i),
                    endDateValues.get(i)
            );
            verify(writeDataService, times(1))
                    .writeStateMetricsInTableStorage(
                            result,
                            mockLogger,
                            clientIdValues.get(i),
                            paymentTypeCodeValues.get(i),
                            pspIdValues.get(i)
                    );
        }

    }

}
