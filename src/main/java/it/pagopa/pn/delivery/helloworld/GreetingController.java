package it.pagopa.pn.delivery.helloworld;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GreetingController {
    
    @GetMapping("/hello")
    public Mono<ResponseEntity<Hello>> getHello() {
        Hello hello = new Hello();
        hello.setFirstName("P.N.");
        hello.setLastName("Registered Delivery");
        return Mono.just(ResponseEntity.ok(hello));
    }

    @PostMapping("/hello")
    public Mono<ResponseEntity<Void>> postHello(Hello hello) {
        return Mono.just(ResponseEntity.accepted().build());
    }
}


