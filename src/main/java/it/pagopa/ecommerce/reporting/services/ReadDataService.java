package it.pagopa.ecommerce.reporting.services;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.clients.EcommerceHelpdeskServiceClient;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class ReadDataService {

    private final EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    private final Set<String> ecommerceClientList;
    private final Set<String> paymentTypeCodeList;
    private final Map<String, Set<String>> pspList;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadDataService(
            EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient,
            Set<String> ecommerceClientList,
            Set<String> paymentTypeCodeList,
            String pspList
    ) {
        this.ecommerceHelpdeskServiceClient = ecommerceHelpdeskServiceClient;
        this.ecommerceClientList = ecommerceClientList;
        this.paymentTypeCodeList = paymentTypeCodeList;
        this.pspList = parsePspMap(pspList, paymentTypeCodeList).fold(exception -> {
            throw exception;
        },
                Function.identity()
        );

    }

    private Either<JobConfigurationException, Map<String, Set<String>>> parsePspMap(
                                                                                    String pspList,
                                                                                    Set<String> paymentMethodsTypeCodeToHandle
    ) {
        try {
            Set<String> expectedKeys = new HashSet<>(paymentMethodsTypeCodeToHandle);
            Map<String, Set<String>> paymentMethodPspMap = objectMapper
                    .readValue(pspList, new TypeReference<HashMap<String, Set<String>>>() {
                    });
            Set<String> configuredKeys = paymentMethodPspMap.keySet();
            expectedKeys.removeAll(configuredKeys);
            if (!expectedKeys.isEmpty()) {
                return Either.left(
                        new JobConfigurationException(
                                "Misconfigured paymentMethod keys. Missing keys: %s".formatted(expectedKeys)
                        )
                );
            }
            return Either.right(paymentMethodPspMap);
        } catch (JacksonException ignored) {
            // exception here is ignored on purpose in order to avoid secret configuration
            // logging in case of wrong configured json string object
            return Either.left(new JobConfigurationException("Invalid json configuration map"));
        }
    }

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
        ecommerceClientList.parallelStream().forEach(
                client -> paymentTypeCodeList.parallelStream().forEach(
                        paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).parallelStream().forEach(pspId -> {
                                    transactionMetricsResponseDtoList.add(EcommerceHelpdeskServiceClient.fetchTransactionMetrics(client,pspId,paymentMethodTypeCode,startDateTime,endDateTime, Logger.getLogger(ReadDataService.class.getName())));
                        }

                        )
                )
        );
        return transactionMetricsResponseDtoList;

    }

}
