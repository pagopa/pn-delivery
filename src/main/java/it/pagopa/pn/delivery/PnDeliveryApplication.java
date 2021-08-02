package it.pagopa.pn.delivery;

import it.pagopa.pn.commons.cassandra.config.CassandraProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.view.RedirectView;

@EnableConfigurationProperties(CassandraProperties.class)
@SpringBootApplication(scanBasePackages = {PnDeliveryApplication.COMMONS_BASE_PACKAGE, PnDeliveryApplication.DELIVERY_BASE_PACKAGE})
public class PnDeliveryApplication {

	public static final String DELIVERY_BASE_PACKAGE = "it.pagopa.pn.delivery";
	public static final String COMMONS_BASE_PACKAGE = "it.pagopa.pn.commons";


	public static void main(String[] args) {
		SpringApplication.run(PnDeliveryApplication.class, args);
	}


	@Controller
	@RequestMapping("/")
	public static class RootController {

		@GetMapping("/")
		public RedirectView home() {
			return new RedirectView("swagger-ui.html");
		}
	}

}
