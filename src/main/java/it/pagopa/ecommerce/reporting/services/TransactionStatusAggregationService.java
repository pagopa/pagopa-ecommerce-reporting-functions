package it.pagopa.ecommerce.reporting.services;

import com.azure.core.http.rest.PagedIterable;
import com.azure.data.tables.TableClient;
import com.azure.data.tables.TableClientBuilder;
import com.azure.data.tables.models.ListEntitiesOptions;
import com.azure.data.tables.models.TableEntity;

import it.pagopa.ecommerce.reporting.utils.AggregatedStatusGroup;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Service class to aggregate status counts for transactions within a given date
 * range. The aggregation is performed on the data stored in an eCommerce
 * reporting Azure Table Storage. To use: <code>
 *     private static final StringCONNECTION_STRING =
 *     System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING");
 *     private static final String TRANSACTIONS_STATUS_TABLE = "StateReporting";
 *     TableClient tableClient = new TableClientBuilder() .connectionString(CONNECTION_STRING).tableName(TRANSACTIONS_STATUS_TABLE) .buildClient();
 *     LocalDate today = LocalDate.now();
 *     LocalDate lastMonday = today.minusWeeks(1).with(DayOfWeek.MONDAY);
 *     LocalDate lastSunday = lastMonday.with(DayOfWeek.SUNDAY);
 *     List<AggregatedStatusGroup> aggregated = TransactionStatusAggregationService.aggregateStatusCountByDateRange(lastMonday, lastSunday, logger);
 * </code>
 */
public class TransactionStatusAggregationService {

    private final String CONNECTION_STRING = System.getenv("ECOMMERCE_REPORTING_CONNECTION_STRING");
    private final String TRANSACTIONS_STATUS_TABLE = "StateReporting";

    private final List<String> STATUS_FIELDS = Arrays.asList(
            "ACTIVATED",
            "CLOSED",
            "NOTIFIED_OK",
            "EXPIRED",
            "REFUNDED",
            "CANCELED",
            "EXPIRED_NOT_AUTHORIZED",
            "UNAUTHORIZED",
            "REFUND_ERROR",
            "REFUND_REQUESTED",
            "CANCELLATION_REQUESTED",
            "CANCELLATION_EXPIRED",
            "AUTHORIZATION_REQUESTED",
            "AUTHORIZATION_COMPLETED",
            "CLOSURE_REQUESTED",
            "CLOSURE_ERROR"
    );

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

                String clientId = String.valueOf(entity.getProperty("ClientId"));
                String pspId = String.valueOf(entity.getProperty("PspId"));
                String paymentType = String.valueOf(entity.getProperty("PaymentTypeCode"));

                String key = String.join("|", partitionKey, clientId, pspId, paymentType);
                AggregatedStatusGroup group = aggregatedMap.get(key);

                if (group == null) {
                    group = new AggregatedStatusGroup(partitionKey, clientId, pspId, paymentType, STATUS_FIELDS);
                    aggregatedMap.put(key, group);
                }

                for (String status : STATUS_FIELDS) {
                    Object raw = entity.getProperty(status);
                    int count = raw != null ? Integer.parseInt(raw.toString()) : 0;
                    group.incrementStatus(status, count);
                }
            }
        }

        List<AggregatedStatusGroup> aggregated = new ArrayList<>(aggregatedMap.values());

        logger.info("[aggregateStatusCountByDateRange] Aggregation transaction status completed.");
        return aggregated;
    }
}
