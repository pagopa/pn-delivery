package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.commons.configs.MVPParameterConsumer;
import it.pagopa.pn.commons.exceptions.ExceptionHelper;
import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.exceptions.dto.ProblemError;
import it.pagopa.pn.commons.utils.ValidateUtils;
import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.config.PhysicalAddressLookupParameterConsumer;
import it.pagopa.pn.delivery.exception.PnBadRequestException;
import it.pagopa.pn.delivery.exception.PnInvalidInputException;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.api.AgenziaEntrateApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.model.CheckTaxIdOK;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.model.CheckTaxIdRequestBody;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.model.CheckTaxIdRequestBodyFilter;
import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.*;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.rest.dto.ConstraintViolationImpl;
import it.pagopa.pn.delivery.svc.search.AllowedAdditionalLanguages;
import it.pagopa.pn.delivery.utils.DenominationValidationUtils;
import it.pagopa.pn.delivery.utils.FeatureFlagUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.utils.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static it.pagopa.pn.delivery.exception.PnDeliveryExceptionCodes.*;
import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.NONE;
import static it.pagopa.pn.delivery.utils.DenominationValidationUtils.ValidationTypeAllowedValues.REGEX;

@Slf4j
@Component
public class NotificationReceiverValidator {

    private final Validator validator;
    private final MVPParameterConsumer mvpParameterConsumer;
    private final ValidateUtils validateUtils;
    private final PnDeliveryConfigs pnDeliveryConfigs;
    private final AgenziaEntrateApi agenziaEntrateApi;
    private final PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter;

    private final FeatureFlagUtils featureFlagUtils;
    public static final String REQUIRED_ADDITIONAL_LANG_SIZE = "È obbligatorio fornire una sola lingua aggiuntiva.";
    private static final String APPLICATION_PDF_CONTENT_TYPE = "application/pdf";
    private static final String APPLICATION_JSON_CONTENT_TYPE = "application/json";
    public static final String EXTENSION_PDF = ".pdf";
    public static final String EXTENSION_JSON = ".json";
    public static final String PN_NOTIFICATION_ATTACHMENTS = "PN_NOTIFICATION_ATTACHMENTS";
    public static final String PN_F24_META = "PN_F24_META";

