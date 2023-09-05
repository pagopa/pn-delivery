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
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;

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
    }

    public void checkNewNotificationBeforeInsertAndThrow(InternalNotification internalNotification) {
        Set<ConstraintViolation<InternalNotification>> errors = checkNewNotificationBeforeInsert(internalNotification);
        if (!errors.isEmpty()) {
            List<ProblemError> errorList = new ExceptionHelper(Optional.empty()).generateProblemErrorsFromConstraintViolation(errors);
            throw new PnInvalidInputException(internalNotification.getPaProtocolNumber(), errorList);
        }
    }

    public Set<ConstraintViolation<InternalNotification>> checkNewNotificationBeforeInsert(InternalNotification internalNotification) {
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

    public Set<ConstraintViolation<NewNotificationRequestV21>> checkNewNotificationRequestBeforeInsert(NewNotificationRequestV21 newNotificationRequestV2) {
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

            if (!validateUtils.validate(recipient.getTaxId())) {
                ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Invalid taxId for recipient " + recIdx);
                errors.add(constraintViolation);
            }
            if (!distinctTaxIds.add(recipient.getTaxId())) {
                ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Duplicated recipient taxId");
                errors.add(constraintViolation);
            }

            if (checkForDuplicateNoticeCode(recipient.getPayments())) {
                ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("Duplicated notice code");
                errors.add(constraintViolation);
            }

            boolean notificationFeePolicy = newNotificationRequestV2.getNotificationFeePolicy().equals(NotificationFeePolicy.DELIVERY_MODE);

            int pagoPAapplyCostFlgCount = 0;
            int f24ApplyCostFlgCount = 0;

            for (NotificationPaymentItem paymentInfo : recipient.getPayments()) {
                if (paymentInfo.getPagoPa() != null && Boolean.TRUE.equals(paymentInfo.getPagoPa().getApplyCost())) {
                    pagoPAapplyCostFlgCount++;
                }

                if(paymentInfo.getF24() != null && Boolean.TRUE.equals(paymentInfo.getF24().getApplyCost())){
                    f24ApplyCostFlgCount++;
                }

                //Verifica presenza del campo descrizione contenente il nome del file PDF per ogni F24. Nota: riportare nella documentazione openAPI
                //che tale campo dovrà indicare il nome del file visualizzato dal destinatario per lo scaricamento del relativo file PDF
                if(!StringUtils.hasText(paymentInfo.getF24().getTitle())){
                    ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("F24 description is mandatory");
                    errors.add(constraintViolation);
                }
            }

            if (notificationFeePolicy) {
                if (pagoPAapplyCostFlgCount < 1) {
                    ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must be valorized for only one payment");
                    errors.add(constraintViolation);
                }
                if (f24ApplyCostFlgCount < 1) {
                    ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must be valorized for only one payment");
                    errors.add(constraintViolation);
                }
            } else {
                if (pagoPAapplyCostFlgCount != 0) {
                    ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("PagoPA applyCostFlg must not be valorized for any payment");
                    errors.add(constraintViolation);
                }
                if (f24ApplyCostFlgCount != 0) {
                    ConstraintViolationImpl<NewNotificationRequestV21> constraintViolation = new ConstraintViolationImpl<>("F24 applyCostFlg must not be valorized for any payment");
                    errors.add(constraintViolation);
                }
            }
            NotificationPhysicalAddress physicalAddress = recipient.getPhysicalAddress();
            checkProvinceV2(errors, physicalAddress);
            recIdx++;
        }
        errors.addAll(validator.validate(newNotificationRequestV2));
        return errors;

    }


    public static boolean checkForDuplicateNoticeCode(List<NotificationPaymentItem> payments) {
        Set<String> noticeCodeSet = new HashSet<>();

        for (NotificationPaymentItem payment : payments) {
            String id = payment.getPagoPa().getNoticeCode();
            if (noticeCodeSet.contains(id)) {
                return true;
            } else {
                noticeCodeSet.add(id);
            }
        }
        return false;
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