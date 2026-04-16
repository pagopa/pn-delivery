package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.delivery.models.internal.notification.NotificationPhysicalAddress;
import it.pagopa.pn.delivery.models.internal.notification.NotificationRecipient;
import it.pagopa.pn.delivery.svc.validation.ErrorCodes;
import it.pagopa.pn.delivery.svc.validation.ValidationResult;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import it.pagopa.pn.delivery.svc.validation.validators.FormalValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
public class PhysicalAddressValidator implements FormalValidator<NotificationContext> {

    private final boolean physicalValidationActivated;
    private final Integer length;
    private final String pattern;

    @Override
    public ValidationResult validate(NotificationContext context) {
        int recIdx = 0;
        ArrayList<ProblemError> errors = new ArrayList<>();

        for (NotificationRecipient recipient : context.getPayload().getRecipients()) {
            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            checkPhysicalAddressIsForamllyCorrect(physicalAddress, recIdx, context, errors);
            recIdx++;
        }

        return new ValidationResult(errors);
    }

    private void checkPhysicalAddressIsForamllyCorrect(NotificationPhysicalAddress physicalAddress, int recIdx, NotificationContext context, ArrayList<ProblemError> errors) {

        if(physicalAddress == null){
            errors.add(ProblemError.builder().element("physicalAddress").code(ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_NULL.getValue()).detail("PhysicalAddress cannot be null").build());
            return;
        }

        if (physicalValidationActivated) {

            Pair<String, String> address = Pair.of("address", physicalAddress.getAddress());
            Pair<String, String> addressDetails = Pair.of("addressDetails", physicalAddress.getAddressDetails());
            Pair<String, String> province = Pair.of("province", physicalAddress.getProvince());
            Pair<String, String> foreignState = Pair.of("foreignState", physicalAddress.getForeignState());
            Pair<String, String> at = Pair.of("at", physicalAddress.getAt());
            Pair<String, String> zip = Pair.of("zip", physicalAddress.getZip());
            Pair<String, String> municipality = Pair.of("municipality", physicalAddress.getMunicipality());
            Pair<String, String> municipalityDetails = Pair.of("municipalityDetails", physicalAddress.getMunicipalityDetails());
            Pair<String, String> row2 = buildPair("at and municipalityDetails", List.of(at, municipalityDetails));
            Pair<String, String> row5 = buildPair("zip, municipality and Province", List.of(zip, municipality, province));


            Stream.of(address, addressDetails, province, foreignState, at, zip, municipality, municipalityDetails)
                    .filter(field -> field.getValue() != null &&
                            (!field.getValue().matches("[" + pattern + "]*")))
                    .map(field -> ProblemError.builder().element("physicalAddress").code(ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_INVALID_CHARACTERS.getValue()).detail(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), recIdx)).build())
                    .forEach(errors::add);

            Stream.of(row2, addressDetails, address, row5, foreignState)
                    .filter(field -> field.getValue() != null && field.getValue().trim().length() > length )
                    .map(field -> ProblemError.builder().element("physicalAddress").code(ErrorCodes.ERROR_CODE_PHYSICAL_ADDRESS_LENGTH_EXCEEDED.getValue()).detail(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), recIdx, length)).build())
                    .forEach(errors::add);

        }

    }

    private static Pair<String, String> buildPair(String name, List<Pair<String, String>> pairs){
        List<String> rowElem = new ArrayList<>();

        pairs.stream().
                filter(field -> field.getValue() != null)
                .forEach( field -> rowElem.add(field.getValue().trim()));

        return Pair.of(name, String.join(" ", rowElem));

    }
}
