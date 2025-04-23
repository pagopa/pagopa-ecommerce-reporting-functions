package it.pagopa.ecommerce.reporting.entity;

public class SlackMessage {
    private String type;
    private String text;

    public SlackMessage() {

    }

    public SlackMessage(
            String type,
            String text
    ) {
        this.type = type;
        this.text = text;
    }

    public String getType() {
        return this.type;
    }

    public String getText() {
        return this.text;
    }
}
