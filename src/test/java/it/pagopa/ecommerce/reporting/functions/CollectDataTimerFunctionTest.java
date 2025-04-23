package it.pagopa.ecommerce.reporting.functions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.microsoft.azure.functions.ExecutionContext;
import it.pagopa.ecommerce.reporting.services.ReadDataService;
import it.pagopa.ecommerce.reporting.services.WriteDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CollectDataTimerFunctionTest {

    @Spy
    CollectDataTimerFunction collectDataTimerFunction;

    @Mock
    ExecutionContext context;

    @Mock
    ReadDataService readDataService;

    @Mock
    WriteDataService writeDataService;
    /*
     * @Test void readAndWrite() { // general var Logger logger =
     * Logger.getLogger("testlogging"); List<JsonNode> resList = List.of(new
     * TextNode("mockedValue")); // precondition
     * doReturn(readDataService).when(collectDataTimerFunction).
     * getReadDataServiceInstance(logger);
     * doReturn(writeDataService).when(collectDataTimerFunction).
     * getWriteDataServiceInstance();
     * doReturn(resList).when(readDataService).readData();
     * when(context.getLogger()).thenReturn(logger);
     * when(readDataService.readData()).thenReturn(new ArrayList<>()); // test
     * execution collectDataTimerFunction.readAndWriteData("timerInfo", context);
     *
     * verify(readDataService, times(1)).readData(); verify(writeDataService,
     * times(1)).writeData(new ArrayList<>()); }
     */
}

