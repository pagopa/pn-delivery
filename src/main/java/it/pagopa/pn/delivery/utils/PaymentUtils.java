package it.pagopa.pn.delivery.utils;

public class PaymentUtils {

    public static String composeIuv(String noticeCode, String taxId) {
        return taxId + "##" + noticeCode;
    }
}
