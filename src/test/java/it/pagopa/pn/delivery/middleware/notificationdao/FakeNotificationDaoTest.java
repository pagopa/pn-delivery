package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.commons.abstractions.IdConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FakeNotificationDaoTest extends AbstractNotificationDaoTest {

    @BeforeEach
    void instantiateDao() {
        dao = new FakeNotificationDao();
    }

    @Override
    @Test
    void testInsertOk() throws IdConflictException {
        super.testInsertOk();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsDeliveryMode() throws IdConflictException {
        super.testInsertOkWithPaymentsDeliveryMode();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsFlat() throws IdConflictException {
        super.testInsertOkWithPaymentsFlat();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsIuvOnly() throws IdConflictException {
        super.testInsertOkWithPaymentsIuvOnly();
    }

    @Override
    @Test
    void testInsertOkWithPaymentsNoIuv() throws IdConflictException {
        super.testInsertOkWithPaymentsNoIuv();
    }

    @Override
    @Test
    void testInsertFail() throws IdConflictException {
        super.testInsertFail();
    }

    @Override
    @Test
    void testDelete() throws IdConflictException {
        super.testDelete();
    }

}
