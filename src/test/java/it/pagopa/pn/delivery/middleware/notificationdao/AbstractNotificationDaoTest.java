package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.api.dto.notification.Notification;
import it.pagopa.pn.commons.abstractions.IdConflictException;
import it.pagopa.pn.delivery.NotificationFactoryForTesting;
import it.pagopa.pn.delivery.middleware.NotificationDao;
import org.junit.jupiter.api.Assertions;

abstract class AbstractNotificationDaoTest {

    protected NotificationDao dao;

    abstract void instantiateDao();

    void testInsertOk() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithoutPayments( true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Notification saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertEquals( notification, saved );
    }

    void testInsertOkWithPaymentsDeliveryMode() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsDeliveryMode( true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Notification saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertEquals( notification, saved );
    }

    void testInsertOkWithPaymentsFlat() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsFlat( true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Notification saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertEquals( notification, saved );
    }

    void testInsertOkWithPaymentsIuvOnly() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithPaymentsIuvOnly( true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Notification saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertEquals( notification, saved );
    }

    void testInsertOkWithPaymentsNoIuv() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting
                              .newNotificationWithPaymentsDeliveryMode( null, true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Notification saved = this.dao.getNotificationByIun( notification.getIun() );
        Assertions.assertEquals( notification, saved );
    }



    void testInsertFail() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithoutPayments( true );

        // WHEN
        this.dao.addNotification( notification );

        // THEN
        Assertions.assertThrows( IdConflictException.class, () -> {
            this.dao.addNotification( notification );
        });
    }


    void testDelete() throws IdConflictException {

        // GIVEN
        Notification notification = NotificationFactoryForTesting.newNotificationWithoutPayments( true );

        // WHEN
        this.dao.addNotification( notification );
        this.dao.deleteNotificationByIun( notification.getIun() );

        // THEN
        Assertions.assertNull( this.dao.getNotificationByIun( notification.getIun() ) );
    }

}
