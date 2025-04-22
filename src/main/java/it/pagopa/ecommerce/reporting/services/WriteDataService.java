package it.pagopa.ecommerce.reporting.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WriteDataService {

    private static WriteDataService instance = null;

    public static WriteDataService getInstance() {
        if (instance == null) {
            instance = new WriteDataService();
        }
        return instance;
    }

    public void writeData(List<JsonNode> metricsResponseDtos) {
    }
}
