package it.pagopa.ecommerce.reporting.exceptions;

public class JobConfigurationException extends RuntimeException {

    public JobConfigurationException(String message) {
        super(message);
    }

    public JobConfigurationException(
            String message,
            Throwable t
    ) {
        super(message, t);
    }

}
