package it.pagopa.ecommerce.reporting.services;

import com.azure.data.tables.TableClient;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;
import com.azure.core.http.rest.PagedIterable;
import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junitpioneer.jupiter.SetEnvironmentVariable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionStatusAggregationServiceTest {

    private TableClient mockTableClient;
    private Logger mockLogger;
    private TransactionStatusAggregationService service;

    @Mock
    private PagedIterable<TableEntity> mockPagedIterable;

    @BeforeEach
    void setUp() {
        mockTableClient = mock(TableClient.class);
        mockLogger = mock(Logger.class);
        service = new TransactionStatusAggregationService(mockTableClient);
    }

    @Test
    void testAggregateStatusCountByClientAndPaymentType() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 1);

        TableEntity entity = new TableEntity("2025-09-01", "row1");
        entity.addProperty("clientId", "clientA");
        entity.addProperty("pspId", "pspX");
        entity.addProperty("paymentTypeCode", "PT1");
        entity.addProperty("ACTIVATED", 5); // IN CORSO
        entity.addProperty("CANCELED", 2); // ABBANDONATO
        entity.addProperty("NOTIFIED_OK", 3); // OK
        entity.addProperty("UNAUTHORIZED", 4); // KO
        entity.addProperty("CLOSED", 0);

        List<TableEntity> entities = List.of(entity);

        when(mockPagedIterable.iterator()).thenReturn(entities.iterator());
        when(mockTableClient.listEntities(any(ListEntitiesOptions.class), isNull(), isNull()))
                .thenReturn(mockPagedIterable);

        // When
        List<AggregatedStatusGroup> result = service
                .aggregateStatusCountByClientAndPaymentType(startDate, endDate, mockLogger);

        // Then
        assertEquals(1, result.size());

        AggregatedStatusGroup group = result.get(0);
        assertEquals("clientA", group.getClientId());
        assertEquals("PT1", group.getPaymentTypeCode());
        assertNull(group.getDate()); // Date should be null for this aggregation
        assertNull(group.getPspId()); // PSP should be null for this aggregation

        Map<String, Integer> counts = group.getStatusCounts();
        assertEquals(5, counts.get("IN CORSO"));
        assertEquals(2, counts.get("ABBANDONATO"));
        assertEquals(3, counts.get("OK"));
        assertEquals(4, counts.get("KO"));
        assertEquals(4, counts.size());

        // Verify logging
        verify(mockLogger, atLeastOnce()).info(contains("aggregateStatusCountByClientAndPaymentType"));
        verify(mockLogger, atLeastOnce()).info(contains("Aggregation completed"));
        verify(mockLogger, atLeastOnce()).info(contains("Aggregation filtered"));
    }

    @Test
    void testAggregateStatusCountByClientAndPaymentTypeFiltersEmptyGroups() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 1);

        // Entity with all zero counts
        TableEntity entityAllZeros = new TableEntity("2025-09-01", "row1");
        entityAllZeros.addProperty("clientId", "clientEmpty");
        entityAllZeros.addProperty("pspId", "pspX");
        entityAllZeros.addProperty("paymentTypeCode", "PT1");
        entityAllZeros.addProperty("ACTIVATED", 0);
        entityAllZeros.addProperty("CANCELED", 0);
        entityAllZeros.addProperty("NOTIFIED_OK", 0);

        // Entity with some counts
        TableEntity entityWithCounts = new TableEntity("2025-09-01", "row2");
        entityWithCounts.addProperty("clientId", "clientActive");
        entityWithCounts.addProperty("pspId", "pspY");
        entityWithCounts.addProperty("paymentTypeCode", "PT2");
        entityWithCounts.addProperty("ACTIVATED", 3);
        entityWithCounts.addProperty("NOTIFIED_OK", 2);

        List<TableEntity> entities = List.of(entityAllZeros, entityWithCounts);

        when(mockPagedIterable.iterator()).thenReturn(entities.iterator());
        when(mockTableClient.listEntities(any(ListEntitiesOptions.class), isNull(), isNull()))
                .thenReturn(mockPagedIterable);

        // When
        List<AggregatedStatusGroup> result = service
                .aggregateStatusCountByClientAndPaymentType(startDate, endDate, mockLogger);

        // Then
        assertEquals(1, result.size()); // Only the group with counts should remain

        AggregatedStatusGroup group = result.get(0);
        assertEquals("clientActive", group.getClientId());
        assertEquals("PT2", group.getPaymentTypeCode());

        Map<String, Integer> counts = group.getStatusCounts();
        assertEquals(3, counts.get("IN CORSO"));
        assertEquals(2, counts.get("OK"));
        assertEquals(2, counts.size());
    }

    @Test
    void testAggregateStatusCountByClientAndPaymentTypeUnknownStatus() {
        // Given
        LocalDate startDate = LocalDate.of(2025, 9, 1);
        LocalDate endDate = LocalDate.of(2025, 9, 1);

        TableEntity entity = new TableEntity("2025-09-01", "row1");
        entity.addProperty("clientId", "clientA");
        entity.addProperty("pspId", "pspX");
        entity.addProperty("paymentTypeCode", "PT1");
        entity.addProperty("UNKNOWN_STATUS", 5); // status is not in the mapping
        entity.addProperty("ACTIVATED", 3);

        List<TableEntity> entities = List.of(entity);

        when(mockPagedIterable.iterator()).thenReturn(entities.iterator());
        when(mockTableClient.listEntities(any(ListEntitiesOptions.class), isNull(), isNull()))
                .thenReturn(mockPagedIterable);

        // When
        List<AggregatedStatusGroup> result = service
                .aggregateStatusCountByClientAndPaymentType(startDate, endDate, mockLogger);

        // Then
        assertEquals(1, result.size());
        AggregatedStatusGroup group = result.get(0);
        Map<String, Integer> counts = group.getStatusCounts();

        // Only ACTIVATED should be mapped to "IN CORSO"
        assertEquals(3, counts.get("IN CORSO"));
        assertEquals(1, counts.size());

        // UNKNOWN_STATUS should be ignored since it's not in STATUS_TO_CATEGORY mapping
        assertFalse(counts.containsKey("UNKNOWN_STATUS"));
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

    @Test
    @SetEnvironmentVariable(
            key = "ECOMMERCE_REPORTING_CONNECTION_STRING", value = "DefaultEndpointsProtocol=http;AccountName=test;AccountKey=test;BlobEndpoint=http://127.0.0.1:10000/test;QueueEndpoint=http://127.0.0.1:10001/test;TableEndpoint=http://127.0.0.1:10002/test;"
    )
    @SetEnvironmentVariable(key = "ECOMMERCE_REPORTING_TABLE", value = "test-table")
    void testDefaultConstructorWithEnvironmentVariables() {
        // Test the default constructor that reads from environment variables
        TransactionStatusAggregationService defaultService = new TransactionStatusAggregationService();
        assertNotNull(defaultService);
    }
}
