package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.TableEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

class WriteDataServiceTest {

    private TableClient mockTableClient;
    private Logger mockLogger;
    private WriteDataService writeDataService;

    @BeforeEach
    void setUp() {
        mockTableClient = mock(TableClient.class);
        mockLogger = mock(Logger.class);
        writeDataService = new WriteDataService(mockTableClient);
    }

    @Test
    void testWriteStateMetricsInTableStorage_shouldInsertEntity() throws Exception {
        // Given
        String jsonInput = """
                {
                    "clientId": "CHECKOUT",
                    "pspId": "pspX",
                    "paymentTypeCode": "PT1",
                    "ACTIVATED": 12,
                    "CLOSED": 45,
                    "NOTIFIED_OK": 20,
                    "EXPIRED": 0,
                    "REFUNDED": 0,
                    "CANCELED": 0,
                    "EXPIRED_NOT_AUTHORIZED": 0,
                    "UNAUTHORIZED": 0,
                    "REFUND_ERROR": 0,
                    "REFUND_REQUESTED": 0,
                    "CANCELLATION_REQUESTED": 0,
                    "CANCELLATION_EXPIRED": 0,
                    "AUTHORIZATION_REQUESTED": 0,
                    "AUTHORIZATION_COMPLETED": 0,
                    "CLOSURE_REQUESTED": 0,
                    "CLOSURE_ERROR": 0,
                    "NOTIFIED_KO": 0,
                    "NOTIFICATION_ERROR": 0,
                    "NOTIFICATION_REQUESTED": 0
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonInput);

        // When
        writeDataService.writeStateMetricsInTableStorage(jsonNode, mockLogger);

        // Then
        verify(mockTableClient, times(1)).createEntity(any(TableEntity.class));
        verify(mockLogger).info(contains("Successfully inserted state metrics"));
    }

    @Test
    void testWriteStateMetricsInTableStorage_shouldLogErrorOnFailure() throws Exception {
        // Given
        String jsonInput = """
                {
                    "clientId": "IO",
                    "pspId": "pspY",
                    "paymentTypeCode": "PT2",
                    "ACTIVATED": 12,
                    "CLOSED": 45,
                    "NOTIFIED_OK": 20,
                    "EXPIRED": 0,
                    "REFUNDED": 0,
                    "CANCELED": 0,
                    "EXPIRED_NOT_AUTHORIZED": 0,
                    "UNAUTHORIZED": 0,
                    "REFUND_ERROR": 0,
                    "REFUND_REQUESTED": 0,
                    "CANCELLATION_REQUESTED": 0,
                    "CANCELLATION_EXPIRED": 0,
                    "AUTHORIZATION_REQUESTED": 0,
                    "AUTHORIZATION_COMPLETED": 0,
                    "CLOSURE_REQUESTED": 0,
                    "CLOSURE_ERROR": 0,
                    "NOTIFIED_KO": 0,
                    "NOTIFICATION_ERROR": 0,
                    "NOTIFICATION_REQUESTED": 0
                }
                """;

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(jsonInput);

        doThrow(new RuntimeException("Simulated failure"))
                .when(mockTableClient).createEntity(any(TableEntity.class));

        // When
        writeDataService.writeStateMetricsInTableStorage(jsonNode, mockLogger);

        // Then
        verify(mockLogger).warning(contains("Failed to write state metrics to Azure Table Storage"));
    }
}
