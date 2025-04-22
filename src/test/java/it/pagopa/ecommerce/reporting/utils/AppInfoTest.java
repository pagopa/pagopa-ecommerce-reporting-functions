package it.pagopa.ecommerce.reporting.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AppInfoTest {
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testAppInfoBuilderAndGetters() {
        AppInfo appInfo = AppInfo.builder()
                .name("ecommerce-app")
                .version("1.2.3")
                .environment("dev")
                .build();

        assertEquals("ecommerce-app", appInfo.getName());
        assertEquals("1.2.3", appInfo.getVersion());
        assertEquals("dev", appInfo.getEnvironment());
    }

    @Test
    void testAppInfoSettersAndEquals() {
        AppInfo app1 = new AppInfo();
        app1.setName("test");
        app1.setVersion("0.0.1");
        app1.setEnvironment("prod");

        AppInfo app2 = AppInfo.builder()
                .name("test")
                .version("0.0.1")
                .environment("prod")
                .build();

        assertEquals(app1, app2);
        assertEquals(app1.hashCode(), app2.hashCode());
    }

    @Test
    void testJsonSerialization() throws Exception {
        AppInfo appInfo = AppInfo.builder()
                .name("ecommerce-app")
                .version("1.0.0")
                .build();

        String json = objectMapper.writeValueAsString(appInfo);

        assertTrue(json.contains("ecommerce-app"));
        assertTrue(json.contains("1.0.0"));
        assertFalse(json.contains("environment")); // should be omitted because it's null
    }

    @Test
    void testJsonDeserializationIgnoresUnknownFields() throws Exception {
        String json = """
            {
              "name": "ecommerce-app",
              "version": "1.0.0",
              "environment": "test",
              "extraField": "shouldBeIgnored"
            }
        """;

        AppInfo appInfo = objectMapper.readValue(json, AppInfo.class);

        assertEquals("ecommerce-app", appInfo.getName());
        assertEquals("1.0.0", appInfo.getVersion());
        assertEquals("test", appInfo.getEnvironment());
    }
}
