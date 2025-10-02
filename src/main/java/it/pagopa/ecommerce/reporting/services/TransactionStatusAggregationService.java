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
import org.slf4j.Logger;

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

    public static final String ABANDONED = "ABBANDONATO";
    public static final String TO_BE_ANALYZED = "DA ANALIZZARE";
    public static final String KO = "KO";
    public static final String OK = "OK";
    public static final String IN_PROGRESS = "IN CORSO";
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
            Map.entry("EXPIRED_NOT_AUTHORIZED", ABANDONED),
            Map.entry("CANCELLATION_EXPIRED", ABANDONED),
            Map.entry("CANCELED", ABANDONED),

            Map.entry("CLOSURE_ERROR", TO_BE_ANALYZED),
            Map.entry("EXPIRED", TO_BE_ANALYZED),
            Map.entry("REFUND_ERROR", TO_BE_ANALYZED),

            Map.entry("REFUNDED", KO),
            Map.entry("UNAUTHORIZED", KO),

            Map.entry("NOTIFIED_OK", OK),

            Map.entry("ACTIVATED", IN_PROGRESS),
            Map.entry("AUTHORIZATION_REQUESTED", IN_PROGRESS),
            Map.entry("AUTHORIZATION_COMPLETED", IN_PROGRESS),
            Map.entry("CLOSED", IN_PROGRESS),
            Map.entry("REFUND_REQUESTED", IN_PROGRESS),
            Map.entry("CANCELLATION_REQUESTED", IN_PROGRESS),
            Map.entry("CLOSURE_REQUESTED", IN_PROGRESS)
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
            processEntitiesForDate(entities, aggregatedMap);
        }

        List<AggregatedStatusGroup> aggregated = new ArrayList<>(aggregatedMap.values());
        aggregated.forEach(AggregatedStatusGroup::filterZeroCountStatuses);

        logger.info("[aggregateStatusCountByClientAndPaymentType] Aggregation completed {}", aggregated.size());
        List<AggregatedStatusGroup> filteredAggregated = aggregated.stream()
                .filter(aggregatedStatusGroup -> !aggregatedStatusGroup.getStatusCounts().isEmpty())
                .toList();

        logger.info("[aggregateStatusCountByClientAndPaymentType] Aggregation filtered {}", filteredAggregated.size());
        return filteredAggregated;
    }

    /**
     * Processes all table entities for a specific date and updates the aggregated
     * status map.
     * <p>
     * This method iterates through all entities retrieved for a given date
     * partition and delegates the processing of each individual entity to
     * {@link #processEntity(TableEntity, Map)}.
     *
     * @param entities      the collection of table entities to process for the date
     * @param aggregatedMap the map containing aggregated status groups, keyed by
     *                      clientId|paymentType
     */
    private void processEntitiesForDate(
                                        PagedIterable<TableEntity> entities,
                                        Map<String, AggregatedStatusGroup> aggregatedMap
    ) {
        for (TableEntity entity : entities) {
            processEntity(entity, aggregatedMap);
        }
    }

    /**
     * Processes a single table entity and updates the corresponding aggregated
     * status group.
     * <p>
     * Extracts the clientId and paymentTypeCode from the entity to create a unique
     * key. If no aggregated group exists for this key, creates a new one with
     * default status categories. Then processes all status fields for this entity
     * using {@link #processStatusFields(TableEntity, AggregatedStatusGroup)}.
     *
     * @param entity        the table entity to process
     * @param aggregatedMap the map containing aggregated status groups, keyed by
     *                      clientId|paymentType
     */
    private void processEntity(
                               TableEntity entity,
                               Map<String, AggregatedStatusGroup> aggregatedMap
    ) {
        String clientId = String.valueOf(entity.getProperty("clientId"));
        String paymentType = String.valueOf(entity.getProperty("paymentTypeCode"));
        String key = String.join("|", clientId, paymentType);

        AggregatedStatusGroup group = aggregatedMap.computeIfAbsent(
                key,
                k -> new AggregatedStatusGroup(
                        null, // no longer grouping by date
                        clientId,
                        null, // pspId not needed anymore
                        paymentType,
                        new ArrayList<>(Set.of(ABANDONED, TO_BE_ANALYZED, KO, OK, IN_PROGRESS))
                )
        );

        processStatusFields(entity, group);
    }

    /**
     * Processes all status fields from a table entity and updates the aggregated
     * status group.
     * <p>
     * Iterates through all status fields defined in
     * {@link StatusStorageFields#values} and extracts the count for each status. If
     * the count is greater than zero, maps the raw status to its corresponding
     * category using {@link #STATUS_TO_CATEGORY} and increments the aggregated
     * group's status count.
     * <p>
     * Raw status values that are not found in the mapping default to
     * {@link #IN_PROGRESS}.
     *
     * @param entity the table entity containing status field data
     * @param group  the aggregated status group to update with status counts
     */
    private void processStatusFields(
                                     TableEntity entity,
                                     AggregatedStatusGroup group
    ) {
        for (String rawStatus : StatusStorageFields.values) {
            Object raw = entity.getProperty(rawStatus);
            int count = raw != null ? Integer.parseInt(raw.toString()) : 0;

            if (count > 0) {
                String category = STATUS_TO_CATEGORY.getOrDefault(rawStatus, IN_PROGRESS);
                group.incrementStatus(category, count);
            }
        }
    }
}
