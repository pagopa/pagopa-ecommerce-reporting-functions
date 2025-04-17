package it.pagopa.ecommerce.reporting.utils;

public enum PSP {
    PSP_1("PSP_1");

    private final String value;

    PSP(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
