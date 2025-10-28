package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.*;
import org.slf4j.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

import java.lang.reflect.Method;

class SlackDateRangeReportMessageUtilsTest {

    @ExtendWith(MockitoExtension.class)

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
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
    void shouldFilterByClientIdAndSortByActivated() {
        // Given
        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientA",
                "pspX",
                "PPAL",
                List.of("OK", "KO")
        );
        group1.incrementStatus("ACTIVATED", 5);
        group1.incrementStatus("FAILED", 2);

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientA",
                "pspY",
                "CP",
                List.of("OK", "KO")
        );
        group2.incrementStatus("ACTIVATED", 10);
        group2.incrementStatus("KO", 1);

        AggregatedStatusGroup group3 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientB",
                "pspZ",
                "APPL",
                List.of("OK", "KO")
        );
        group3.incrementStatus("ACTIVATED", 7);

        List<AggregatedStatusGroup> groups = List.of(group1, group2, group3);

        // When
        List<AggregatedStatusGroup> sorted = SlackDateRangeReportMessageUtils.sortAggregatedGroups(groups, "clientA");

        // Then
        // Only clientA groups
        assertEquals(2, sorted.size());
        assertTrue(sorted.stream().allMatch(g -> "clientA".equals(g.getClientId())));

        // Sorted with CP (Carte) first, then alphabetically
        assertEquals("CP", sorted.get(0).getPaymentTypeCode());
        assertEquals("PPAL", sorted.get(1).getPaymentTypeCode());
    }

    @Test
    void shouldReturnEmptyListIfNoClientMatches() {
        // Given
        AggregatedStatusGroup group = new AggregatedStatusGroup(
                "2025-09-16",
                "clientB",
                "pspX",
                "CP",
                List.of("OK")
        );
        group.incrementStatus("ACTIVATED", 3);

        // When
        List<AggregatedStatusGroup> sorted = SlackDateRangeReportMessageUtils
                .sortAggregatedGroups(List.of(group), "clientA");

        // Then
        assertTrue(sorted.isEmpty(), "No groups should be returned for unmatched clientId");
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
        List<AggregatedStatusGroup> resultClient1 = SlackDateRangeReportMessageUtils
                .sortAggregatedGroups(groups, "client1");
        List<AggregatedStatusGroup> resultClient2 = SlackDateRangeReportMessageUtils
                .sortAggregatedGroups(groups, "client2");

        // Then
        assertEquals("client1", resultClient1.get(0).getClientId()); // 50
        assertEquals("client2", resultClient2.get(0).getClientId()); // 0 (default)
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
        assertTrue(result.get("image_url").toString().contains("logo_pagopacorp.png"));
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
    void shouldCreateEmptyReportMessageWithReflection() throws Exception {
        // Given
        LocalDate startDate = LocalDate.of(2023, 1, 1);
        LocalDate endDate = LocalDate.of(2023, 1, 7);

        // Use reflection to access the private static method
        Method method = SlackDateRangeReportMessageUtils.class
                .getDeclaredMethod("createEmptyReportMessage", LocalDate.class, LocalDate.class);
        method.setAccessible(true);

        // When
        String json = (String) method.invoke(null, startDate, endDate);

        // Then
        JsonNode root = OBJECT_MAPPER.readTree(json);

        // Verify that "blocks" is an array with 3 elements
        assertTrue(root.has("blocks"));
        JsonNode blocks = root.get("blocks");
        assertEquals(3, blocks.size());

        // 1st block: header
        JsonNode header = blocks.get(0);
        assertEquals("header", header.get("type").asText());
        JsonNode headerText = header.get("text");
        assertEquals("plain_text", headerText.get("type").asText());
        assertEquals(
                ":pagopa: Report Settimanale Transazioni 1 gennaio 2023 - 7 gennaio 2023 :pagopa:",
                headerText.get("text").asText()
        );
        assertTrue(headerText.get("emoji").asBoolean());

        // 2nd block: image
        JsonNode imageBlock = blocks.get(1);
        assertEquals("image", imageBlock.get("type").asText());

        // 3rd block: text section
        JsonNode sectionBlock = blocks.get(2);
        assertEquals("section", sectionBlock.get("type").asText());
        JsonNode sectionText = sectionBlock.get("text");
        assertEquals("mrkdwn", sectionText.get("type").asText());
        assertEquals(
                "Non ci sono dati da visualizzare per il periodo selezionato.",
                sectionText.get("text").asText()
        );
    }

    @Test
    void shouldCreateTableBlocksWithAggregatedGroups() {
        // Given
        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientA",
                "pspX",
                "CP",
                List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
        );

        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientB",
                "pspY",
                "CP",
                List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
        );

        List<AggregatedStatusGroup> groups = Arrays.asList(group1, group2);

        // When
        List<Map<String, Object>> blocks = SlackDateRangeReportMessageUtils.createTableBlocks(groups);

        // Then
        assertNotNull(blocks);
        assertEquals(1, blocks.size(), "Expected only one table block");

        Map<String, Object> tableBlock = blocks.get(0);
        assertEquals("table", tableBlock.get("type"));

        @SuppressWarnings("unchecked")
        List<List<Map<String, Object>>> rows = (List<List<Map<String, Object>>>) tableBlock.get("rows");

        // Should have 1 header + 2 data rows
        assertEquals(3, rows.size());
    }

    @Test
    void shouldCreateAggregatedTableWeeklyReportWithGroups() throws JsonProcessingException {

        LocalDate startDate = LocalDate.of(2025, 9, 16);
        LocalDate endDate = LocalDate.of(2025, 9, 22);

        AggregatedStatusGroup group1 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientA",
                "pspX",
                "CP",
                List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
        );
        AggregatedStatusGroup group2 = new AggregatedStatusGroup(
                "2025-09-16",
                "clientB",
                "pspY",
                "CP",
                List.of("OK", "KO", "ABBANDONATO", "IN CORSO", "DA ANALIZZARE")
        );

        List<AggregatedStatusGroup> groups = List.of(group1, group2);

        // When
        String[] messages = SlackDateRangeReportMessageUtils.createAggregatedTableWeeklyReport(
                groups,
                startDate,
                endDate,
                mockLogger,
                "clientA"
        );

        // Then
        assertNotNull(messages);
        assertEquals(1, messages.length, "Expected 1 Slack message for small number of groups");

        JsonNode root = OBJECT_MAPPER.readTree(messages[0]);
        assertTrue(root.has("blocks"), "Message should contain blocks");

        JsonNode blocks = root.get("blocks");
        assertEquals(3, blocks.size(), "Expected 1 table block + 1 title block + 1 divider block");
        assertEquals("table", blocks.get(1).get("type").asText());

        verify(mockLogger).info("Created {} Slack messages", 1);
    }
}
