package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NotificationRecipientV21;
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
    private JacksonTester<NotificationRecipientV21> jacksonTester;


    @Test
    void notificationRecipientSerializationTest() throws IOException {
        NotificationRecipientV21 recipient = new NotificationRecipientV21()
                .recipientType(NotificationRecipientV21.RecipientTypeEnum.PF)
                .denomination("Mario Rossi")
                .taxId("CCCCCCCC")
                .internalId(null);

        JsonContent<NotificationRecipientV21> json = jacksonTester.write(recipient);

        System.out.println(json.getJson());

        assertThat(json).hasJsonPath("recipientType")
                .hasJsonPath("denomination")
                .hasJsonPath("taxId");
    }
}
