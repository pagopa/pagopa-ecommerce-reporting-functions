package it.pagopa.ecommerce.reporting.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;

@Getter
public class AggregatedStatusGroup {
    private String date;
    private String clientId;
    private String pspId;
    private String paymentTypeCode;
    private Map<String, Integer> statusCounts = new HashMap<>();

    public AggregatedStatusGroup(
            String date,
            String clientId,
            String pspId,
            String paymentTypeCode,
            List<String> statusFields
    ) {
        this.date = date;
        this.clientId = clientId;
        this.pspId = pspId;
        this.paymentTypeCode = paymentTypeCode;
        for (String status : statusFields) {
            statusCounts.put(status, 0);
        }
    }

    public void incrementStatus(
                                String status,
                                int value
    ) {
        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + value);
    }

    @Override
    public String toString() {
        return String.format(
                "Date: %s | ClientId: %s | PspId: %s | PaymentType: %s | Counts: %s",
                date,
                clientId,
                pspId,
                paymentTypeCode,
                statusCounts
        );
    }

    public void filterZeroCountStatuses() {
        Map<String, Integer> filteredCounts = new HashMap<>();

        for (Map.Entry<String, Integer> entry : statusCounts.entrySet()) {
            if (entry.getValue() > 0) {
                filteredCounts.put(entry.getKey(), entry.getValue());
            }
        }

        this.statusCounts = filteredCounts;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getPspId() {
        return pspId;
    }

    public void setPspId(String pspId) {
        this.pspId = pspId;
    }

    public String getPaymentTypeCode() {
        return paymentTypeCode;
    }

    public void setPaymentTypeCode(String paymentTypeCode) {
        this.paymentTypeCode = paymentTypeCode;
    }

    public Map<String, Integer> getStatusCounts() {
        return statusCounts;
    }

    public void setStatusCounts(Map<String, Integer> statusCounts) {
        this.statusCounts = statusCounts;
    }
}
