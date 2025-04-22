package it.pagopa.ecommerce.reporting.services;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReadDataService {
    private Set<String> ecommerceClientList;
    private Set<String> paymentTypeCodeList;
    private Map<String, Set<String>> pspList;

    public List<JsonNode> readData() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDateTime = OffsetDateTime.of(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour() - 2,
                0,
                0,
                0,
                now.getOffset()
        );
        OffsetDateTime endDateTime = OffsetDateTime.of(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour() - 1,
                0,
                0,
                0,
                now.getOffset()
        ).minusNanos(1);
        List<JsonNode> transactionMetricsResponseDtoList = new ArrayList<>();
        ecommerceClientList.forEach(
                client -> paymentTypeCodeList.forEach(
                        paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).forEach(pspId -> {
                            transactionMetricsResponseDtoList.add(
                                    EcommerceHelpdeskServiceClient.fetchTransactionMetrics(
                                            client,
                                            pspId,
                                            paymentMethodTypeCode,
                                            startDateTime,
                                            endDateTime,
                                            Logger.getLogger(ReadDataService.class.getName())
                                    )
                            );
                        }

                        )
                )
        );
        return transactionMetricsResponseDtoList;
    }

}
