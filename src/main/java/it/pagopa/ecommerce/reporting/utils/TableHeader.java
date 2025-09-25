package it.pagopa.ecommerce.reporting.utils;

import lombok.Getter;

@Getter
public enum TableHeader {
    METODO("Metodo"),
    OK("OK"),
    KO("KO"),
    ABBANDONATO("ABBANDONATO"),
    IN_CORSO("IN CORSO"),
    DA_ANALIZZARE("DA ANALIZZARE");

    private final String label;

    TableHeader(String label) {
        this.label = label;
    }

}
