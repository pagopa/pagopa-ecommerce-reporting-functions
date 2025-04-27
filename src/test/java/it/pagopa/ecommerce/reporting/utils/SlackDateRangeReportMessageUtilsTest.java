package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class SlackDateRangeReportMessageUtilsTest {

    @Mock
    private Logger mockLogger;

    @Test
    void shouldFormatDateInItalianLocale() {
        // Given
        LocalDate date = LocalDate.of(2023, 5, 15);

        // When
        String result = SlackDateRangeReportMessageUtils.formatDate(date);

        // Then
        assertEquals("15 maggio 2023", result);
    }

    @Test
    void shouldFormatStatusDetailsCorrectly() {
        // Given
        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        statusCounts.put("ACTIVATED", 100);
        statusCounts.put("NOTIFIED_OK", 80);
        statusCounts.put("UNKNOWN_STATUS", 5);

        // When
        String result = SlackDateRangeReportMessageUtils.formatStatusDetails(statusCounts);

        // Then
        assertTrue(result.contains(":white_check_mark: *Attivate*: 100"));
        assertTrue(result.contains(":tada: *Complete con notifica*: 80"));
        assertTrue(result.contains(":black_circle: *UNKNOWN_STATUS*: 5"));
    }

    @Test
    void shouldSortStatusKeysAlphabetically() {
        // Given
        Map<String, Integer> statusCounts = new HashMap<>();
        statusCounts.put("CLOSED", 30);
        statusCounts.put("ACTIVATED", 100);
        statusCounts.put("EXPIRED", 20);

        // When
        String result = SlackDateRangeReportMessageUtils.formatStatusDetails(statusCounts);

        // Then
        int activatedIndex = result.indexOf("Attivate");
        int closedIndex = result.indexOf("Chiuse");
        int expiredIndex = result.indexOf("Scadute");

        assertTrue(activatedIndex < closedIndex);
        assertTrue(closedIndex < expiredIndex);
    }

    @Test
    void shouldFormatKnownPaymentType() {
        // Given
        String paymentTypeCode = "CP";

        // When
        String result = SlackDateRangeReportMessageUtils.formatPaymentTypeCode(paymentTypeCode);

        // Then
        assertEquals("   :credit_card: *Carte*", result);
    }

    @Test
    void shouldFormatUnknownPaymentType() {
        // Given
        String paymentTypeCode = "UNKNOWN_TYPE";

        // When
        String result = SlackDateRangeReportMessageUtils.formatPaymentTypeCode(paymentTypeCode);

        // Then
        assertEquals("   :moneybag: *UNKNOWN_TYPE*", result);
    }

    @Test
    void shouldSortByActivatedCountDescending() {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();

        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group1.incrementStatus("ACTIVATED", 50);

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2023-01-01",
                "client2",
                "psp2",
                "PPAL",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group2.incrementStatus("ACTIVATED", 100);

        AggregatedStatusGroup group3 = new AggregatedStatusGroup(
                "2023-01-01",
                "client3",
                "psp3",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group3.incrementStatus("ACTIVATED", 25);

        groups.add(group1);
        groups.add(group2);
        groups.add(group3);

        // When
        List<AggregatedStatusGroup> result = SlackDateRangeReportMessageUtils.sortAggregatedGroups(groups);

        // Then
        assertEquals("client2", result.get(0).getClientId()); // 100
        assertEquals("client1", result.get(1).getClientId()); // 50
        assertEquals("client3", result.get(2).getClientId()); // 25
    }

    @Test
    void shouldHandleMissingActivatedStatus() {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();

        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group1.incrementStatus("ACTIVATED", 50);

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2023-01-01",
                "client2",
                "psp2",
                "PPAL",
                Arrays.asList("NOTIFIED_OK")
        );
        group2.incrementStatus("NOTIFIED_OK", 100);

        groups.add(group1);
        groups.add(group2);

        // When
        List<AggregatedStatusGroup> result = SlackDateRangeReportMessageUtils.sortAggregatedGroups(groups);

        // Then
        assertEquals("client1", result.get(0).getClientId()); // 50
        assertEquals("client2", result.get(1).getClientId()); // 0 (default)
    }

    @Test
    void shouldCreateCorrectHeaderBlock() {
        // Given
        String startDate = "1 gennaio 2023";
        String endDate = "7 gennaio 2023";

        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils.createHeaderBlock(startDate, endDate);

        // Then
        assertEquals("header", result.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> textBlock = (Map<String, Object>) result.get("text");

        assertEquals("plain_text", textBlock.get("type"));
        assertEquals(
                ":pagopa: Report Settimanale Transazioni 1 gennaio 2023 - 7 gennaio 2023 :pagopa:",
                textBlock.get("text")
        );
        assertEquals(true, textBlock.get("emoji"));
    }

    @Test
    void shouldCreateCorrectImageBlock() {
        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils.createImageBlock();

        // Then
        assertEquals("image", result.get("type"));
        assertTrue(result.get("image_url").toString().contains("logo_asset.png"));
        assertEquals("PagoPA Logo", result.get("alt_text"));
    }

    @Test
    void shouldCreateCorrectDividerBlock() {
        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils.createDivider();

        // Then
        assertEquals("divider", result.get("type"));
    }

    @Test
    void shouldCreateCorrectGroupHeaderSection() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "testClient",
                "testPsp",
                "CP",
                Collections.emptyList()
        );

        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils.createGroupHeaderSection(group);

        // Then
        assertEquals("section", result.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> textBlock = (Map<String, Object>) result.get("text");

        assertEquals("mrkdwn", textBlock.get("type"));
        String text = (String) textBlock.get("text");

        assertTrue(text.contains("Client *testClient*"));
        assertTrue(text.contains("PSP *testPsp*"));
        assertTrue(text.contains("Carte"));
    }

    @Test
    void shouldCreateCorrectStatusDetailsSection() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "testClient",
                "testPsp",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group.incrementStatus("ACTIVATED", 100);
        group.incrementStatus("NOTIFIED_OK", 80);

        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils.createStatusDetailsSection(group);

        // Then
        assertEquals("section", result.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> textBlock = (Map<String, Object>) result.get("text");

        assertEquals("mrkdwn", textBlock.get("type"));
        String text = (String) textBlock.get("text");

        assertTrue(text.contains("*Attivate*: 100"));
        assertTrue(text.contains("*Complete con notifica*: 80"));
    }

    @Test
    void shouldCreateCorrectTextBlock() {
        // Given
        String blockType = "section";
        String textType = "mrkdwn";
        String content = "Test content";
        boolean emoji = true;

        // When
        Map<String, Object> result = SlackDateRangeReportMessageUtils
                .createTextBlock(blockType, textType, content, emoji);

        // Then
        assertEquals(blockType, result.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> textBlock = (Map<String, Object>) result.get("text");

        assertEquals(textType, textBlock.get("type"));
        assertEquals(content, textBlock.get("text"));
        assertEquals(emoji, textBlock.get("emoji"));
    }

    @Test
    void shouldCreateCompleteReport() throws Exception {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();

        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group1.incrementStatus("ACTIVATED", 100);
        group1.incrementStatus("NOTIFIED_OK", 80);

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2023-01-01",
                "client2",
                "psp2",
                "PPAL",
                Arrays.asList("ACTIVATED", "EXPIRED")
        );
        group2.incrementStatus("ACTIVATED", 50);
        group2.incrementStatus("EXPIRED", 10);

        groups.add(group1);
        groups.add(group2);

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 7);

        // When
        String[] results = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(groups, startDate, endDate, mockLogger);

        // Then
        assertNotNull(results);
        assertTrue(results.length > 0, "Should return at least one message");

        // Print the results for debugging
        for (int i = 0; i < results.length; i++) {
            System.out.println("Report result " + (i + 1) + ": " + results[i]);
        }

        // Check first message structure
        String firstMessage = results[0];
        assertTrue(firstMessage.contains("blocks"), "Result should contain 'blocks'");

        // Check date formatting
        String formattedStartDate = SlackDateRangeReportMessageUtils.formatDate(startDate);
        String formattedEndDate = SlackDateRangeReportMessageUtils.formatDate(endDate);

        assertTrue(
                firstMessage.contains(formattedStartDate),
                "Result should contain formatted start date: " + formattedStartDate
        );
        assertTrue(
                firstMessage.contains(formattedEndDate),
                "Result should contain formatted end date: " + formattedEndDate
        );

        // Check for client and PSP IDs across all messages
        boolean foundClient1 = false;
        boolean foundClient2 = false;
        boolean foundPsp1 = false;
        boolean foundPsp2 = false;
        boolean foundCP = false;
        boolean foundPPAL = false;

        for (String result : results) {
            if (result.contains("client1") || result.contains("\\*client1\\*"))
                foundClient1 = true;
            if (result.contains("client2") || result.contains("\\*client2\\*"))
                foundClient2 = true;
            if (result.contains("psp1") || result.contains("\\*psp1\\*"))
                foundPsp1 = true;
            if (result.contains("psp2") || result.contains("\\*psp2\\*"))
                foundPsp2 = true;
            if (result.contains("CP") || result.contains("Carte"))
                foundCP = true;
            if (result.contains("PPAL") || result.contains("PayPal"))
                foundPPAL = true;
        }

        assertTrue(foundClient1, "Results should contain client1");
        assertTrue(foundClient2, "Results should contain client2");
        assertTrue(foundPsp1, "Results should contain psp1");
        assertTrue(foundPsp2, "Results should contain psp2");
        assertTrue(foundCP, "Results should contain CP or its translation");
        assertTrue(foundPPAL, "Results should contain PPAL or its translation");

        // Verify each message has at most MAX_BLOCKS_PER_MESSAGE blocks
        for (String result : results) {
            int blockCount = countOccurrences(result);
            assertTrue(
                    blockCount <= 50,
                    "Each message should have at most 50 blocks, but found " + blockCount
            );
        }
    }

    // Helper method to count number of blocks
    private int countOccurrences(
                                 String jsonStr
    ) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> messageMap = objectMapper.readValue(jsonStr, Map.class);
        List<Object> blocks = (List<Object>) messageMap.get("blocks");
        return blocks.size();
    }

    @Test
    void shouldHandleEmptyGroups() throws Exception {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 7);

        // When
        String[] results = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(groups, startDate, endDate, mockLogger);

        // Then
        assertNotNull(results);
        assertEquals(1, results.length, "Should return exactly one message for empty groups");

        String result = results[0];
        assertTrue(result.contains("blocks"));
        assertTrue(result.contains("1 gennaio 2023"));
        assertTrue(result.contains("7 gennaio 2023"));
    }

    @Test
    void shouldIncrementCorrectly() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );

        // When
        group.incrementStatus("ACTIVATED", 5);
        group.incrementStatus("ACTIVATED", 10);
        group.incrementStatus("NOTIFIED_OK", 7);

        // Then
        assertEquals(15, group.getStatusCounts().get("ACTIVATED"));
        assertEquals(7, group.getStatusCounts().get("NOTIFIED_OK"));
    }

    @Test
    void shouldHandleNonInitializedStatus() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED")
        );

        // When
        group.incrementStatus("NOTIFIED_OK", 10); // Not in initial list

        // Then
        assertEquals(10, group.getStatusCounts().get("NOTIFIED_OK"));
    }

    @Test
    void shouldFormatCorrectly() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                Arrays.asList("ACTIVATED", "NOTIFIED_OK")
        );
        group.incrementStatus("ACTIVATED", 100);
        group.incrementStatus("NOTIFIED_OK", 80);

        // When
        String result = group.toString();

        // Then
        assertTrue(result.contains("Date: 2023-01-01"));
        assertTrue(result.contains("ClientId: client1"));
        assertTrue(result.contains("PspId: psp1"));
        assertTrue(result.contains("PaymentType: CP"));
        assertTrue(result.contains("ACTIVATED=100"));
        assertTrue(result.contains("NOTIFIED_OK=80"));
    }

    @Test
    void shouldInitializeStatusCountsWithZeros() {
        // Given
        List<String> statusFields = Arrays.asList("ACTIVATED", "NOTIFIED_OK", "EXPIRED");

        // When
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2023-01-01",
                "client1",
                "psp1",
                "CP",
                statusFields
        );

        // Then
        assertEquals(0, group.getStatusCounts().get("ACTIVATED"));
        assertEquals(0, group.getStatusCounts().get("NOTIFIED_OK"));
        assertEquals(0, group.getStatusCounts().get("EXPIRED"));
        assertEquals(3, group.getStatusCounts().size());
    }

    @Test
    void shouldSplitReportIntoMultipleMessagesWhenExceedingMaxBlocks() throws Exception {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();

        // Create enough groups to exceed the 50 block limit
        for (int i = 0; i < 20; i++) {
            AggregatedStatusGroup group = new AggregatedStatusGroup(
                    "2023-01-01",
                    "client" + i,
                    "psp" + i,
                    "CP",
                    Arrays.asList("ACTIVATED", "NOTIFIED_OK", "EXPIRED")
            );
            group.incrementStatus("ACTIVATED", 100 - i);
            group.incrementStatus("NOTIFIED_OK", 80 - i);
            group.incrementStatus("EXPIRED", i);

            groups.add(group);
        }

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 7);

        // When
        String[] results = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(groups, startDate, endDate, mockLogger);

        // Then
        assertNotNull(results);
        assertTrue(results.length > 1, "Should split into multiple messages");

        // Check that each message has at most 50 blocks
        for (String result : results) {
            int blockCount = countOccurrences(result);
            assertTrue(
                    blockCount <= 50,
                    "Each message should have at most 50 blocks, but found " + blockCount
            );
        }

        // First message should contain header information
        assertTrue(
                results[0].contains("Report Settimanale Transazioni"),
                "First message should contain report header"
        );
    }

    @Test
    void shouldIncludeAllGroupsAcrossMessages() throws Exception {
        // Given
        List<AggregatedStatusGroup> groups = new ArrayList<>();

        // Create groups with distinct identifiable names
        for (int i = 0; i < 15; i++) {
            String uniqueId = "UNIQUE_ID_" + i;
            AggregatedStatusGroup group = new AggregatedStatusGroup(
                    "2023-01-01",
                    uniqueId,
                    "psp" + i,
                    "CP",
                    Arrays.asList("ACTIVATED", "NOTIFIED_OK")
            );
            group.incrementStatus("ACTIVATED", 100 - i);
            group.incrementStatus("NOTIFIED_OK", 80 - i);

            groups.add(group);
        }

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 7);

        // When
        String[] results = SlackDateRangeReportMessageUtils
                .createAggregatedWeeklyReport(groups, startDate, endDate, mockLogger);

        // Then
        // Verify all unique IDs are present across all messages
        for (int i = 0; i < 15; i++) {
            String uniqueId = "UNIQUE_ID_" + i;
            boolean foundId = false;

            for (String result : results) {
                if (result.contains(uniqueId)) {
                    foundId = true;
                    break;
                }
            }

            assertTrue(foundId, "Group with ID " + uniqueId + " should be included in at least one message");
        }
    }
}
