package it.pagopa.ecommerce.reporting.services;

import it.pagopa.generated.ecommerce.helpdesk.v2.dto.TransactionMetricsResponseDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WriteDataService {

    @Autowired
    public WriteDataService() {
    }

    public void writeData(List<TransactionMetricsResponseDto> metricsResponseDtos) {
    }
}
