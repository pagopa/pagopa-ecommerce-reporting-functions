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

        // EXPIRED shouldn't be in the map (0 occurrences)
        assertFalse(counts.containsKey("EXPIRED"));
    }

    @Test
    void testAggregateStatusCountByDateRangeFiltersZeroCounts() {
        // Given
        LocalDate startDate = LocalDate.of(2024, 4, 1);
        LocalDate endDate = LocalDate.of(2024, 4, 1);
        String partitionKey = "2024-04-01";

        TableEntity entity = new TableEntity(partitionKey, "row1");
        entity.addProperty("clientId", "clientA");
        entity.addProperty("pspId", "pspX");
        entity.addProperty("paymentTypeCode", "PT1");
        entity.addProperty("ACTIVATED", 5);
        entity.addProperty("CLOSED", 0);
        entity.addProperty("NOTIFIED_OK", 3);
        entity.addProperty("EXPIRED", 0);

        Iterable<TableEntity> iterable = List.of(entity);

        // Mocking the PagedIterable with an iterator
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

        // Verify that statuses with non-zero counts are present
        assertTrue(counts.containsKey("ACTIVATED"));
        assertEquals(5, counts.get("ACTIVATED"));
        assertTrue(counts.containsKey("NOTIFIED_OK"));
        assertEquals(3, counts.get("NOTIFIED_OK"));

        // Verify that statuses with zero counts are filtered out
        assertFalse(counts.containsKey("CLOSED"));
        assertFalse(counts.containsKey("EXPIRED"));

        // Verify the size of the map (should only contain the non-zero statuses)
        assertEquals(2, counts.size());
    }

    @Test
    void testAggregateStatusCountByClientAndPaymentTypeAggregatesIntoCategories() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 1);
        String partitionKey = "2025-09-01";

        TableEntity entity = new TableEntity(partitionKey, "row1");
        entity.addProperty("clientId", "clientA");
        entity.addProperty("pspId", "pspX"); // will be ignored in new grouping
        entity.addProperty("paymentTypeCode", "PT1");

        // raw statuses -> categories
        entity.addProperty("ACTIVATED", 5); // IN CORSO
        entity.addProperty("CANCELED", 2); // ABBANDONATO
        entity.addProperty("NOTIFIED_OK", 3); // OK
        entity.addProperty("UNAUTHORIZED", 4); // KO
        entity.addProperty("CLOSED", 0); // IN CORSO but should be filtered out

        Iterable<TableEntity> iterable = List.of(entity);

        // Mocking the PagedIterable with an iterator
        PagedIterable<TableEntity> pagedEntities = mock(PagedIterable.class);
        when(pagedEntities.iterator()).thenReturn(iterable.iterator());

        when(mockTableClient.listEntities(any(ListEntitiesOptions.class), isNull(), isNull()))
                .thenReturn(pagedEntities);

        // When
        List<AggregatedStatusGroup> result = service
                .aggregateStatusCountByClientAndPaymentType(startDate, endDate, mockLogger);

        // Then
        assertEquals(1, result.size());

        AggregatedStatusGroup group = result.get(0);
        assertEquals("clientA", group.getClientId());
        assertEquals("PT1", group.getPaymentTypeCode());

        Map<String, Integer> counts = group.getStatusCounts();

        // Verify category aggregation
        assertEquals(5, counts.get("IN CORSO")); // ACTIVATED
        assertEquals(2, counts.get("ABBANDONATO")); // CANCELED
        assertEquals(3, counts.get("OK")); // NOTIFIED_OK
        assertEquals(4, counts.get("KO")); // UNAUTHORIZED

        // Verify that zero-count statuses are filtered out
        assertFalse(counts.containsKey("CLOSED"));

        // Verify total categories size (only the non-zero categories remain)
        assertEquals(4, counts.size());
    }
}
