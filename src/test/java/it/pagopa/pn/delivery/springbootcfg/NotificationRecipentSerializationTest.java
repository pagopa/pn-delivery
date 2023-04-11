package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipient;
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
    private JacksonTester<NotificationRecipient> jacksonTester;


    @Test
    void notificationRecipientSerializationTest() throws IOException {
        NotificationRecipient recipient = new NotificationRecipient()
                .recipientType(NotificationRecipient.RecipientTypeEnum.PF)
                .denomination("Mario Rossi")
                .taxId("CCCCCCCC")
                .internalId(null);

        JsonContent<NotificationRecipient> json = jacksonTester.write(recipient);

        System.out.println(json.getJson());

        assertThat(json).hasJsonPath("recipientType")
                .hasJsonPath("denomination")
                .hasJsonPath("taxId")
                .doesNotHaveJsonPath("internalId");
    }
}
