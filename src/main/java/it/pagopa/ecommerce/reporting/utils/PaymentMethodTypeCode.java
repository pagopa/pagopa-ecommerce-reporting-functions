package it.pagopa.ecommerce.reporting.utils;

public enum PaymentMethodTypeCode {
    CARDS("CP"),
    PAYPAL("PAYPAL");

    private final String value;

    PaymentMethodTypeCode(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
