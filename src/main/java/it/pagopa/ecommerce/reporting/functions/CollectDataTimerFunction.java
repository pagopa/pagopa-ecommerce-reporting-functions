package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.services.WriteDataService;
import it.pagopa.ecommerce.reporting.utils.MapParametersUtils;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CollectDataTimerFunction {

    public CollectDataTimerFunction() {
    }

    @FunctionName("readAndWriteData")
    public void readAndWriteData(
                                 @TimerTrigger(
                                         name = "readAndWriteDataTrigger", schedule = "0 */5 * * * *"
                                 ) String timerInfo,
                                 ExecutionContext context
    ) {
        Logger logger = context.getLogger();
        logger.log(
                Level.CONFIG,
                () -> "[CollectDataTimerFunction][id=" + context.getInvocationId() + "] new timer " + timerInfo
        );
        ReadDataService readDataService = this.getReadDataServiceInstance();
        WriteDataService writeDataService = this.getWriteDataServiceInstance();

        List<JsonNode> transactionMetricsResponseDtoList = readDataService.readData();
        writeDataService.writeData(transactionMetricsResponseDtoList);
    }

    protected ReadDataService getReadDataServiceInstance() {
        return ReadDataService.getInstance();
    }

    protected WriteDataService getWriteDataServiceInstance() {
        return WriteDataService.getInstance();
    }

}
