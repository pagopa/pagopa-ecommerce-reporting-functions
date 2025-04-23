package it.pagopa.ecommerce.reporting.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import com.azure.data.tables.models.TableEntity;
import org.junit.jupiter.api.Test;

public class StateMetricEntityTest {

    @Test
    void shouldCreateEntityWithAllProperties() {

        LocalDate date = LocalDate.of(2025, 4, 22);
        String clientId = "client-xyz";
        String paymentTypeCode = "PTC001";
        String pspId = "psp-abc";

        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("ACTIVATED", 10);
        statusCounts.put("CLOSED", 5);
        statusCounts.put("NOTIFIED_OK", 3);

        TableEntity entity = StateMetricEntity.createEntity(date, clientId, paymentTypeCode, pspId, statusCounts);

        assertEquals("2025-04-22", entity.getPartitionKey());
        assertNotNull(entity.getRowKey());
        assertEquals(clientId, entity.getProperty("clientId"));
        assertEquals(paymentTypeCode, entity.getProperty("paymentTypeCode"));
        assertEquals(pspId, entity.getProperty("pspId"));

        assertTrue(entity.getProperties().containsKey("createdAt"));
        assertTrue(entity.getProperty("createdAt") instanceof String);
        assertTrue(
                ((String) entity.getProperty("createdAt")).startsWith("2025")
                        || ((String) entity.getProperty("createdAt")).contains("T")
        );

        assertEquals(10, entity.getProperty("ACTIVATED"));
        assertEquals(5, entity.getProperty("CLOSED"));
        assertEquals(3, entity.getProperty("NOTIFIED_OK"));
    }
}
