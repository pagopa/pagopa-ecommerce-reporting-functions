package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.core.http.rest.PagedIterable;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionStatusAggregationServiceTest {

    private TableClient mockTableClient;
    private Logger mockLogger;
    private TransactionStatusAggregationService service;

    @BeforeEach
    void setUp() {
        mockTableClient = mock(TableClient.class);
        mockLogger = mock(Logger.class);
        service = new TransactionStatusAggregationService(mockTableClient);
    }

    @Test
    void testAggregateStatusCountByDateRange() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2024, 4, 1);
        String partitionKey = "2024-04-01";

        TableEntity entityFirst = new TableEntity(partitionKey, "row1");
        entityFirst.addProperty("clientId", "clientA");
        entityFirst.addProperty("pspId", "pspX");
        entityFirst.addProperty("paymentTypeCode", "PT1");
        entityFirst.addProperty("ACTIVATED", 3);
        entityFirst.addProperty("CLOSED", 1);
        entityFirst.addProperty("NOTIFIED_OK", 2);

        TableEntity entitySecond = new TableEntity(partitionKey, "row1");
        entitySecond.addProperty("clientId", "clientA");
        entitySecond.addProperty("pspId", "pspX");
        entitySecond.addProperty("paymentTypeCode", "PT1");
        entitySecond.addProperty("ACTIVATED", 3);
        entitySecond.addProperty("CLOSED", 5);
        entitySecond.addProperty("NOTIFIED_OK", 9);

        Iterable<TableEntity> iterable = List.of(entityFirst, entitySecond);

        // Mocking the PagedIterable with simple iterator
        PagedIterable<TableEntity> pagedEntities = mock(PagedIterable.class);
        when(pagedEntities.iterator()).thenReturn(iterable.iterator());

        when(mockTableClient.listEntities(any(ListEntitiesOptions.class), isNull(), isNull()))
                .thenReturn(pagedEntities);

        // When
        List<AggregatedStatusGroup> result = service.aggregateStatusCountByDateRange(startDate, endDate, mockLogger);

        // Then
        assertEquals(1, result.size());

        AggregatedStatusGroup group = result.get(0);
        assertEquals("2024-04-01", group.getDate());
        assertEquals("clientA", group.getClientId());
        assertEquals("pspX", group.getPspId());
        assertEquals("PT1", group.getPaymentTypeCode());

        Map<String, Integer> counts = group.getStatusCounts();
        assertEquals(6, counts.get("ACTIVATED"));
        assertEquals(6, counts.get("CLOSED"));
        assertEquals(11, counts.get("NOTIFIED_OK"));
        assertEquals(0, counts.get("EXPIRED"));
    }
}
