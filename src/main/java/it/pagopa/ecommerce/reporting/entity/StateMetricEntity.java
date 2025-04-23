package it.pagopa.ecommerce.reporting.entity;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.Map;

import com.azure.data.tables.models.TableEntity;

public class StateMetricEntity {

    // PartitionKey format: yyyy-MM-dd (ISO)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    /**
     * Creates a metric entity to store in Azure Table Storage.
     *
     * @param date            The date of the metric (used as PartitionKey, no
     *                        time).
     * @param clientId        The client ID.
     * @param paymentTypeCode The payment type code or name.
     * @param pspId           The PSP ID or name.
     * @param statusCounts    A map containing the count for each status (e.g.,
     *                        "ACTIVATED", "CLOSED", etc.).
     * @return TableEntity A ready-to-store TableEntity.
     */
    public static TableEntity createEntity(
                                           LocalDate date,
                                           String clientId,
                                           String paymentTypeCode,
                                           String pspId,
                                           Map<String, Integer> statusCounts
    ) {
        TableEntity entity = new TableEntity(date.format(DATE_FORMATTER), UUID.randomUUID().toString())
                .addProperty("clientId", clientId)
                .addProperty("paymentTypeCode", paymentTypeCode)
                .addProperty("pspId", pspId)
                .addProperty("createdAt", OffsetDateTime.now().format(TIMESTAMP_FORMATTER));

        // Add each status and its corresponding count as a property
        for (var entry : statusCounts.entrySet()) {
            entity.addProperty(entry.getKey(), entry.getValue());
        }

        return entity;
    }
}
