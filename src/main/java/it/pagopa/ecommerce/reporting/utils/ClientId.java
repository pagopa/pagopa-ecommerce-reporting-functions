package it.pagopa.ecommerce.reporting.utils;

public enum ClientId {
    CHECKOUT("CHEKCOUT"),
    IO("IO"),
    CHECKOUT_CART("CHECKOUT_CART"),
    WISP_REDIRECT("WISP_REDIRECT");

    private final String value;

    ClientId(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
