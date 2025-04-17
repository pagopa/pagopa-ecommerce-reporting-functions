package it.pagopa.ecommerce.reporting;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.scheduledOperations.ReadDataJob;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class PagopaEcommerceReporting {
}
