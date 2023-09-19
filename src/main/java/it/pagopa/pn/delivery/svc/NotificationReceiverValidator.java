package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
@Component
public class NotificationReceiverValidator {

    private final Validator validator;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final ValidateUtils validateUtils;
    private final PnDeliveryConfigs pnDeliveryConfigs;

    public NotificationReceiverValidator(Validator validator, MVPParameterConsumer mvpParameterConsumer, ValidateUtils validateUtils, PnDeliveryConfigs pnDeliveryConfigs) {
        this.validator = validator;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.validateUtils = validateUtils;
        this.pnDeliveryConfigs = pnDeliveryConfigs;

        log.info("Validation enabled={}", pnDeliveryConfigs.isPhysicalAddressValidation());
        log.info("Validation pattern={}", pnDeliveryConfigs.getPhysicalAddressValidationPattern());
        log.info("Validation length={}", pnDeliveryConfigs.getPhysicalAddressValidationLength());
    }

    protected void checkNewNotificationBeforeInsertAndThrow(InternalNotification internalNotification) {
        Set<ConstraintViolation<InternalNotification>> errors = checkNewNotificationBeforeInsert(internalNotification);
        if( ! errors.isEmpty() ) {
            List<ProblemError> errorList  = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(internalNotification.getPaProtocolNumber(), errorList);
        }
    }

    protected Set<ConstraintViolation<InternalNotification>> checkNewNotificationBeforeInsert(InternalNotification internalNotification) {
        return validator.validate(internalNotification);
    }


    public void checkNewNotificationRequestBeforeInsertAndThrow(NewNotificationRequestV21 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV21>> errors = checkNewNotificationRequestBeforeInsert(newNotificationRequestV2);
        if (Boolean.TRUE.equals(mvpParameterConsumer.isMvp(newNotificationRequestV2.getSenderTaxId())) && errors.isEmpty()) {
            errors = checkNewNotificationRequestForMVP(newNotificationRequestV2);
        }
        if (!errors.isEmpty()) {
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(newNotificationRequestV2.getPaProtocolNumber(), errorList);
        }
    }

    protected Set<ConstraintViolation<NewNotificationRequestV21>> checkNewNotificationRequestBeforeInsert(NewNotificationRequestV21 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV21>> errors = new HashSet<>();

        // check del numero massimo di documenti allegati
        if (pnDeliveryConfigs.getMaxAttachmentsCount() > 0 && newNotificationRequestV2.getDocuments().size() > pnDeliveryConfigs.getMaxAttachmentsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Max attachment count reached");
            errors.add(constraintViolation);
            return errors;
        }

        // check del numero massimo di recipient
        if (pnDeliveryConfigs.getMaxRecipientsCount() > 0 && newNotificationRequestV2.getRecipients().size() > pnDeliveryConfigs.getMaxRecipientsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Max recipient count reached");
            errors.add(constraintViolation);
            return errors;
        }

        int recIdx = 0;
        Set<String> distinctTaxIds = new HashSet<>();
        for (NotificationRecipientV21 recipient : newNotificationRequestV2.getRecipients()) {

            // limitazione temporanea: destinatari PG possono avere solo TaxId numerico
            onlyNumericalTaxIdForPGV2(errors, recIdx, recipient);

          if( !validateUtils.validate(recipient.getTaxId())) {
              ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>( "Invalid taxId for recipient " + recIdx );
              errors.add(constraintViolation);
          }
          if ( !distinctTaxIds.add( recipient.getTaxId() )){
              ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>( "Duplicated recipient taxId" );
              errors.add(constraintViolation);
          }
          if(recipient.getPayment() != null){
              String noticeCode = recipient.getPayment().getNoticeCode();
              String noticeCodeAlternative = recipient.getPayment().getNoticeCodeAlternative();
              if ( noticeCode.equals(noticeCodeAlternative) ) {
                  ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>( "Alternative notice code equals to notice code" );
                  errors.add(constraintViolation);
              }
          }
          NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
          checkProvince(errors, physicalAddress);
          recIdx++;
      }
      errors.addAll(validator.validate(internalNotification));
      errors.addAll( this.checkPhysicalAddress(internalNotification));
      return errors;
    }

    protected Set<ConstraintViolation<NewNotificationRequest>> checkPhysicalAddress(NewNotificationRequest internalNotification) {

        Set<ConstraintViolation<NewNotificationRequest>> errors = new HashSet<>();

        if (this.pnDeliveryConfigs.isPhysicalAddressValidation()) {

            int recIdx = 0;

            for (NotificationRecipient recipient : internalNotification.getRecipients()) {

                NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();

                Pair<String, String> denomination = Pair.of("denomination", recipient.getDenomination());
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

                int finalRecIdx = recIdx;
                Stream.of(denomination, address, addressDetails, province, foreignState, at, zip, municipality, municipalityDetails)
                        .filter(field -> field.getValue() != null &&
                                (!field.getValue().matches("[" + this.pnDeliveryConfigs.getPhysicalAddressValidationPattern() + "]*")))
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequest>(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), finalRecIdx)))
                        .forEach(errors::add);

                Stream.of(denomination, row2, addressDetails, address, row5, foreignState)
                        .filter(field -> field.getValue() != null && field.getValue().trim().length() > this.pnDeliveryConfigs.getPhysicalAddressValidationLength() )
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequest>(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), finalRecIdx, this.pnDeliveryConfigs.getPhysicalAddressValidationLength())))
                        .forEach(errors::add);

                recIdx++;
            }
        }

        return errors;

    }

    private static Pair<String, String> buildPair(String name, List<Pair<String, String>> pairs){
        List<String> rowElem = new ArrayList<>();

        pairs.stream().
                filter(field -> field.getValue() != null)
                .forEach( field -> rowElem.add(field.getValue().trim()));

        return Pair.of(name, String.join(" ", rowElem));

    }


    private static void onlyNumericalTaxIdForPGV2(Set<ConstraintViolation<NewNotificationRequestV21>> errors, int recIdx, NotificationRecipientV21 recipient) {
        if (NotificationRecipientV21.RecipientTypeEnum.PG.equals(recipient.getRecipientType()) &&
                (!recipient.getTaxId().matches("^\\d+$"))) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("SEND accepts only numerical taxId for PG recipient " + recIdx);
            errors.add(constraintViolation);
        }
    }


    private static void checkProvinceV2(Set<ConstraintViolation<NewNotificationRequestV21>> errors, NotificationPhysicalAddress physicalAddress) {
        if (Objects.nonNull(physicalAddress) &&
                (!StringUtils.hasText(physicalAddress.getForeignState()) || physicalAddress.getForeignState().toUpperCase().trim().startsWith("ITAL")) &&
                !StringUtils.hasText(physicalAddress.getProvince())) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("No province provided in physical address");
            errors.add(constraintViolation);
        }
    }


    public Set<ConstraintViolation<NewNotificationRequestV21>> checkNewNotificationRequestForMVP(NewNotificationRequestV21 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV21>> errors = new HashSet<>();

        if (newNotificationRequestV2.getRecipients().size() > 1) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Max one recipient");
            errors.add(constraintViolation);
        }

        List<NotificationPaymentItem> payment = newNotificationRequestV2.getRecipients().get(0).getPayments();
        if (Objects.isNull(payment) || payment.isEmpty()) {
            ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("No recipient payment");
            errors.add(constraintViolation);
        }
        return errors;
    }
}