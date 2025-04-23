package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;

import java.io.IOException;
import java.util.*;

public class MapParametersUtils {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static Either<JobConfigurationException, Set<String>> parseSetString(String inputParam) {
        try {
            return Either.right(objectMapper.readValue(inputParam, new TypeReference<Set<String>>() {
            }));
        } catch (IOException ignored) {
            return Either.left(new JobConfigurationException("Invalid json configuration map"));
        }
    }

    public static Either<JobConfigurationException, Map<String, Set<String>>> parsePspMap(
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
        } catch (IOException ignored) {
            // exception here is ignored on purpose in order to avoid secret configuration
            // logging in case of wrong configured json string object
            return Either.left(new JobConfigurationException("Invalid json configuration map"));
        }
    }

}
