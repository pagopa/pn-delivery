package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.configs.PnSpringBootApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.RedirectView;

@PnSpringBootApplication
public class PnDeliveryApplication {

	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryApplication.class, args);
	}


	@Controller
	@RequestMapping("/")
	public static class RootController {

		@GetMapping("/")
		public RedirectView home() {
			return new RedirectView("swagger-ui.html?configUrl=/delivery/v3/api-docs/swagger-config");
		}
	}

}
