package it.pagopa.pn.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"it.pagopa.pn.commons.cassandra"})
public class PnDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryApplication.class, args);
	}

}