    public NotificationReceiverValidator(Validator validator, MVPParameterConsumer mvpParameterConsumer, ValidateUtils validateUtils, PnDeliveryConfigs pnDeliveryConfigs,
                                         AgenziaEntrateApi agenziaEntrateApi, PhysicalAddressLookupParameterConsumer physicalAddressLookupParameter, FeatureFlagUtils featureFlagUtils) {
        this.validator = validator;
        this.mvpParameterConsumer = mvpParameterConsumer;
        this.validateUtils = validateUtils;
        this.pnDeliveryConfigs = pnDeliveryConfigs;
        this.agenziaEntrateApi = agenziaEntrateApi;
        this.physicalAddressLookupParameter = physicalAddressLookupParameter;
        this.featureFlagUtils = featureFlagUtils;

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


    public void checkNewNotificationRequestBeforeInsertAndThrow(NewNotificationRequestV25 newNotificationRequestV2, String paId) {
        Set<ConstraintViolation<NewNotificationRequestV25>> errors = checkNewNotificationRequestBeforeInsert(newNotificationRequestV2, paId);
        if (Boolean.TRUE.equals(mvpParameterConsumer.isMvp(newNotificationRequestV2.getSenderTaxId())) && errors.isEmpty()) {
            errors = checkNewNotificationRequestForMVP(newNotificationRequestV2);
        }
        if (!errors.isEmpty()) {
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(newNotificationRequestV2.getPaProtocolNumber(), errorList);
        }

        checkAdditionalLanguages(newNotificationRequestV2.getAdditionalLanguages());
    }

    protected void checkAdditionalLanguages(List<String> additionalLanguages) {
        if(!CollectionUtils.isNullOrEmpty(additionalLanguages) && additionalLanguages.size() > 1){
            throw new PnBadRequestException(REQUIRED_ADDITIONAL_LANG_SIZE, REQUIRED_ADDITIONAL_LANG_SIZE, ERROR_CODE_DELIVERY_REQUIRED_ADDITIONAL_LANG, REQUIRED_ADDITIONAL_LANG_SIZE);
        }
        else if(!CollectionUtils.isNullOrEmpty(additionalLanguages) && !isValidAdditionalLanguage(additionalLanguages.get(0))){
            String logMessage = String.format("Lingua aggiuntiva non valida, i valori accettati sono %s", Arrays.stream(AllowedAdditionalLanguages.values()).map(Enum::name).collect(Collectors.joining(",")));
            throw new PnBadRequestException(logMessage, logMessage, ERROR_CODE_DELIVERY_INVALID_ADDITIONAL_LANG, logMessage);
        }
    }

    private boolean isValidAdditionalLanguage(String lang) {
        return Arrays.stream(AllowedAdditionalLanguages.values())
                .map(AllowedAdditionalLanguages::name)
                .anyMatch(lang::equals);
    }

    protected Set<ConstraintViolation<NewNotificationRequestV25>> checkNewNotificationRequestBeforeInsert(NewNotificationRequestV25 newNotificationRequestV25, String paId) {
        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();

        boolean physicalAddressLookup = checkPhysicalAddressLookupIsEnabled(paId);

        // check del numero massimo di documenti allegati
        if (pnDeliveryConfigs.getMaxAttachmentsCount() > 0 && newNotificationRequestV25.getDocuments().size() > pnDeliveryConfigs.getMaxAttachmentsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("Max attachment count reached");
            errors.add(constraintViolation);
            return errors;
        }

        // check del numero massimo di recipient
        if (pnDeliveryConfigs.getMaxRecipientsCount() > 0 && newNotificationRequestV25.getRecipients().size() > pnDeliveryConfigs.getMaxRecipientsCount()) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("Max recipient count reached");
            errors.add(constraintViolation);
            return errors;
        }

        // check campi paFee e vat per notifiche DELIVERY_MODE
        if ( newNotificationRequestV25.getNotificationFeePolicy().equals(NotificationFeePolicy.DELIVERY_MODE) &&
                (Objects.isNull(newNotificationRequestV25.getPaFee()) || Objects.isNull(newNotificationRequestV25.getVat()))) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("paFee or vat field not filled in for notification with notificationFeePolicy DELIVERY_MODE");
            errors.add( constraintViolation );
        }

        int recIdx = 0;
        Set<String> distinctTaxIds = new HashSet<>();
        Set<String> distinctIuvs = new HashSet<>();
        for (NotificationRecipientV24 recipient : newNotificationRequestV25.getRecipients()) {

            // limitazione temporanea: destinatari PG possono avere solo TaxId numerico
            onlyNumericalTaxIdForPGV2(errors, recIdx, recipient);

            //Check taxId
            errors.addAll(checkTaxId(recipient, distinctTaxIds, recIdx));

            boolean isNotificationFeePolicyDeliveryMode = newNotificationRequestV25.getNotificationFeePolicy().equals(NotificationFeePolicy.DELIVERY_MODE);
            if(recipient.getPayments() != null) {
                errors.addAll(checkApplyCost(isNotificationFeePolicyDeliveryMode, recipient.getPayments()));
                errors.addAll(checkIuvs(recipient.getPayments(), distinctIuvs, recIdx));
                errors.addAll(checkPaymentAttachmentExtension(recipient.getPayments()));
            }

            if(!physicalAddressLookup || recipient.getPhysicalAddress() != null) {
                NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
                errors.addAll( this.checkPhysicalAddress(physicalAddress, recIdx));
                checkProvinceV2(errors, physicalAddress);
            }

            errors.addAll(this.checkDenomination(recipient, recIdx ));

            recIdx++;
        }

