package it.pagopa.pn.delivery.svc.validation.validators.formal;

import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.model.PaGroup;
import it.pagopa.pn.delivery.models.InternalNotification;
import it.pagopa.pn.delivery.pnclient.externalregistries.PnExternalRegistriesClientImpl;
import it.pagopa.pn.delivery.svc.validation.context.InformalNotificationContext;
import it.pagopa.pn.delivery.svc.validation.context.NotificationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSingleErrorContaining;
import static it.pagopa.pn.delivery.svc.validation.validators.ValidatorTestSupport.assertSuccess;
import static org.mockito.Mockito.when;

class GroupValidatorTest {
    @Mock
    private PnExternalRegistriesClientImpl pnExternalRegistriesClient;

    private GroupValidator groupValidator;

    private static final String DEFAULT_SENDER_ID = "senderId";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        groupValidator = new GroupValidator(pnExternalRegistriesClient);
    }

    @Test
    void validateWithValidGroup() {
        NotificationContext context = notificationContext("validGroup", List.of("validGroup"));
        when(pnExternalRegistriesClient.getGroups("senderId", true))
                .thenReturn(List.of(new PaGroup().id("validGroup")));

        assertSuccess(groupValidator.validate(context));
    }

    @Test
    void validateWithoutUsingGroup() {
        NotificationContext context = notificationContext(null, Collections.emptyList());

        assertSuccess(groupValidator.validate(context));
        Mockito.verify(pnExternalRegistriesClient, Mockito.never()).getGroups(Mockito.anyString(), Mockito.anyBoolean());
    }

    @Test
    void validateWithInvalidGroup() {
        NotificationContext context = notificationContext("invalidGroup", List.of("validGroup", "invalidGroup"));
        when(pnExternalRegistriesClient.getGroups(DEFAULT_SENDER_ID, true))
                .thenReturn(List.of(new PaGroup().id("validGroup")));

        assertSingleErrorContaining(groupValidator.validate(context), "Group=invalidGroup not present or suspended/deleted in pa_groups=[PaGroup(id=validGroup, name=null, description=null, status=null)]");
    }

    @Test
    void validateWithGroupNotInCxGroups() {
        NotificationContext context = notificationContext("validGroup", List.of("otherGroup"));
        when(pnExternalRegistriesClient.getGroups(DEFAULT_SENDER_ID, true))
                .thenReturn(List.of(new PaGroup().id("validGroup")));

        assertSingleErrorContaining(groupValidator.validate(context), "Group=validGroup not present in cx_groups=[otherGroup]");
    }

    @Test
    void validateWithEmptyGroupAndCxGroups() {
        NotificationContext context = notificationContext(null, List.of("group1", "group2"));

        assertSingleErrorContaining(groupValidator.validate(context), "Specify a group in cx_groups=[group1, group2]");
    }



    private static NotificationContext notificationContext(String group, List<String> cxGroups) {
        return InformalNotificationContext.builder()
                .payload(InternalNotification.builder().group(group).build())
                .cxId(DEFAULT_SENDER_ID)
                .cxGroups(cxGroups)
                .build();
    }
}

