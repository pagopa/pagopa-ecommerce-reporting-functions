package it.pagopa.ecommerce.reporting;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;

import java.time.LocalDateTime;

/**
 * Azure Functions with Azure Queue trigger.
 */
public class PagopaEcommerceReporting {

    /**
     * This function will be invoked periodically according to the specified schedule.
     */
    @FunctionName("PagopaEcommerceReportingFunction")
    public void run(
            @TimerTrigger(name = "timerInfo", schedule = "0 * * * * *") String timerInfo,
            final ExecutionContext context
    ) {
        context.getLogger().info("Java Timer trigger function executed at: " + LocalDateTime.now());
    }
}
