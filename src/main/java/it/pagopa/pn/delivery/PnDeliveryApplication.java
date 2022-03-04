package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.configs.PnSpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@PnSpringBootApplication
public class PnDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryApplication.class, args);
	}

	@RestController
	public static class HomeController {

		@GetMapping("")
		public String home() {
			return "Sono Vivo";
		}
	}

}
