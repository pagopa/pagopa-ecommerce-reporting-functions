package it.pagopa.ecommerce.reporting.utils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AggregatedStatusGroup {
    private String date;
    private String clientId;
    private String pspId;
    private String paymentTypeCode;
    private Map<String, Integer> statusCounts = new HashMap<>();

    public AggregatedStatusGroup(String date, String clientId, String pspId, String paymentTypeCode, List<String> statusFields) {
        this.date = date;
        this.clientId = clientId;
        this.pspId = pspId;
        this.paymentTypeCode = paymentTypeCode;
        for (String status : statusFields) {
            statusCounts.put(status, 0);
        }
    }

    public void incrementStatus(String status, int value) {
        statusCounts.put(status, statusCounts.getOrDefault(status, 0) + value);
    }

    @Override
    public String toString() {
        return String.format(
            "Date: %s | ClientId: %s | PspId: %s | PaymentType: %s | Counts: %s",
            date, clientId, pspId, paymentTypeCode, statusCounts
        );
    }
}
