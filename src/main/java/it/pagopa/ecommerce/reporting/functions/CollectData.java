package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.TimerTrigger;
import io.vavr.control.Either;
import it.pagopa.ecommerce.reporting.client.EcommerceHelpdeskServiceClient;
import it.pagopa.ecommerce.reporting.exceptions.JobConfigurationException;
import it.pagopa.ecommerce.reporting.services.CommunicationService;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.services.WriteDataService;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.SearchMetricsRequestDto;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.SearchMetricsRequestTimeRangeDto;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.TransactionMetricsResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.Function;

@Component
public class CollectData {

    private ReadDataService readDataService;
    private WriteDataService writeDataService;

    @Autowired
    public CollectData(
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
        List<TransactionMetricsResponseDto> transactionMetricsResponseDtoList = readDataService.readData();
        writeDataService.writeData(transactionMetricsResponseDtoList);
    }

}
