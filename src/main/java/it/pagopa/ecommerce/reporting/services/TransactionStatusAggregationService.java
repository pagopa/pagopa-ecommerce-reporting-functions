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
     * Aggregates the status counts for transactions in a specified date range.
     *
     * @param startDate the start date of the aggregation period
     * @param endDate   the end date of the aggregation period
     * @param logger    the logger to log information during processing
     * @return a list of aggregated status counts for each unique combination of
     *         partition key, client ID, PSP ID, and payment type code
     */
    public List<AggregatedStatusGroup> aggregateStatusCountByDateRange(
                                                                       LocalDate startDate,
                                                                       LocalDate endDate,
                                                                       Logger logger
    ) {

        logger.info("[aggregateStatusCountByDateRange] Execution started.");

        Map<String, AggregatedStatusGroup> aggregatedMap = new HashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {

            logger.info("[aggregateStatusCountByDateRange] Execution for date: " + date);

            String partitionKey = formatter.format(date);

            PagedIterable<TableEntity> entities = tableClient.listEntities(
                    new ListEntitiesOptions().setFilter(String.format("PartitionKey eq '%s'", partitionKey)),
                    null,
                    null
            );

            logger.info("[aggregateStatusCountByDateRange] Found " + entities.stream().count() + " entities");

            for (TableEntity entity : entities) {

                String clientId = String.valueOf(entity.getProperty("clientId"));
                String pspId = String.valueOf(entity.getProperty("pspId"));
                String paymentType = String.valueOf(entity.getProperty("paymentTypeCode"));

                String key = String.join("|", partitionKey, clientId, pspId, paymentType);
                AggregatedStatusGroup group = aggregatedMap.get(key);

                if (group == null) {
                    group = new AggregatedStatusGroup(
                            partitionKey,
                            clientId,
                            pspId,
                            paymentType,
                            StatusStorageFields.values
                    );
                    aggregatedMap.put(key, group);
                }

                for (String status : StatusStorageFields.values) {
                    Object raw = entity.getProperty(status);
                    int count = raw != null ? Integer.parseInt(raw.toString()) : 0;
                    group.incrementStatus(status, count);
                }
            }
        }

        List<AggregatedStatusGroup> aggregated = new ArrayList<>(aggregatedMap.values());

        // Filter out statuses with count 0 from each AggregatedStatusGroup
        for (AggregatedStatusGroup group : aggregated) {
            group.filterZeroCountStatuses();
        }

        logger.info("[aggregateStatusCountByDateRange] Aggregation transaction status completed.");
        return aggregated;
    }

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

                // new key: client + paymentType
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
