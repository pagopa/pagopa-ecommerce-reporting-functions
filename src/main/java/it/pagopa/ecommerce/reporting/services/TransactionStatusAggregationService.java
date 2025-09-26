package it.pagopa.ecommerce.reporting.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;

import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;
import it.pagopa.ecommerce.reporting.utils.StatusStorageFields;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Service class to aggregate status counts for transactions within a given date
 * range. The aggregation is performed on the data stored in an eCommerce
 * reporting Azure Table Storage.
 *
 * To use: private static final String CONNECTION_STRING =
 * System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING"); private static final
 * String TRANSACTIONS_STATUS_TABLE = "StateReporting"; TableClient tableClient
 * = new TableClientBuilder() .connectionString(CONNECTION_STRING)
 * .tableName(TRANSACTIONS_STATUS_TABLE) .buildClient(); LocalDate today =
 * LocalDate.now(); LocalDate lastMonday =
 * today.minusWeeks(1).with(DayOfWeek.MONDAY); LocalDate lastSunday =
 * lastMonday.with(DayOfWeek.SUNDAY); List<AggregatedStatusGroup> aggregated =
 * TransactionStatusAggregationService.aggregateStatusCountByDateRange(lastMonday,
 * lastSunday, logger);
 */
public class TransactionStatusAggregationService {

    private final String CONNECTION_STRING = System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING");
    private final String TRANSACTIONS_STATUS_TABLE = System.getenv("ECOMMERCE_REPORTING_TABLE");

    private final TableClient tableClient;

    public TransactionStatusAggregationService() {
        this.tableClient = new TableClientBuilder()
                .connectionString(CONNECTION_STRING)
                .tableName(TRANSACTIONS_STATUS_TABLE)
                .buildClient();
    }

    public TransactionStatusAggregationService(TableClient tableClient) {
        this.tableClient = tableClient;
    }

    private static final Map<String, String> STATUS_TO_CATEGORY = Map.ofEntries(
            Map.entry("EXPIRED_NOT_AUTHORIZED", "ABBANDONATO"),
            Map.entry("CANCELLATION_EXPIRED", "ABBANDONATO"),
            Map.entry("CANCELED", "ABBANDONATO"),

            Map.entry("CLOSURE_ERROR", "DA ANALIZZARE"),
            Map.entry("EXPIRED", "DA ANALIZZARE"),
            Map.entry("REFUND_ERROR", "DA ANALIZZARE"),
            Map.entry("NOTIFICATION_ERROR", "DA ANALIZZARE"),

            Map.entry("NOTIFIED_KO", "KO"),
            Map.entry("REFUNDED", "KO"),
            Map.entry("UNAUTHORIZED", "KO"),

            Map.entry("NOTIFIED_OK", "OK"),

            Map.entry("ACTIVATED", "IN CORSO"),
            Map.entry("AUTHORIZATION_REQUESTED", "IN CORSO"),
            Map.entry("AUTHORIZATION_COMPLETED", "IN CORSO"),
            Map.entry("CLOSED", "IN CORSO"),
            Map.entry("NOTIFICATION_REQUESTED", "IN CORSO"),
            Map.entry("REFUND_REQUESTED", "IN CORSO"),
            Map.entry("CANCELLATION_REQUESTED", "IN CORSO")
    );

    /**
     * Aggregates transaction status counts from Azure Table Storage, grouped by
     * {@param clientId} and {@param paymentType}, over a given date range.
     * <p>
     * For each day between {@param startDate} and {@param endDate} (inclusive),
     * this method queries the table storage for entities matching the daily
     * partition key. For each entity, the method extracts the {@param clientId} and
     * {@param paymentTypeCode}, then accumulates status counts into an
     * {@link AggregatedStatusGroup}. Groups are keyed by {@param clientId} +
     * {@param paymentType}, and include the following status categories:
     * <ul>
     * <li>{@code ABBANDONATO}</li>
     * <li>{@code DA ANALIZZARE}</li>
     * <li>{@code KO}</li>
     * <li>{@code OK}</li>
     * <li>{@code IN CORSO}</li>
     * </ul>
     * <p>
     * Status counts are normalized into categories using the
     * {@code STATUS_TO_CATEGORY} mapping. If an entity property for a given status
     * is non-null and greater than zero, its count is added to the corresponding
     * category total in the aggregated group.
     *
     * @param startDate the inclusive start date of the reporting period
     * @param endDate   the inclusive end date of the reporting period
     * @param logger    the logger used to record execution progress
     * @return a list of {@link AggregatedStatusGroup} objects, one for each unique
     *         {@code clientId | paymentType} pair with aggregated status counts
     */
    public List<AggregatedStatusGroup> aggregateStatusCountByClientAndPaymentType(
                                                                                  LocalDate startDate,
                                                                                  LocalDate endDate,
                                                                                  Logger logger
    ) {
        logger.info("[aggregateStatusCountByClientAndPaymentType] Execution started.");

        Map<String, AggregatedStatusGroup> aggregatedMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            String partitionKey = formatter.format(date);

            PagedIterable<TableEntity> entities = tableClient.listEntities(
                    new ListEntitiesOptions().setFilter(String.format("PartitionKey eq '%s'", partitionKey)),
                    null,
                    null
            );

            for (TableEntity entity : entities) {
                String clientId = String.valueOf(entity.getProperty("clientId"));
                String paymentType = String.valueOf(entity.getProperty("paymentTypeCode"));

                // key: client + paymentType
                String key = String.join("|", clientId, paymentType);

                AggregatedStatusGroup group = aggregatedMap.get(key);
                if (group == null) {
                    group = new AggregatedStatusGroup(
                            null, // no longer grouping by date
                            clientId,
                            null, // pspId not needed anymore
                            paymentType,
                            new ArrayList<>(Set.of("ABBANDONATO", "DA ANALIZZARE", "KO", "OK", "IN CORSO"))
                    );
                    aggregatedMap.put(key, group);
                }

                for (String rawStatus : StatusStorageFields.values) {
                    Object raw = entity.getProperty(rawStatus);
                    int count = raw != null ? Integer.parseInt(raw.toString()) : 0;

                    if (count > 0) {
                        String category = STATUS_TO_CATEGORY.get(rawStatus);
                        if (category != null) {
                            group.incrementStatus(category, count);
                        }
                    }
                }
            }
        }

        List<AggregatedStatusGroup> aggregated = new ArrayList<>(aggregatedMap.values());

        for (AggregatedStatusGroup group : aggregated) {
            group.filterZeroCountStatuses();
        }

        logger.info("[aggregateStatusCountByClientAndPaymentType] Aggregation completed " + aggregated.size());
        // TODO: remove if we also want all 0s rows
        List<AggregatedStatusGroup> filteredAggregated = aggregated.stream()
                .filter(aggregatedStatusGroup -> !aggregatedStatusGroup.getStatusCounts().isEmpty())
                .toList();

        logger.info("[aggregateStatusCountByClientAndPaymentType] Aggregation filtered " + filteredAggregated.size());

        return filteredAggregated;
    }
}
