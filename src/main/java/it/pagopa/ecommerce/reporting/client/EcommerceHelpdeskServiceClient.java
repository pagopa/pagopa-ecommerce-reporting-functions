package it.pagopa.ecommerce.reporting.client;

import it.pagopa.ecommerce.reporting.exceptions.InvalidRequestException;
import it.pagopa.generated.ecommerce.helpdesk.v2.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class EcommerceHelpdeskServiceClient {

    private final it.pagopa.generated.ecommerce.helpdesk.v2.api.ECommerceApi eCommerceApiClientWebClientV2;

    @Autowired
    public EcommerceHelpdeskServiceClient(
            @Qualifier(
                "eCommerceApiClientWebClientV2"
            ) it.pagopa.generated.ecommerce.helpdesk.v2.api.ECommerceApi eCommerceApiClientWebClientV2
    ) {
        this.eCommerceApiClientWebClientV2 = eCommerceApiClientWebClientV2;
    }

    public Mono<TransactionMetricsResponseDto> searchMetrics(SearchMetricsRequestDto searchMetricsRequestDto) {
        return eCommerceApiClientWebClientV2
                .ecommerceSearchMetrics(searchMetricsRequestDto)
                .doOnError(
                        WebClientResponseException.class,
                        EcommerceHelpdeskServiceClient::logWebClientException
                )
                .onErrorMap(
                        err -> new InvalidRequestException("Error while invoke method for read psp list")
                );
    }

    private static void logWebClientException(WebClientResponseException e) {
        /*
         * TODO log.info( "Got bad response from payment-methods-service [HTTP {}]: {}",
         * e.getStatusCode(), e.getResponseBodyAsString() );
         */
    }
}
