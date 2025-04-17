package it.pagopa.ecommerce.reporting.scheduledOperations;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.client.EcommerceHelpdeskServiceClient;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.SearchMetricsRequestDto;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.SearchMetricsRequestTimeRangeDto;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.TransactionMetricsResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.function.Function;

@Component
public class ReadDataJob {

    private final EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient;

    private final Set<String> ecommerceClientList;
    private final Set<String> paymentTypeCodeList;
    private final Map<String, Set<String>> pspList;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReadDataJob(
            EcommerceHelpdeskServiceClient ecommerceHelpdeskServiceClient,
            @Value(
                "${ecommerce.clients.list}"
            ) Set<String> ecommerceClientList,
            @Value(
                "${ecommerce.paymentMethodsTypeCode.list}"
            ) Set<String> paymentTypeCodeList,
            @Value(
                "${ecommerce.paymentMethods.psp.list}"
            ) String pspList
    ) {
        this.ecommerceHelpdeskServiceClient = ecommerceHelpdeskServiceClient;
        this.ecommerceClientList = ecommerceClientList;
        this.paymentTypeCodeList = paymentTypeCodeList;
        this.pspList = parsePspMap(pspList, paymentTypeCodeList).fold(exception -> {
            throw exception;
        },
                Function.identity()
        );
        ;
    }

    private void writeData(List<TransactionMetricsResponseDto> metricsResponseDtos) {
        // TODO To be implemented
    }

    private List<TransactionMetricsResponseDto> readData() {
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
        List<TransactionMetricsResponseDto> transactionMetricsResponseDtoList = new ArrayList<>();
        ecommerceClientList.parallelStream().forEach(
                client -> paymentTypeCodeList.parallelStream().forEach(
                        paymentMethodTypeCode -> pspList.get(paymentMethodTypeCode).parallelStream().forEach(pspId -> {
                            SearchMetricsRequestDto searchMetricsRequestDto = new SearchMetricsRequestDto()
                                    .clientId(client)
                                    .paymentTypeCode(paymentMethodTypeCode)
                                    .pspId(pspId)
                                    .timeRange(
                                            new SearchMetricsRequestTimeRangeDto()
                                                    .startDate(startDateTime)
                                                    .endDate(endDateTime)
                                    );
                            ecommerceHelpdeskServiceClient.searchMetrics(searchMetricsRequestDto)
                                    .map(transactionMetricsResponseDtoList::add);
                        }

                        )
                )
        );
        return transactionMetricsResponseDtoList;

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

    @FunctionName("readAndWriteData")
    public void readAndWriteData(
                                 @TimerTrigger(
                                         name = "readAndWriteDataTrigger", schedule = "0 */5 * * * *"
                                 ) String timerInfo,
                                 ExecutionContext context
    ) {
        List<TransactionMetricsResponseDto> transactionMetricsResponseDtoList = readData();
        writeData(transactionMetricsResponseDtoList);
    }

}
