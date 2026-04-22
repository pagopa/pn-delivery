package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.legalContext;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.notification;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.pfRecipient;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.physicalAddress;
import static org.assertj.core.api.Assertions.assertThat;

class ProvinceRequiredValidatorTest {

	@Test
	void shouldReturnErrorWhenItalianAddressHasNoProvince() {
		ProvinceRequiredValidator validator = new ProvinceRequiredValidator();
		NotificationPhysicalAddress address = physicalAddress();
		address.setProvince(null);
		address.setForeignState("ITALIA");
		NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", address, List.of());

		ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

		assertThat(result.isFailure()).isTrue();
		assertThat(result.getErrors()).hasSize(1);
		assertThat(result.getErrors().get(0).getCode()).isEqualTo(ErrorCodes.ERROR_CODE_PROVINCE_REQUIRED.getValue());
		assertThat(result.getErrors().get(0).getElement()).isEqualTo("address");
		assertThat(result.getErrors().get(0).getDetail()).contains("No province provided in physical address");
	}

	@Test
	void shouldReturnSuccessWhenItalianAddressHasProvince() {
		ProvinceRequiredValidator validator = new ProvinceRequiredValidator();
		NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", physicalAddress(), List.of());

		assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
	}

	@Test
	void shouldReturnSuccessWhenForeignAddressHasNoProvince() {
		ProvinceRequiredValidator validator = new ProvinceRequiredValidator();
		NotificationPhysicalAddress address = physicalAddress();
		address.setProvince(null);
		address.setForeignState("FRANCIA");
		NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", address, List.of());

		assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
	}

	@Test
	void shouldReturnSuccessWhenPhysicalAddressIsNull() {
		ProvinceRequiredValidator validator = new ProvinceRequiredValidator();
		NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", null, List.of());

		assertSuccess(validator.validate(legalContext(notification(List.of(recipient), List.of()))));
	}

	@Test
	void shouldReturnErrorWhenProvinceIsBlankForItalianAddress() {
		ProvinceRequiredValidator validator = new ProvinceRequiredValidator();
		NotificationPhysicalAddress address = physicalAddress();
		address.setProvince("   ");
		address.setForeignState("italy");
		NotificationRecipient recipient = pfRecipient("AAAAAA00A00A000A", "Mario Rossi", address, List.of());

		ValidationResult result = validator.validate(legalContext(notification(List.of(recipient), List.of())));

		assertThat(result.isFailure()).isTrue();
		assertThat(result.getErrors()).singleElement().satisfies(error -> {
			assertThat(error.getCode()).isEqualTo(ErrorCodes.ERROR_CODE_PROVINCE_REQUIRED.getValue());
			assertThat(error.getDetail()).contains("No province provided in physical address");
		});
	}
}
