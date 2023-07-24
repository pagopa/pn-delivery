package it.pagopa.pn.delivery.utils;

public class PnDeliveryRestConstants {

    private PnDeliveryRestConstants() {}

    public static final String CX_ID_HEADER = "x-pagopa-pn-cx-id";
    public static final String UID_HEADER = "x-pagopa-pn-uid";
    public static final String CX_TYPE_HEADER = "x-pagopa-pn-cx-type";
    public static final String CX_GROUPS_HEADER = "x-pagopa-pn-cx-groups";

    public static final String SOURCE_CHANNEL_HEADER = "x-pagopa-pn-src-ch";
    public static final String SOURCE_CHANNEL_DETAILS_HEADER = "x-pagopa-pn-src-ch-details";


    private static final String DELIVERY_PATH = "delivery";
    private static final String DELIVERY_PATH_PRIVATE = "delivery-private";

    public static final String SEND_NOTIFICATIONS_PATH = DELIVERY_PATH + "/notifications/sent";
    public static final String ATTACHMENT_PRELOAD_REQUEST = DELIVERY_PATH + "/attachments/preload";

    public static final String NOTIFICATION_SENT_PATH = DELIVERY_PATH + "/notifications/sent/{iun}";
    public static final String NOTIFICATION_SENT_DOCUMENTS_PATH = DELIVERY_PATH + "/notifications/sent/{iun}/documents/{documentIndex}";
    public static final String NOTIFICATION_SENT_LEGALFACTS_PATH = DELIVERY_PATH + "/notifications/sent/{iun}/legalfacts";


    public static final String NOTIFICATIONS_RECEIVED_PATH = DELIVERY_PATH + "/notifications/received";
    public static final String NOTIFICATION_RECEIVED_PATH = DELIVERY_PATH + "/notifications/received/{iun}";
    public static final String NOTIFICATION_RECEIVED_LEGALFACTS_PATH = DELIVERY_PATH + "/notifications/received/{iun}/legalfacts";
    public static final String NOTIFICATION_VIEWED_PATH = DELIVERY_PATH + "/notifications/received/{iun}/documents/{documentIndex}";

    public static final String NOTIFICATION_UPDATE_STATUS_PATH = DELIVERY_PATH_PRIVATE + "/notifications/update-status";

    public static final String DIRECT_ACCESS_PATH = DELIVERY_PATH + "/direct_access";

    public static final String NOTIFICATION_RECEIVED_DELEGATED_PATH = DELIVERY_PATH + "/notifications/received/delegated";

}
