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
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ReadDataService {

    private static ReadDataService instance = null;
    private final Set<String> ecommerceClientList = MapParametersUtils
            .parseSetString(System.getenv("ECOMMERCE_CLIENTS_LIST")).fold(exception -> {
                throw exception;
            }, Function.identity());
    private final Set<String> paymentTypeCodeList = MapParametersUtils
            .parseSetString(System.getenv("ECOMMERCE_PAYMENT_METHODS_TYPE_CODE_LIST")).fold(exception -> {
                throw exception;
            }, Function.identity());

    private final Map<String, Set<String>> pspList = MapParametersUtils
            .parsePspMap(System.getenv("ECOMMERCE_PAYMENT_METHODS_PSP_LIST"), paymentTypeCodeList)
            .fold(exception -> {
                throw exception;
            },
                    Function.identity()
            );

    public static ReadDataService getInstance() {
        if (instance == null) {
            instance = new ReadDataService();
        }
        return instance;
    }

    public List<JsonNode> readData() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime startDateTime = OffsetDateTime.of(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                0,
                0,
                0,
                now.getOffset()
        ).minusHours(2);
        OffsetDateTime endDateTime = OffsetDateTime.of(
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                0,
                0,
                0,
                now.getOffset()
        ).minusHours(1).minusNanos(1);
        EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient = getEcommerceHelpdeskServiceClient();
        List<JsonNode> transactionMetricsResponseDtoList = new ArrayList<>();
        ecommerceClientList.forEach(
                client -> paymentTypeCodeList.forEach(
                        paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).forEach(pspId -> {
                            transactionMetricsResponseDtoList.add(
                                    ecommerceHelpdeskServiceClient.fetchTransactionMetrics(
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

    protected EcommerceHelpdeskServiceClient getEcommerceHelpdeskServiceClient() {
        return EcommerceHelpdeskServiceClient.getInstance();
    }

}
