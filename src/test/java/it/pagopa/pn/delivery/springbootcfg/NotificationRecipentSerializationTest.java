package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV23;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class NotificationRecipentSerializationTest {

    @Autowired
    private JacksonTester<NotificationRecipientV23> jacksonTester;


    @Test
    void notificationRecipientSerializationTest() throws IOException {
        NotificationRecipientV23 recipient = new NotificationRecipientV23()
                .recipientType(NotificationRecipientV23.RecipientTypeEnum.PF)
                .denomination("Mario Rossi")
                .taxId("CCCCCCCC")
                .internalId(null);

        JsonContent<NotificationRecipientV23> json = jacksonTester.write(recipient);

        System.out.println(json.getJson());

        assertThat(json).hasJsonPath("recipientType")
                .hasJsonPath("denomination")
                .hasJsonPath("taxId");
    }
}
