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
import it.pagopa.pn.delivery.utils.DenominationValidationUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.REGEX;
import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.NONE;

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


    public void checkNewNotificationRequestBeforeInsertAndThrow(NewNotificationRequestV23 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV23>> errors = checkNewNotificationRequestBeforeInsert(newNotificationRequestV2);
        if (Boolean.TRUE.equals(mvpParameterConsumer.isMvp(newNotificationRequestV2.getSenderTaxId())) && errors.isEmpty()) {
            errors = checkNewNotificationRequestForMVP(newNotificationRequestV2);
        }
        if (!errors.isEmpty()) {
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(newNotificationRequestV2.getPaProtocolNumber(), errorList);
        }
    }

    protected Set<ConstraintViolation<NewNotificationRequestV23>> checkNewNotificationRequestBeforeInsert(NewNotificationRequestV23 newNotificationRequestV23) {
        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();

        // check del numero massimo di documenti allegati
        if (pnDeliveryConfigs.getMaxAttachmentsCount() > 0 && newNotificationRequestV23.getDocuments().size() > pnDeliveryConfigs.getMaxAttachmentsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("Max attachment count reached");
            errors.add(constraintViolation);
            return errors;
        }

        // check del numero massimo di recipient
        if (pnDeliveryConfigs.getMaxRecipientsCount() > 0 && newNotificationRequestV23.getRecipients().size() > pnDeliveryConfigs.getMaxRecipientsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("Max recipient count reached");
            errors.add(constraintViolation);
            return errors;
        }

        // check campi paFee e vat per notifiche DELIVERY_MODE
        if ( newNotificationRequestV23.getNotificationFeePolicy().equals(NotificationFeePolicy.DELIVERY_MODE) &&
                (Objects.isNull(newNotificationRequestV23.getPaFee()) || Objects.isNull(newNotificationRequestV23.getVat()))) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("paFee or vat field not filled in for notification with notificationFeePolicy DELIVERY_MODE");
            errors.add( constraintViolation );
        }


        int recIdx = 0;
        Set<String> distinctTaxIds = new HashSet<>();
        Set<String> distinctIuvs = new HashSet<>();
        for (NotificationRecipientV23 recipient : newNotificationRequestV23.getRecipients()) {

            // limitazione temporanea: destinatari PG possono avere solo TaxId numerico
            onlyNumericalTaxIdForPGV2(errors, recIdx, recipient);
            boolean isPF = NotificationRecipientV23.RecipientTypeEnum.PF.getValue().equals(recipient.getRecipientType().getValue());

            if( !validateUtils.validate(recipient.getTaxId(), isPF, false)) {
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>( "Invalid taxId for recipient " + recIdx );
                errors.add(constraintViolation);
            }
            if ( !distinctTaxIds.add( recipient.getTaxId() )){
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>( "Duplicated recipient taxId" );
                errors.add(constraintViolation);
            }

            boolean isNotificationFeePolicyDeliveryMode = newNotificationRequestV23.getNotificationFeePolicy().equals(NotificationFeePolicy.DELIVERY_MODE);
            if(recipient.getPayments() != null) {
                errors.addAll(checkApplyCost(isNotificationFeePolicyDeliveryMode, recipient.getPayments()));
                errors.addAll(checkIuvs(recipient.getPayments(), distinctIuvs, recIdx));
            }

            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            checkProvinceV2(errors, physicalAddress);
            recIdx++;
        }

        if (!hasDistinctAttachments(newNotificationRequestV23)) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("Same attachment compares more then once in the same request");
            errors.add(constraintViolation);
        }

        errors.addAll(validator.validate( newNotificationRequestV23 ));
        errors.addAll( this.checkPhysicalAddress( newNotificationRequestV23 ));
        errors.addAll(this.checkDenomination( newNotificationRequestV23 ));
        return errors;
    }

    /**
     * Validazio di NewNotificationRequestV23 per verificare l'assenza di duplicati tra gli allegati
     * @param newNotificationRequestV23
     * @return
     */
    protected boolean hasDistinctAttachments(NewNotificationRequestV23 newNotificationRequestV23){
        Set<String> uniqueIds = new HashSet<>();

        for (NotificationDocument doc : emptyIfNull(newNotificationRequestV23.getDocuments())) {
            if (doc.getRef() != null && doc.getDigests() != null) {
                String id = doc.getRef().getKey() + doc.getDigests().getSha256();
                if (!uniqueIds.add(id)) {
                    return false;
                }
            }
        }

        long duplicates = emptyIfNull(newNotificationRequestV23.getRecipients())
            .stream()
            .map(recipient -> hasRecipientDistinctAttachments(recipient, uniqueIds))
            .filter(res -> !res).count();

        return duplicates==0;
    }

    private boolean hasRecipientDistinctAttachments(NotificationRecipientV23 recipient, Set<String> docIds){
        Set<String> recipientAttachmentIds = new HashSet<>();
        recipientAttachmentIds.addAll(docIds);

        long duplicatedAttachments = emptyIfNull(recipient.getPayments()).stream()
            .filter( payment -> payment.getPagoPa() != null && payment.getPagoPa().getAttachment() != null)
            .map(payment ->{
                NotificationPaymentAttachment att = payment.getPagoPa().getAttachment();
                if (att.getRef() != null && att.getDigests() != null) {
                    String id = att.getRef().getKey() + att.getDigests().getSha256();

                    if (!recipientAttachmentIds.add(id)) {
                        return false;
                    }
                }
                return true;
            }).filter( uniqueAttachment -> !uniqueAttachment)
            .count();

        return duplicatedAttachments == 0;
    }

    public static <T> List<T> emptyIfNull(List<T> list) {
        return list == null ? Collections.<T>emptyList() : list;
    }

    protected Set<ConstraintViolation<NewNotificationRequestV23>> checkPhysicalAddress(NewNotificationRequestV23 internalNotification) {

        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();

        if (this.pnDeliveryConfigs.isPhysicalAddressValidation()) {

            int recIdx = 0;

            for (NotificationRecipientV23 recipient : internalNotification.getRecipients()) {
                NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();

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
                Stream.of(address, addressDetails, province, foreignState, at, zip, municipality, municipalityDetails)
                        .filter(field -> field.getValue() != null &&
                                (!field.getValue().matches("[" + this.pnDeliveryConfigs.getPhysicalAddressValidationPattern() + "]*")))
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequestV23>(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), finalRecIdx)))
                        .forEach(errors::add);

                Stream.of(row2, addressDetails, address, row5, foreignState)
                        .filter(field -> field.getValue() != null && field.getValue().trim().length() > this.pnDeliveryConfigs.getPhysicalAddressValidationLength() )
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequestV23>(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), finalRecIdx, this.pnDeliveryConfigs.getPhysicalAddressValidationLength())))
                        .forEach(errors::add);

                recIdx++;
            }
        }

        return errors;

    }

    protected Set<ConstraintViolation<NewNotificationRequestV23>> checkDenomination(NewNotificationRequestV23 internalNotification) {

        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();

        int recIdx = 0;

        for (NotificationRecipientV23 recipient : internalNotification.getRecipients()) {

            Pair<String, String> denomination = Pair.of("denomination", recipient.getDenomination());

            int finalRecIdx = recIdx;
            if(this.pnDeliveryConfigs.getDenominationLength() != null && this.pnDeliveryConfigs.getDenominationLength() != 0){
                Stream.of(denomination)
                        .filter(field -> field.getValue() != null && field.getValue().trim().length() > this.pnDeliveryConfigs.getDenominationLength() )
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequestV23>(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), finalRecIdx, this.pnDeliveryConfigs.getDenominationLength())))
                        .forEach(errors::add);
            }

            if(this.pnDeliveryConfigs.getDenominationValidationTypeValue() != null && !this.pnDeliveryConfigs.getDenominationValidationTypeValue().equalsIgnoreCase(NONE.name())){
                String denominationValidationType = this.pnDeliveryConfigs.getDenominationValidationTypeValue().toLowerCase();

                ValidationRegex validationRegex = initializeValidationRegex(denominationValidationType);

                String regex = validationRegex.regex;
                String excludeCharacterRegex = validationRegex.excludedCharacterRegex;

                log.info("Check denomination with validation type {}",denominationValidationType);
                Stream.of( denomination)
                        .filter(field -> filterDenomination(field,regex,excludeCharacterRegex))
                        .map(field -> new ConstraintViolationImpl<NewNotificationRequestV23>(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), finalRecIdx)))
                        .forEach(errors::add);
            }
            recIdx++;
        }
        return errors;
    }

    private record ValidationRegex(String regex, String excludedCharacterRegex){}

    private ValidationRegex initializeValidationRegex(String denominationValidationType){
        String regex;
        String excludeCharacterRegex = null;
        if(denominationValidationType.equalsIgnoreCase(REGEX.name() )){
            regex ="[" + this.pnDeliveryConfigs.getDenominationValidationRegexValue() + "]*";
        }else{
            regex = DenominationValidationUtils.getRegexValue(denominationValidationType);

            if(this.pnDeliveryConfigs.getDenominationValidationExcludedCharacter() != null &&
                    !this.pnDeliveryConfigs.getDenominationValidationExcludedCharacter().trim().isEmpty() &&
                    !this.pnDeliveryConfigs.getDenominationValidationExcludedCharacter().equalsIgnoreCase(NONE.name())){
                excludeCharacterRegex = "[^"+this.pnDeliveryConfigs.getDenominationValidationExcludedCharacter()+"]*";
            }
        }
        return new ValidationRegex(regex,excludeCharacterRegex);
    }

    private boolean filterDenomination(Pair<String,String> field, String regex, String excludeCharacterRegex ){
        return field.getValue() != null
                && ((!field.getValue().matches(regex))
                || (excludeCharacterRegex != null && (!field.getValue().matches(excludeCharacterRegex))));
    }

    private Set<ConstraintViolation<NewNotificationRequestV23>> checkApplyCost(boolean isNotificationFeePolicyDeliveryMode, List<NotificationPaymentItem> payments){

        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();

        int pagoPAPaymentsCounter = 0;
        int f24PaymentsCounter = 0;
        int pagoPAApplyCostFlgCount = 0;
        int f24ApplyCostFlgCount = 0;

        for (NotificationPaymentItem paymentInfo : payments) {
            if (paymentInfo.getPagoPa() != null) {
                pagoPAPaymentsCounter++;
                if (Boolean.TRUE.equals(paymentInfo.getPagoPa().getApplyCost())) {
                    pagoPAApplyCostFlgCount++;
                }
            }

            if(paymentInfo.getF24() != null) {
                f24PaymentsCounter++;
                if(Boolean.TRUE.equals(paymentInfo.getF24().getApplyCost())) {
                    f24ApplyCostFlgCount++;
                }

                if(!StringUtils.hasText(paymentInfo.getF24().getTitle())){
                    ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("F24 description is mandatory");
                    errors.add(constraintViolation);
                }
            }
        }

        boolean notificationHasPagoPaPayments = pagoPAPaymentsCounter > 0;
        boolean notificationHasF24Payments = f24PaymentsCounter > 0;
        checkApplyCost(pagoPAApplyCostFlgCount, f24ApplyCostFlgCount, notificationHasPagoPaPayments, notificationHasF24Payments, isNotificationFeePolicyDeliveryMode, errors);
        return errors;
    }

    private void checkApplyCost(int pagoPAApplyCostFlgCount, int f24ApplyCostFlgCount, boolean notificationHasPagoPaPayments, boolean notificationHasF24Payments, boolean isNotificationFeePolicyDeliveryMode, Set<ConstraintViolation<NewNotificationRequestV23>> errors) {
        if (isNotificationFeePolicyDeliveryMode) {
            if (notificationHasPagoPaPayments && pagoPAApplyCostFlgCount == 0) {
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must be valorized for at least one payment");
                errors.add(constraintViolation);
            }
            if (notificationHasF24Payments && f24ApplyCostFlgCount == 0) {
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must be valorized for at least one payment");
                errors.add(constraintViolation);
            }
        } else {
            if (pagoPAApplyCostFlgCount != 0) {
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must not be valorized for any payment");
                errors.add(constraintViolation);
            }
            if (f24ApplyCostFlgCount != 0) {
                ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must not be valorized for any payment");
                errors.add(constraintViolation);
            }
        }
    }

    public Set<ConstraintViolation<NewNotificationRequestV23>> checkIuvs(List<NotificationPaymentItem> payments, Set<String> iuvSet, int recIdx) {
        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();
        int paymIdx = 0;
        for (NotificationPaymentItem payment : payments) {
            if(payment.getPagoPa() != null) {
                String iuv = payment.getPagoPa().getCreditorTaxId() + payment.getPagoPa().getNoticeCode();

                if ( !iuvSet.add( iuv ) ) {
                    String errorMsg = String.format("Duplicated iuv { %s } on recipient with index %s in payment with index %s", iuv, recIdx, paymIdx);
                    ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>(errorMsg);
                    errors.add(constraintViolation);
                }
            }

            paymIdx++;
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


    private static void onlyNumericalTaxIdForPGV2(Set<ConstraintViolation<NewNotificationRequestV23>> errors, int recIdx, NotificationRecipientV23 recipient) {
        if (NotificationRecipientV23.RecipientTypeEnum.PG.equals(recipient.getRecipientType()) &&
                (!recipient.getTaxId().matches("^\\d+$"))) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("SEND accepts only numerical taxId for PG recipient " + recIdx);
            errors.add(constraintViolation);
        }
    }


    private static void checkProvinceV2(Set<ConstraintViolation<NewNotificationRequestV23>> errors, NotificationPhysicalAddress physicalAddress) {
        if (Objects.nonNull(physicalAddress) &&
                (!StringUtils.hasText(physicalAddress.getForeignState()) || physicalAddress.getForeignState().toUpperCase().trim().startsWith("ITAL")) &&
                !StringUtils.hasText(physicalAddress.getProvince())) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("No province provided in physical address");
            errors.add(constraintViolation);
        }
    }


    public Set<ConstraintViolation<NewNotificationRequestV23>> checkNewNotificationRequestForMVP(NewNotificationRequestV23 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV23>> errors = new HashSet<>();

        if (newNotificationRequestV2.getRecipients().size() > 1) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("Max one recipient");
            errors.add(constraintViolation);
        }

        List<NotificationPaymentItem> payment = newNotificationRequestV2.getRecipients().get(0).getPayments();
        if (Objects.isNull(payment) || payment.isEmpty()) {
            ConstraintViolationImpl<NewNotificationRequestV23> constraintViolation = new ConstraintViolationImpl<>("No recipient payment");
            errors.add(constraintViolation);
        }
        return errors;
    }
}