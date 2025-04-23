package it.pagopa.ecommerce.reporting.utils;

import java.util.Arrays;
import java.util.List;

public class StatusStorageFields {

    public static final List<String> values = Arrays.asList(
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
}
