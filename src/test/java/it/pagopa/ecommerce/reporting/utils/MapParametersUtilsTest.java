package it.pagopa.ecommerce.reporting.utils;

import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MapParametersUtilsTest {

    @Test
    public void parseSetShouldReturnEitherLeft() {
        Either<JobConfigurationException, Set<String>> val = MapParametersUtils.parseSetString("test");
        assertTrue(val.isLeft());
        assertInstanceOf(JobConfigurationException.class, val.getLeft());
    }

    @Test
    public void parseSetShouldReturnEitherRight() {
        Either<JobConfigurationException, Set<String>> val = MapParametersUtils.parseSetString("[\"test\",\"test2\"]");
        assertTrue(val.isRight());
        Set<String> value = val.get();
        assertTrue(value.contains("test"));
        assertTrue(value.contains("test2"));
    }

    @Test
    public void parseMapShouldReturnEitherRight() {
        Either<JobConfigurationException, Map<String, Set<String>>> val = MapParametersUtils.parsePspMap(
                "{\"PAY_1\":[\"PSP_1\",\"PSP_2\"],\"PAY_2\":[\"PSP_3\",\"PSP_2\"]}",
                Set.of("PAY_1", "PAY_2")
        );
        assertTrue(val.isRight());
        Map<String, Set<String>> map = val.get();
        assertTrue(map.get("PAY_1").contains("PSP_1"));
        assertTrue(map.get("PAY_1").contains("PSP_2"));
        assertTrue(map.get("PAY_2").contains("PSP_3"));
        assertTrue(map.get("PAY_2").contains("PSP_2"));
    }

    @Test
    public void parseMapShouldReturnEitherLeftForInvalidConfigurationMap() {
        Either<JobConfigurationException, Map<String, Set<String>>> val = MapParametersUtils
                .parsePspMap("test", new HashSet<>());
        assertTrue(val.isLeft());
        assertInstanceOf(JobConfigurationException.class, val.getLeft());
        assertEquals("Invalid json configuration map", val.getLeft().getMessage());
    }

    @Test
    public void parseMapShouldReturnEitherLeftForMissingKeys() {
        Either<JobConfigurationException, Map<String, Set<String>>> val = MapParametersUtils.parsePspMap(
                "{\"PAY_1\":[\"PSP_1\",\"PSP_2\"]}",
                Set.of("PAY_1", "PAY_2")
        );
        assertTrue(val.isLeft());
        assertInstanceOf(JobConfigurationException.class, val.getLeft());
        assertEquals("Misconfigured paymentMethod keys. Missing keys: [PAY_2]", val.getLeft().getMessage());
    }

}
