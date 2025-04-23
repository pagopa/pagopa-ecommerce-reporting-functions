package it.pagopa.ecommerce.reporting.entity;

import com.azure.data.tables.models.TableEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class TransactionMetric {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_DATE;

    private final LocalDate date;
    private final String clientId;
    private final String paymentTypeCode;
    private final String pspId;
    private final Map<String, Integer> statusCounts;

    /**
     * Creates a TransactionMetric from a TableEntity
     *
     * @param entity The TableEntity to convert
     * @return A new TransactionMetric instance
     */
    public static TransactionMetric fromTableEntity(TableEntity entity) {
        LocalDate date = LocalDate.parse(entity.getPartitionKey(), DATE_FORMATTER);
        String clientId = entity.getProperty("clientId").toString();
        String paymentTypeCode = entity.getProperty("paymentTypeCode").toString();
        String pspId = entity.getProperty("pspId").toString();

        // Extract status counts
        Map<String, Integer> statusCounts = new HashMap<>();
        for (String key : entity.getProperties().keySet()) {
            if (!key.equals("clientId") && !key.equals("paymentTypeCode") &&
                    !key.equals("pspId") && !key.equals("createdAt")) {
                try {
                    Object value = entity.getProperty(key);
                    if (value instanceof Number) {
                        statusCounts.put(key, ((Number) value).intValue());
                    }
                } catch (Exception e) {
                    // Skip non-integer properties
                }
            }
        }

        return new TransactionMetric(date, clientId, paymentTypeCode, pspId, statusCounts);
    }

    public TransactionMetric(LocalDate date, String clientId, String paymentTypeCode, String pspId, Map<String, Integer> statusCounts) {
        this.date = date;
        this.clientId = clientId;
        this.paymentTypeCode = paymentTypeCode;
        this.pspId = pspId;
        this.statusCounts = new HashMap<>(statusCounts);
    }

    /**
     * Converts this TransactionMetric to a TableEntity for storage
     *
     * @return TableEntity ready for storage
     */
    public TableEntity toTableEntity() {
        return StateMetricEntity.createEntity(date, clientId, paymentTypeCode, pspId, statusCounts);
    }

    // Getters
    public LocalDate getDate() {
        return date;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPaymentTypeCode() {
        return paymentTypeCode;
    }

    public String getPspId() {
        return pspId;
    }

    public Map<String, Integer> getStatusCounts() {
        return new HashMap<>(statusCounts);
    }
}
