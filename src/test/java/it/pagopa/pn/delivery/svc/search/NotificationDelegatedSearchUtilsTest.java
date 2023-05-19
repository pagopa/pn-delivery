package it.pagopa.pn.delivery.svc.search;


import it.pagopa.pn.delivery.generated.openapi.clients.mandate.model.InternalMandateDto;
import it.pagopa.pn.delivery.middleware.notificationdao.entities.NotificationDelegationMetadataEntity;
import it.pagopa.pn.delivery.models.InputSearchNotificationDelegatedDto;
import it.pagopa.pn.delivery.pnclient.mandate.PnMandateClientImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class NotificationDelegatedSearchUtilsTest {

    private PnMandateClientImpl mandateClient;

    private NotificationDelegatedSearchUtils notificationDelegatedSearchUtils;
    private InputSearchNotificationDelegatedDto searchDto;
    private static final int PAGE_SIZE = 10;
    private static final String DELEGATE_ID = "delegateId";


    @BeforeEach
    void setup() {
        mandateClient = mock(PnMandateClientImpl.class);
        searchDto = InputSearchNotificationDelegatedDto.builder()
                .delegateId(DELEGATE_ID)
                .startDate(Instant.now().minus(500, ChronoUnit.DAYS))
                .endDate(Instant.now())
                .size(PAGE_SIZE)
                .build();
        notificationDelegatedSearchUtils = new NotificationDelegatedSearchUtils(mandateClient);

    }

    @Test
    void testCheckMandate() {
        InternalMandateDto internalMandateDto = new InternalMandateDto();
        internalMandateDto.setDelegate(DELEGATE_ID);
        internalMandateDto.setMandateId("mandateId");
        internalMandateDto.setDatefrom(Instant.now().minus(500, ChronoUnit.DAYS).toString());
        internalMandateDto.setDateto(Instant.now().toString());
        internalMandateDto.setVisibilityIds(Collections.singletonList("visibilityId"));
        internalMandateDto.setDelegator("dr1");
        when(mandateClient.listMandatesByDelegators(any(), any(), any())).thenReturn(Collections.singletonList(internalMandateDto));
        List<NotificationDelegationMetadataEntity> list = Collections.singletonList(
                NotificationDelegationMetadataEntity.builder()
                        .mandateId("mandateId")
                        .sentAt(Instant.now())
                        .senderId("senderId")
                        .recipientId("dr1")
                        .build()
        );
        List<NotificationDelegationMetadataEntity> result = notificationDelegatedSearchUtils.checkMandates(list, searchDto);
        assertNotNull(result);
        assertEquals(0, result.size());
    }
}