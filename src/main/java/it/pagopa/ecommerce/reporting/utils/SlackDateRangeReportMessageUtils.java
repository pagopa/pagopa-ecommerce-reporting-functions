package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Builds Slack messages using Block Kit elements for date range reports.
 *
 * @see <a href="https://api.slack.com/reference/block-kit/block-element">Slack
 *      block elements reference</a>
 */
public class SlackDateRangeReportMessageUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN);
    private static final String PAGOPA_LOGO_URL = "https://selfcare.pagopa.it/assets/logo_pagopacorp.png";
    public static final String PLAIN_TEXT = "plain_text";
    public static final String BLOCKS = "blocks";
    public static final String ELEMENTS = "elements";

    /**
     * Creates an aggregated weekly report message for Slack, split into multiple
     * messages with a maximum blocks size each.
     *
     * @param aggregatedGroups List of aggregated status groups
     * @param startDate        Report start date
     * @param endDate          Report end date
     * @param logger           Logger instance
     * @param clientId         Client ID
     * @return Array of formatted Slack messages in JSON format
     * @throws JsonProcessingException If JSON conversion fails
     */
    public static String[] createAggregatedTableWeeklyReport(
                                                             List<AggregatedStatusGroup> aggregatedGroups,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             Logger logger,
                                                             String clientId
    ) throws JsonProcessingException {

        if (aggregatedGroups == null || aggregatedGroups.isEmpty()) {
            logger.info("No aggregated groups to report");
            return new String[] {
                    createEmptyReportMessage(startDate, endDate)
            };
        }

        List<AggregatedStatusGroup> sortedGroups = sortAggregatedGroups(aggregatedGroups, clientId);

        // Build table blocks
        List<Map<String, Object>> tableBlocks = createTableBlocks(sortedGroups);

        // Split blocks into multiple messages if needed
        List<String> messages = new ArrayList<>();
        Map<String, Object> clientBlock = createTextBlock("header", PLAIN_TEXT, "Client: " + clientId, false);
        tableBlocks.add(0, clientBlock);
        tableBlocks.add(createDivider());
        List<Map<String, Object>> messageBlocks = new ArrayList<>(tableBlocks);

        Map<String, Object> message = Map.of(BLOCKS, messageBlocks);
        messages.add(OBJECT_MAPPER.writeValueAsString(message));

        logger.info("Created {} Slack messages", messages.size());
        return messages.toArray(new String[0]);
    }

    /**
     * Creates the initial block with header, image, and description for the Slack
     * report.
     *
     * @param startDate Report start date
     * @param endDate   Report end date
     * @param logger    Logger instance
     * @return Array of formatted Slack messages in JSON format
     * @throws JsonProcessingException If JSON conversion fails
     */
    public static String[] createInitialBlock(
                                              LocalDate startDate,
                                              LocalDate endDate,
                                              Logger logger
    ) throws JsonProcessingException {
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);
        List<String> messages = new ArrayList<>();
        List<Map<String, Object>> messageBlocks = new ArrayList<>();

        // Add header, image, divider to each message
        messageBlocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
        messageBlocks.add(createImageBlock());
        messageBlocks.add(createHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
        messageBlocks.add(createDivider());

        Map<String, Object> message = Map.of(BLOCKS, messageBlocks);
        messages.add(OBJECT_MAPPER.writeValueAsString(message));

        logger.info("Created {} Slack messages", messages.size());
        return messages.toArray(new String[0]);
    }

    /**
     * Creates an empty report message when no data is available.
     *
     * @param startDate Report start date
     * @param endDate   Report end date
     * @return JSON string representing empty report message
     * @throws JsonProcessingException If JSON conversion fails
     */
    private static String createEmptyReportMessage(
                                                   LocalDate startDate,
                                                   LocalDate endDate
    )
            throws JsonProcessingException {
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        List<Map<String, Object>> blocks = new ArrayList<>();
        blocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
        blocks.add(createImageBlock());
        blocks.add(
                createTextBlock(
                        "section",
                        "mrkdwn",
                        "Non ci sono dati da visualizzare per il periodo selezionato.",
                        false
                )
        );

        Map<String, Object> message = new HashMap<>();
        message.put(BLOCKS, blocks);
        return OBJECT_MAPPER.writeValueAsString(message);
    }

    /**
     * Sorts aggregated groups with "CP" (Carte) first, then alphabetically by
     * payment type code.
     *
     * @param aggregatedGroups Groups to sort
     * @param clientId         Client ID to filter by
     * @return Sorted list of groups filtered by client ID
     */
    static List<AggregatedStatusGroup> sortAggregatedGroups(
                                                            List<AggregatedStatusGroup> aggregatedGroups,
                                                            String clientId
    ) {
        List<AggregatedStatusGroup> sortedGroups = new ArrayList<>(aggregatedGroups);
        sortedGroups = sortedGroups.stream()
                .filter(group -> clientId.equals(group.getClientId()))
                .collect(Collectors.toList());

        sortedGroups.sort(
                Comparator.comparing((AggregatedStatusGroup group) -> {
                    String paymentType = group.getPaymentTypeCode();
                    // "CP" (Carte) first, then sort alphabetically
                    return "CP".equals(paymentType) ? "0" : "1" + paymentType;
                })
        );
        return sortedGroups;
    }

    /**
     * Formats a date.
     *
     * @param date Date to format
     * @return Formatted date string
     */
    static String formatDate(LocalDate date) {
        return date.format(DATE_FORMATTER);
    }

    /**
     * Creates a header block for the report with date range.
     *
     * @param startDate Formatted start date
     * @param endDate   Formatted end date
     * @return Map representing header block
     */
    static Map<String, Object> createHeaderBlock(
                                                 String startDate,
                                                 String endDate

    ) {
        return createTextBlock(
                "header",
                PLAIN_TEXT,
                SlackMessageConstants.PAGOPA_EMOJI + " Report Settimanale Transazioni " + startDate + " - " + endDate
                        + " " + SlackMessageConstants.PAGOPA_EMOJI,
                true
        );
    }

    /**
     * Creates a description block explaining the report.
     *
     * @param startDate Formatted start date
     * @param endDate   Formatted end date
     * @return Map representing description block
     */
    static Map<String, Object> createHeaderDescriptionBlock(
                                                            String startDate,
                                                            String endDate
    ) {
        return createTextBlock(
                "section",
                PLAIN_TEXT,
                "Di seguito il report suddiviso per client e metodo di pagamento per l'intervallo di tempo dal "
                        + startDate + " al " + endDate,
                true
        );
    }

    /**
     * Creates an image block with PagoPA logo.
     *
     * @return Map representing image block
     */
    static Map<String, Object> createImageBlock() {
        Map<String, Object> imageBlock = new HashMap<>();
        imageBlock.put("type", "image");
        imageBlock.put("image_url", PAGOPA_LOGO_URL);
        imageBlock.put("alt_text", "PagoPA Logo");
        return imageBlock;
    }

    /**
     * Creates a divider block for visual separation.
     *
     * @return Map representing divider block
     */
    static Map<String, Object> createDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        return divider;
    }

    /**
     * Creates a text block with specified formatting.
     *
     * @param blockType Block type (e.g., "section", "header")
     * @param textType  Text type (e.g., "plain_text", "mrkdwn")
     * @param content   Text content to display
     * @param emoji     Whether to enable emoji rendering
     * @return Map representing text block, or null if content is empty
     */
    static Map<String, Object> createTextBlock(
                                               String blockType,
                                               String textType,
                                               String content,
                                               boolean emoji
    ) {
        // Don't create blocks with empty content
        if (content == null || content.trim().isEmpty()) {
            return null;
        }

        Map<String, Object> block = new HashMap<>();
        block.put("type", blockType);

        Map<String, Object> text = new HashMap<>();
        text.put("type", textType);
        text.put("text", content);

        if (emoji) {
            text.put("emoji", true);
        }

        block.put("text", text);
        return block;
    }

    /**
     * Creates table blocks with header and data rows.
     *
     * @param groups List of aggregated status groups
     * @return List of table block maps
     */
    static List<Map<String, Object>> createTableBlocks(List<AggregatedStatusGroup> groups) {
        List<Map<String, Object>> blocks = new ArrayList<>();

        List<List<Map<String, Object>>> rows = new ArrayList<>();

        rows.add(createHeaderRow());

        for (AggregatedStatusGroup group : groups) {
            rows.add(createDataRow(group));
        }

        // Table block
        Map<String, Object> tableBlock = new HashMap<>();
        tableBlock.put("type", "table");
        tableBlock.put("rows", rows);

        blocks.add(tableBlock);

        return blocks;
    }

    /**
     * Creates table header row with column labels.
     *
     * @return List of header cell maps
     */
    private static List<Map<String, Object>> createHeaderRow() {
        List<Map<String, Object>> headerCells = new ArrayList<>();

        for (TableHeader header : TableHeader.values()) {
            Map<String, Object> cell = createBoldCell(header.getLabel());
            headerCells.add(cell);
        }

        return headerCells;
    }

    /**
     * Creates table data row with status percentages and counts.
     *
     * @param group Aggregated status group
     * @return List of data cell maps
     */
    private static List<Map<String, Object>> createDataRow(AggregatedStatusGroup group) {
        List<Map<String, Object>> cells = new ArrayList<>();

        String paymentMethod = group.getPaymentTypeCode();
        int total = group.getStatusCounts().values().stream().mapToInt(Integer::intValue).sum();

        Integer inProgressCount = group.getStatusCounts().get("IN CORSO");
        Integer toAnalyzeCount = group.getStatusCounts().get("DA ANALIZZARE");

        String ok = formatPercentCount(group.getStatusCounts().get("OK"), total);
        String ko = formatPercentCount(group.getStatusCounts().get("KO"), total);
        String abandoned = formatPercentCount(group.getStatusCounts().get("ABBANDONATO"), total);
        String inProgress = formatPercentCount(inProgressCount, total);
        String toAnalyze = formatPercentCount(toAnalyzeCount, total);

        cells.add(createTextCell(paymentMethod));
        cells.add(createTextCell(ok));
        cells.add(createTextCell(ko));
        cells.add(createTextCell(abandoned));
        cells.add(createStyledCell(inProgress, (inProgressCount != null && inProgressCount > 0) ? "warning" : ""));
        cells.add(createStyledCell(toAnalyze, (toAnalyzeCount != null && toAnalyzeCount > 0) ? "warning" : ""));

        return cells;
    }

    /**
     * Creates a bold table cell without emoji.
     *
     * @param text Cell content to display in bold
     * @return Map representing bold table cell
     */
    private static Map<String, Object> createBoldCell(String text) {
        Map<String, Object> style = new HashMap<>();
        style.put("bold", true);

        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "text");
        textObj.put("text", text);
        textObj.put("style", style);

        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(textObj);

        Map<String, Object> richTextSection = new HashMap<>();
        richTextSection.put("type", "rich_text_section");
        richTextSection.put(ELEMENTS, elements);

        List<Map<String, Object>> richTextElements = new ArrayList<>();
        richTextElements.add(richTextSection);

        Map<String, Object> cell = new HashMap<>();
        cell.put("type", "rich_text");
        cell.put(ELEMENTS, richTextElements);

        return cell;
    }

    /**
     * Creates a plain table cell without formatting.
     *
     * @param text Cell content to display
     * @return Map representing plain table cell
     */
    private static Map<String, Object> createTextCell(String text) {
        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "text");
        textObj.put("text", text);

        List<Map<String, Object>> elements = new ArrayList<>();
        elements.add(textObj);

        Map<String, Object> richTextSection = new HashMap<>();
        richTextSection.put("type", "rich_text_section");
        richTextSection.put(ELEMENTS, elements);

        List<Map<String, Object>> richTextElements = new ArrayList<>();
        richTextElements.add(richTextSection);

        Map<String, Object> cell = new HashMap<>();
        cell.put("type", "rich_text");
        cell.put(ELEMENTS, richTextElements);

        return cell;
    }

    /**
     * Creates a styled table cell with bold text and optional emoji prefix.
     *
     * @param text      The cell content to display
     * @param emojiName Optional emoji name to display before text (empty string for
     *                  no emoji)
     * @return Map representing a Slack rich text cell with bold formatting
     */
    private static Map<String, Object> createStyledCell(
                                                        String text,
                                                        String emojiName
    ) {
        Map<String, Object> style = new HashMap<>();
        style.put("bold", true);

        List<Map<String, Object>> elements = new ArrayList<>();

        // add warning emoji for "IN CORSO" and "DA ANALIZZARE" columns
        if (emojiName != null && !emojiName.isEmpty()) {
            Map<String, Object> emojiObj = new HashMap<>();
            emojiObj.put("type", "emoji");
            emojiObj.put("name", emojiName);
            elements.add(emojiObj);
        }

        Map<String, Object> textObj = new HashMap<>();
        textObj.put("type", "text");
        textObj.put("text", emojiName != null && !emojiName.isEmpty() ? " " + text : text);
        textObj.put("style", style);
        elements.add(textObj);

        Map<String, Object> richTextSection = new HashMap<>();
        richTextSection.put("type", "rich_text_section");
        richTextSection.put(ELEMENTS, elements);

        List<Map<String, Object>> richTextElements = new ArrayList<>();
        richTextElements.add(richTextSection);

        Map<String, Object> cell = new HashMap<>();
        cell.put("type", "rich_text");
        cell.put(ELEMENTS, richTextElements);

        return cell;
    }

    /**
     * Formats percentage and count with Italian locale (3 decimal places).
     *
     * @param count Status count
     * @param total Total count
     * @return Formatted string like "12,345% (123)" or "0,000% (0)" if null/zero
     */
    private static String formatPercentCount(
                                             Integer count,
                                             int total
    ) {
        if (count == null || total == 0)
            return "0,000% (0)";
        double percent = (count * 100.0) / total;
        return String.format(Locale.ITALIAN, "%.3f%% (%d)", percent, count);
    }
}
