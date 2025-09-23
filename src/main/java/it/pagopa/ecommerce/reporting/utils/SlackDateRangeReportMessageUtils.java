package it.pagopa.ecommerce.reporting.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Logger;

/**
 * Builds Slack messages using Block Kit elements for date range reports.
 *
 * @see <a href="https://api.slack.com/reference/block-kit/block-element">Slack
 *      block elements reference</a>
 */
public class SlackDateRangeReportMessageUtils {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ITALIAN);
    private static final String PAGOPA_LOGO_URL = "https://developer.pagopa.it/gitbook/docs/8phwN5u2QXllSKsqBjQU/.gitbook/assets/logo_asset.png";
    private static final int MAX_BLOCKS_PER_MESSAGE = 40; // Reduced from 49 for safety

    /**
     * Creates an aggregated weekly report message for Slack, split into multiple
     * messages with a maximum blocks size each.
     *
     * @param aggregatedGroups List of aggregated status groups
     * @param startDate        Report start date
     * @param endDate          Report end date
     * @param logger           Logger instance
     * @return Array of formatted Slack messages in JSON format
     * @throws JsonProcessingException If JSON conversion fails
     */
    public static String[] createAggregatedWeeklyReport(
                                                        List<AggregatedStatusGroup> aggregatedGroups,
                                                        LocalDate startDate,
                                                        LocalDate endDate,
                                                        Logger logger
    ) throws JsonProcessingException {
        if (aggregatedGroups == null || aggregatedGroups.isEmpty()) {
            logger.info("No aggregated groups to report");
            return new String[] {
                    createEmptyReportMessage(startDate, endDate)
            };
        }

        List<AggregatedStatusGroup> sortedGroups = sortAggregatedGroups(aggregatedGroups);
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        // First message with header
        List<Map<String, Object>> firstMessageBlocks = new ArrayList<>();
        firstMessageBlocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
        firstMessageBlocks.add(createImageBlock());
        firstMessageBlocks.add(createHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
        firstMessageBlocks.add(createDivider());

        // Add some groups to first message if space allows
        int groupsInFirstMessage = addGroupsToBlocks(
                sortedGroups,
                0,
                MAX_BLOCKS_PER_MESSAGE - firstMessageBlocks.size(),
                firstMessageBlocks
        );

        // Create first message
        List<String> messages = new ArrayList<>();
        Map<String, Object> firstMessage = new HashMap<>();
        firstMessage.put("blocks", firstMessageBlocks);
        messages.add(OBJECT_MAPPER.writeValueAsString(firstMessage));

        // Create additional messages for remaining groups
        int remainingGroups = sortedGroups.size() - groupsInFirstMessage;
        if (remainingGroups > 0) {
            int currentGroupIndex = groupsInFirstMessage;

            while (currentGroupIndex < sortedGroups.size()) {
                List<Map<String, Object>> messageBlocks = new ArrayList<>();

                // Add groups to this message
                int groupsAdded = addGroupsToBlocks(
                        sortedGroups,
                        currentGroupIndex,
                        MAX_BLOCKS_PER_MESSAGE,
                        messageBlocks
                );

                // Create message
                Map<String, Object> message = new HashMap<>();
                if (!messageBlocks.isEmpty()) {
                    message.put("blocks", messageBlocks);
                    messages.add(OBJECT_MAPPER.writeValueAsString(message));
                }

                currentGroupIndex += groupsAdded;
            }
        }

        logger.info("Created " + messages.size() + " slack messages");
        return messages.toArray(new String[0]);
    }

    public static String[] createAggregatedTableWeeklyReport(
                                                             List<AggregatedStatusGroup> aggregatedGroups,
                                                             LocalDate startDate,
                                                             LocalDate endDate,
                                                             Logger logger
    ) throws JsonProcessingException {

        if (aggregatedGroups == null || aggregatedGroups.isEmpty()) {
            logger.info("No aggregated groups to report");
            return new String[] {
                    createEmptyReportMessage(startDate, endDate)
            };
        }

        List<AggregatedStatusGroup> sortedGroups = sortAggregatedGroups(aggregatedGroups);
        String formattedStartDate = formatDate(startDate);
        String formattedEndDate = formatDate(endDate);

        // Build table blocks
        List<Map<String, Object>> tableBlocks = createTableBlocks(sortedGroups);

        // Split blocks into multiple messages if needed
        List<String> messages = new ArrayList<>();
        int start = 0;
        while (start < tableBlocks.size()) {
            List<Map<String, Object>> messageBlocks = new ArrayList<>();

            // Add header, image, divider to each message
            messageBlocks.add(createHeaderBlock(formattedStartDate, formattedEndDate));
            messageBlocks.add(createImageBlock());
            messageBlocks.add(createHeaderDescriptionBlock(formattedStartDate, formattedEndDate));
            messageBlocks.add(createDivider());

            // Determine how many table blocks can fit in this message
            int remainingSpace = MAX_BLOCKS_PER_MESSAGE - messageBlocks.size();
            int end = Math.min(start + remainingSpace, tableBlocks.size());

            messageBlocks.addAll(tableBlocks.subList(start, end));

            Map<String, Object> message = Map.of("blocks", messageBlocks);
            messages.add(OBJECT_MAPPER.writeValueAsString(message));

            start = end;
        }

        logger.info("Created " + messages.size() + " Slack messages");
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
        message.put("blocks", blocks);
        return OBJECT_MAPPER.writeValueAsString(message);
    }

    /**
     * Adds groups to a block list and returns the number of groups added.
     */
    private static int addGroupsToBlocks(
                                         List<AggregatedStatusGroup> groups,
                                         int startIndex,
                                         int maxBlocksAvailable,
                                         List<Map<String, Object>> blocks
    ) {

        int blocksPerGroup = 3; // Header + Details + Divider
        int maxGroupsToAdd = maxBlocksAvailable / blocksPerGroup;
        int groupsToAdd = Math.min(maxGroupsToAdd, groups.size() - startIndex);

        for (int i = 0; i < groupsToAdd; i++) {
            AggregatedStatusGroup group = groups.get(startIndex + i);

            // Only add groups that have status counts
            if (!group.getStatusCounts().isEmpty()) {
                blocks.add(createGroupHeaderSection(group));

                // Only add status details if there are any
                String statusDetails = formatStatusDetails(group.getStatusCounts());
                if (!statusDetails.trim().isEmpty()) {
                    blocks.add(createStatusDetailsSection(group));
                }

                blocks.add(createDivider());
            }
        }

        return groupsToAdd;
    }

    /**
     * Sorts aggregated groups by ACTIVATED count in descending order.
     *
     * @param aggregatedGroups Groups to sort
     * @return Sorted list of groups
     */
    static List<AggregatedStatusGroup> sortAggregatedGroups(List<AggregatedStatusGroup> aggregatedGroups) {
        List<AggregatedStatusGroup> sortedGroups = new ArrayList<>(aggregatedGroups);
        sortedGroups.sort(
                Comparator.comparing(
                        group -> group.getStatusCounts().getOrDefault("ACTIVATED", 0),
                        Comparator.reverseOrder()
                )
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
                "plain_text",
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
                "plain_text",
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

        StringBuilder sb = new StringBuilder();
        int currentBlockCount = 0;

        // Header row for table
        String header = "*METODO*    *OK*       *KO*      *ABBANDONATO*   *IN CORSO*   *DA ANALIZZARE*\n";

        for (AggregatedStatusGroup group : groups) {
            if (currentBlockCount >= 35) {
                // 35 to leave space for header/divider/other blocks in message
                // Flush current block
                Map<String, Object> block = Map.of(
                        "type",
                        "section",
                        "text",
                        Map.of("type", "mrkdwn", "text", "```" + sb.toString() + "```")
                );
                blocks.add(block);

                sb = new StringBuilder();
                currentBlockCount = 0;
            }

            if (sb.length() == 0) {
                sb.append(header); // Add header at top of each new block
            }

            String metodo = group.getPaymentTypeCode();
            int total = group.getStatusCounts().values().stream().mapToInt(Integer::intValue).sum();

            String ok = formatPercentCount(group.getStatusCounts().get("OK"), total);
            String ko = formatPercentCount(group.getStatusCounts().get("KO"), total);
            String abbandonato = formatPercentCount(group.getStatusCounts().get("ABBANDONATO"), total);
            String inCorso = formatPercentCount(group.getStatusCounts().get("IN CORSO"), total);
            String daAnalizzare = formatPercentCount(group.getStatusCounts().get("DA ANALIZZARE"), total);

            sb.append(
                    String.format(
                            "%-10s %-10s %-10s %-14s %-10s %-14s\n",
                            metodo,
                            ok,
                            ko,
                            abbandonato,
                            inCorso,
                            daAnalizzare
                    )
            );

            currentBlockCount++;
        }

        // Add remaining content as block
        if (sb.length() > 0) {
            Map<String, Object> block = Map.of(
                    "type",
                    "section",
                    "text",
                    Map.of("type", "mrkdwn", "text", "```" + sb.toString() + "```")
            );
            blocks.add(block);
        }

        return blocks;
    }

    private static String formatPercentCount(
                                             Integer count,
                                             int total
    ) {
        if (count == null || total == 0)
            return "0% (0)";
        int percent = (int) Math.round((count * 100.0) / total);
        return percent + "% (" + count + ")";
    }
}