        if (!hasDistinctAttachments(newNotificationRequestV25)) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("Same attachment compares more then once in the same request");
            errors.add(constraintViolation);
        }

        errors.addAll(validator.validate( newNotificationRequestV25 ));
        return errors;
    }

    private Set <ConstraintViolation<NewNotificationRequestV25>> checkPaymentAttachmentExtension(List<NotificationPaymentItem> payments) {
        Set<ConstraintViolation<NewNotificationRequestV25>> violations = new HashSet<>();
        for (NotificationPaymentItem paymentItem : payments) {
            // Verifica per pagamenti F24
            if (paymentItem.getF24() != null) {
                NotificationMetadataAttachment metadataAttachment = paymentItem.getF24().getMetadataAttachment();
                checkContentType(metadataAttachment.getContentType(), metadataAttachment.getRef().getKey(), violations);
            }
            // Verifica per pagamenti PagoPA
            else if (paymentItem.getPagoPa() != null && paymentItem.getPagoPa().getAttachment() != null) {
                NotificationPaymentAttachment attachment = paymentItem.getPagoPa().getAttachment();
                checkContentType(attachment.getContentType(), attachment.getRef().getKey(), violations);
            }
        }
        return violations;
    }

    private void checkContentType(String contentType, String key, Set<ConstraintViolation<NewNotificationRequestV25>> violations) {
        if (APPLICATION_PDF_CONTENT_TYPE.equalsIgnoreCase(contentType) && (!key.contains(PN_NOTIFICATION_ATTACHMENTS) || key.endsWith(EXTENSION_JSON) )) {
            ConstraintViolationImpl<NewNotificationRequestV25> violation = new ConstraintViolationImpl<>(String.format("Key: %s does not conform to the expected content type: %s", key, contentType));
            violations.add(violation);
        }
        if (APPLICATION_JSON_CONTENT_TYPE.equalsIgnoreCase(contentType) && (!key.contains(PN_F24_META) || key.endsWith(EXTENSION_PDF))) {
            ConstraintViolationImpl<NewNotificationRequestV25> violation = new ConstraintViolationImpl<>(String.format("Key: %s does not conform to the expected content type: %s", key, contentType));
            violations.add(violation);
        }
    }

    /**
     * Validazio di NotificationRecipientV24 per verificare il taxId e la sua univocità
     * @param recipient
     * @return
     */
    protected Set<ConstraintViolation<NewNotificationRequestV25>> checkTaxId(NotificationRecipientV24 recipient, Set<String> distinctTaxIds, int recIdx){

        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();
        boolean isPF = NotificationRecipientV24.RecipientTypeEnum.PF.getValue().equals(recipient.getRecipientType().getValue());
        boolean skipCheckTaxIdInBlackList = pnDeliveryConfigs.isSkipCheckTaxIdInBlackList();

        if( !validateUtils.validate(recipient.getTaxId(), isPF, false, skipCheckTaxIdInBlackList)) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>( "Invalid taxId for recipient " + recIdx );
            errors.add(constraintViolation);
        }else{
            if(pnDeliveryConfigs.isEnableTaxIdExternalValidation() && !callAdeCheckTaxId(recipient.getTaxId(), recIdx)){
                ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("Invalid taxId for recipient " + recIdx);
                errors.add(constraintViolation);
            }
        }

        if ( !distinctTaxIds.add( recipient.getTaxId() )){
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>( "Duplicated recipient taxId" );
            errors.add(constraintViolation);
        }

        return errors;
    }


    /**
     * Validazio di NewNotificationRequestV25 per verificare l'assenza di duplicati tra gli allegati
     * @param newNotificationRequest
     * @return
     */
    protected boolean hasDistinctAttachments(NewNotificationRequestV25 newNotificationRequest){
        Set<String> uniqueIds = new HashSet<>();

        for (NotificationDocument doc : emptyIfNull(newNotificationRequest.getDocuments())) {
            if (doc.getRef() != null && doc.getDigests() != null) {
                String id = doc.getRef().getKey() + doc.getDigests().getSha256();
                if (!uniqueIds.add(id)) {
                    return false;
                }
            }
        }

        long duplicates = emptyIfNull(newNotificationRequest.getRecipients())
            .stream()
            .map(recipient -> hasRecipientDistinctAttachments(recipient, uniqueIds))
            .filter(res -> !res).count();

        return duplicates==0;
    }

    private boolean hasRecipientDistinctAttachments(NotificationRecipientV24 recipient, Set<String> docIds){
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

    protected Set<ConstraintViolation<NewNotificationRequestV25>> checkPhysicalAddress(NotificationPhysicalAddress physicalAddress, int recIdx) {

        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();

        if(physicalAddress == null){
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("PhysicalAddress cannot be null");
            errors.add(constraintViolation);
            return errors;
        }

        if (this.pnDeliveryConfigs.isPhysicalAddressValidation()) {

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
                            (!field.getValue().matches("[" + this.pnDeliveryConfigs.getPhysicalAddressValidationPattern() + "]*")))
                    .map(field -> new ConstraintViolationImpl<NewNotificationRequestV25>(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), recIdx)))
                    .forEach(errors::add);

            Stream.of(row2, addressDetails, address, row5, foreignState)
                    .filter(field -> field.getValue() != null && field.getValue().trim().length() > this.pnDeliveryConfigs.getPhysicalAddressValidationLength() )
                    .map(field -> new ConstraintViolationImpl<NewNotificationRequestV25>(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), recIdx, this.pnDeliveryConfigs.getPhysicalAddressValidationLength())))
                    .forEach(errors::add);

        }

        return errors;

    }

    protected Set<ConstraintViolation<NewNotificationRequestV25>> checkDenomination(NotificationRecipientV24 recipient, int recIdx) {

        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();

        Pair<String, String> denomination = Pair.of("denomination", recipient.getDenomination());
        ArrayList<Pair<String, String>> fieldsToCheck = new ArrayList<>();
        fieldsToCheck.add(denomination);

        Pair<String, String> at;
        String atValue = recipient.getPhysicalAddress() == null ? null : recipient.getPhysicalAddress().getAt();
        if (atValue != null && !atValue.isEmpty()) {
            at = Pair.of("at", atValue);
            fieldsToCheck.add(at);
        }

        if(this.pnDeliveryConfigs.getDenominationLength() != null && this.pnDeliveryConfigs.getDenominationLength() != 0){
            fieldsToCheck.stream()
                    .filter(field -> field.getValue() != null && field.getValue().trim().length() > this.pnDeliveryConfigs.getDenominationLength() )
                    .map(field -> new ConstraintViolationImpl<NewNotificationRequestV25>(String.format("Field %s in recipient %s exceed max length of %s chars", field.getKey(), recIdx, this.pnDeliveryConfigs.getDenominationLength())))
                    .forEach(errors::add);
        }

        if(this.pnDeliveryConfigs.getDenominationValidationTypeValue() != null && !this.pnDeliveryConfigs.getDenominationValidationTypeValue().equalsIgnoreCase(NONE.name())){
            String denominationValidationType = this.pnDeliveryConfigs.getDenominationValidationTypeValue().toLowerCase();

            ValidationRegex validationRegex = initializeValidationRegex(denominationValidationType);

            String regex = validationRegex.regex;
            String excludeCharacterRegex = validationRegex.excludedCharacterRegex;

            log.info("Check denomination/at with validation type {}",denominationValidationType);
            fieldsToCheck.stream()
                    .filter(field -> filterDenomination(field,regex,excludeCharacterRegex))
                    .map(field -> new ConstraintViolationImpl<NewNotificationRequestV25>(String.format("Field %s in recipient %s contains invalid characters.", field.getKey(), recIdx)))
                    .forEach(errors::add);
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

    private Set<ConstraintViolation<NewNotificationRequestV25>> checkApplyCost(boolean isNotificationFeePolicyDeliveryMode, List<NotificationPaymentItem> payments){

        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();

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
                    ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("F24 description is mandatory");
                    errors.add(constraintViolation);
                }
            }
        }

        boolean notificationHasPagoPaPayments = pagoPAPaymentsCounter > 0;
        boolean notificationHasF24Payments = f24PaymentsCounter > 0;
        checkApplyCost(pagoPAApplyCostFlgCount, f24ApplyCostFlgCount, notificationHasPagoPaPayments, notificationHasF24Payments, isNotificationFeePolicyDeliveryMode, errors);
        return errors;
    }

    private void checkApplyCost(int pagoPAApplyCostFlgCount, int f24ApplyCostFlgCount, boolean notificationHasPagoPaPayments, boolean notificationHasF24Payments, boolean isNotificationFeePolicyDeliveryMode, Set<ConstraintViolation<NewNotificationRequestV25>> errors) {
        if (isNotificationFeePolicyDeliveryMode) {
            if (notificationHasPagoPaPayments && pagoPAApplyCostFlgCount == 0) {
                ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must be valorized for at least one payment");
                errors.add(constraintViolation);
            }
            if (notificationHasF24Payments && f24ApplyCostFlgCount == 0) {
                ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must be valorized for at least one payment");
                errors.add(constraintViolation);
            }
        } else {
            if (pagoPAApplyCostFlgCount != 0) {
                ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must not be valorized for any payment");
                errors.add(constraintViolation);
            }
            if (f24ApplyCostFlgCount != 0) {
                ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must not be valorized for any payment");
                errors.add(constraintViolation);
            }
        }
    }

    public Set<ConstraintViolation<NewNotificationRequestV25>> checkIuvs(List<NotificationPaymentItem> payments, Set<String> iuvSet, int recIdx) {
        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();
        int paymIdx = 0;
        for (NotificationPaymentItem payment : payments) {
            if(payment.getPagoPa() != null) {
                String iuv = payment.getPagoPa().getCreditorTaxId() + payment.getPagoPa().getNoticeCode();

                if ( !iuvSet.add( iuv ) ) {
                    String errorMsg = String.format("Duplicated iuv { %s } on recipient with index %s in payment with index %s", iuv, recIdx, paymIdx);
                    ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>(errorMsg);
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


    private static void onlyNumericalTaxIdForPGV2(Set<ConstraintViolation<NewNotificationRequestV25>> errors, int recIdx, NotificationRecipientV24 recipient) {
        if (NotificationRecipientV24.RecipientTypeEnum.PG.equals(recipient.getRecipientType()) &&
                (!recipient.getTaxId().matches("^\\d+$"))) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("SEND accepts only numerical taxId for PG recipient " + recIdx);
            errors.add(constraintViolation);
        }
    }

    private boolean callAdeCheckTaxId(String taxId, int recIdx) {
        boolean isValid = false;
        try {
            CheckTaxIdRequestBodyFilter filter = new CheckTaxIdRequestBodyFilter();
            filter.setTaxId(taxId);
            CheckTaxIdRequestBody requestBody = new CheckTaxIdRequestBody();
            requestBody.setFilter(filter);
            CheckTaxIdOK response = agenziaEntrateApi.checkTaxId(requestBody);
            if (Objects.nonNull(response)) {
                isValid = Boolean.TRUE.equals(response.getIsValid());
                if (!isValid) {
                    log.warn("AdE - invalid taxId for recipient {}, error: {}", recIdx, response.getErrorCode());
                }
            }
            return isValid;
        } catch (Exception e) {
            log.error("Error calling check taxId on AdE", e);
            throw new PnInternalException("Error calling check taxId on AdE", 503, ERROR_CODE_DELIVERY_ADECHECKCF);
        }
    }


    private static void checkProvinceV2(Set<ConstraintViolation<NewNotificationRequestV25>> errors, NotificationPhysicalAddress physicalAddress) {
        if (Objects.nonNull(physicalAddress) &&
                (!StringUtils.hasText(physicalAddress.getForeignState()) || physicalAddress.getForeignState().toUpperCase().trim().startsWith("ITAL")) &&
                !StringUtils.hasText(physicalAddress.getProvince())) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("No province provided in physical address");
            errors.add(constraintViolation);
        }
    }

    private boolean checkPhysicalAddressLookupIsEnabled (String paId){
        List<String> activePAsForPhysicalAddressLookup = physicalAddressLookupParameter.getActivePAsForPhysicalAddressLookup();

        return featureFlagUtils.isPhysicalAddressLookupEnabled() &&
                (activePAsForPhysicalAddressLookup.isEmpty() ||
                        activePAsForPhysicalAddressLookup.contains(paId));
    }

    public Set<ConstraintViolation<NewNotificationRequestV25>> checkNewNotificationRequestForMVP(NewNotificationRequestV25 newNotificationRequestV2) {
        Set<ConstraintViolation<NewNotificationRequestV25>> errors = new HashSet<>();

        if (newNotificationRequestV2.getRecipients().size() > 1) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("Max one recipient");
            errors.add(constraintViolation);
        }

        List<NotificationPaymentItem> payment = newNotificationRequestV2.getRecipients().get(0).getPayments();
        if (Objects.isNull(payment) || payment.isEmpty()) {
            ConstraintViolationImpl<NewNotificationRequestV25> constraintViolation = new ConstraintViolationImpl<>("No recipient payment");
            errors.add(constraintViolation);
        }
        return errors;
    }
}