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
     * @return Sorted list of groups
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
     * Formats status details with proper translations and emojis.
     *
     * @param statusCounts Map of status counts
     * @return Formatted status details string
     */
    static String formatStatusDetails(Map<String, Integer> statusCounts) {
        if (statusCounts == null || statusCounts.isEmpty()) {
            return "";
        }

        StringBuilder statusDetails = new StringBuilder();
        List<String> sortedKeys = new ArrayList<>(statusCounts.keySet());
        Collections.sort(sortedKeys);

        for (String statusKey : sortedKeys) {
            Integer count = statusCounts.get(statusKey);
            if (count == null || count == 0) {
                continue; // Skip zero counts
            }

            SlackMessageConstants.TranslationEntry entry = SlackMessageConstants.STATUS_TRANSLATIONS.getOrDefault(
                    statusKey,
                    new SlackMessageConstants.TranslationEntry(statusKey, SlackMessageConstants.DEFAULT_STATUS_EMOJI)
            );

            statusDetails.append("   ")
                    .append(entry.emoji())
                    .append(" *")
                    .append(entry.translation())
                    .append("*: ")
                    .append(count)
                    .append("\n\n");
        }

        return statusDetails.toString();
    }

    /**
     * Formats payment type code with description and emoji.
     *
     * @param paymentTypeCode Payment type code
     * @return Formatted payment type string
     */
    static String formatPaymentTypeCode(String paymentTypeCode) {
        if (paymentTypeCode == null || paymentTypeCode.isEmpty()) {
            paymentTypeCode = "GENERIC";
        }

        SlackMessageConstants.TranslationEntry entry = SlackMessageConstants.PAYMENT_TYPE_CODE.getOrDefault(
                paymentTypeCode,
                new SlackMessageConstants.TranslationEntry(
                        paymentTypeCode,
                        SlackMessageConstants.PAYMENT_TYPE_CODE.get("GENERIC").emoji()
                )
        );

        return "   " + entry.emoji() + " *" + entry.translation() + "*";
    }

    // Block creation methods
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

    static Map<String, Object> createHeaderDescriptionBlock(
                                                            String startDate,
                                                            String endDate
    ) {
        return createTextBlock(
                "section",
                PLAIN_TEXT,
                "Di seguito il report suddiviso per Client, PSP e metodo pagamento di pagamento per l'intervallo di tempo dal "
                        + startDate + " al " + endDate,
                true
        );
    }

    static Map<String, Object> createImageBlock() {
        Map<String, Object> imageBlock = new HashMap<>();
        imageBlock.put("type", "image");
        imageBlock.put("image_url", PAGOPA_LOGO_URL);
        imageBlock.put("alt_text", "PagoPA Logo");
        return imageBlock;
    }

    static Map<String, Object> createDivider() {
        Map<String, Object> divider = new HashMap<>();
        divider.put("type", "divider");
        return divider;
    }

    static Map<String, Object> createGroupHeaderSection(AggregatedStatusGroup group) {
        String clientId = group.getClientId() != null ? group.getClientId() : "Unknown";
        String pspId = group.getPspId() != null ? group.getPspId() : "Unknown";
        String paymentTypeCode = group.getPaymentTypeCode();

        String text = SlackMessageConstants.CHART_EMOJI + " STATISTICHE" +
                "\n\t\t| Client *" + clientId +
                "* con PSP *" + pspId +
                "* e pagato con " + formatPaymentTypeCode(paymentTypeCode);

        return createTextBlock("section", "mrkdwn", text, false);
    }

    static Map<String, Object> createStatusDetailsSection(AggregatedStatusGroup group) {
        String details = formatStatusDetails(group.getStatusCounts());
        if (details.trim().isEmpty()) {
            details = "Nessun dettaglio disponibile.";
        }

        return createTextBlock(
                "section",
                "mrkdwn",
                details,
                false
        );
    }

    // Helper method to create text blocks
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

    private static List<Map<String, Object>> createHeaderRow() {
        List<Map<String, Object>> headerCells = new ArrayList<>();

        for (TableHeader header : TableHeader.values()) {
            Map<String, Object> cell = createBoldCell(header.getLabel());
            headerCells.add(cell);
        }

        return headerCells;
    }

    private static List<Map<String, Object>> createDataRow(AggregatedStatusGroup group) {
        List<Map<String, Object>> cells = new ArrayList<>();

        String metodo = group.getPaymentTypeCode();
        int total = group.getStatusCounts().values().stream().mapToInt(Integer::intValue).sum();

        String ok = formatPercentCount(group.getStatusCounts().get("OK"), total);
        String ko = formatPercentCount(group.getStatusCounts().get("KO"), total);
        String abbandonato = formatPercentCount(group.getStatusCounts().get("ABBANDONATO"), total);
        String inCorso = formatPercentCount(group.getStatusCounts().get("IN CORSO"), total);
        String daAnalizzare = formatPercentCount(group.getStatusCounts().get("DA ANALIZZARE"), total);

        cells.add(createTextCell(metodo));
        cells.add(createTextCell(ok));
        cells.add(createTextCell(ko));
        cells.add(createTextCell(abbandonato));
        cells.add(createTextCell(inCorso));
        cells.add(createTextCell(daAnalizzare));

        return cells;
    }

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
