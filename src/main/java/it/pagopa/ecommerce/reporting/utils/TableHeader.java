package it.pagopa.ecommerce.reporting.utils;

import lombok.Getter;

@Getter
public enum TableHeader {
    METHOD("Metodo"),
    OK("OK"),
    KO("KO"),
    ABANDONED("ABBANDONATO"),
    IN_PROGRESS("IN CORSO"),
    TO_BE_ANALYZED("DA ANALIZZARE");

    private final String label;

    TableHeader(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
