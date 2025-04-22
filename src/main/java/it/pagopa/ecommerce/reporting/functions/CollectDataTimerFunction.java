package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.services.WriteDataService;

import java.util.*;

public class CollectDataTimerFunction {

    private ReadDataService readDataService;
    private WriteDataService writeDataService;

    public CollectDataTimerFunction(
            ReadDataService readDataService,
            WriteDataService writeDataService
    ) {
        this.readDataService = readDataService;
    }

    @FunctionName("readAndWriteData")
    public void readAndWriteData(
                                 @TimerTrigger(
                                         name = "readAndWriteDataTrigger", schedule = "0 */5 * * * *"
                                 ) String timerInfo,
                                 ExecutionContext context
    ) {
        List<JsonNode> transactionMetricsResponseDtoList = readDataService.readData();
        writeDataService.writeData(transactionMetricsResponseDtoList);
    }

}
