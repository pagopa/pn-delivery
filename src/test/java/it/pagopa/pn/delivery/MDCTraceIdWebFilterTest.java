package it.pagopa.pn.delivery;

import it.pagopa.pn.delivery.springbootcfg.MDCTraceIdActivation;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.WebHandler;
import org.springframework.web.server.handler.DefaultWebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@Slf4j
@ExtendWith(SpringExtension.class)
@TestPropertySource(properties = { "pn.log.trace-id-header=X-Amzn-Trace-Id" })
class MDCTraceIdWebFilterTest {

    public static final String MY_HEADER = "Root=1-61b1d38b-752391d8200695e11e2e5bac;";

    @TestConfiguration
    static class SpringTestConfiguration {
        @Bean
        public MDCTraceIdActivation mdcTraceIdWebFilter(){
            return new MDCTraceIdActivation();
        };
    }

    @Autowired
    MDCTraceIdActivation mdcTraceIdWebFilter;

    @Test
    void filterTraceIdConfig() {

        MockServerHttpRequest request = MockServerHttpRequest.get("http://localhost")
                .header("X-Amzn-Trace-Id", MY_HEADER).build();
        ServerWebExchange exchange = MockServerWebExchange.from(request);

        WebHandler webHandler = new WebHandler() {
            @Override
            public Mono<Void> handle(ServerWebExchange serverWebExchange) {
                Assertions.assertEquals(MY_HEADER, MDC.get(MDCTraceIdActivation.MDC_TRACE_ID_KEY));
                return Mono.empty();
            }
        };
        WebFilterChain filterChain = new DefaultWebFilterChain(webHandler, Collections.emptyList());

        mdcTraceIdWebFilter.filter(exchange, filterChain).block();
    }

}
