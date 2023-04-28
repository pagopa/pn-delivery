package it.pagopa.pn.delivery.validation;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.IOException;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ValidationsTest {

    private static Validator validator;

    private static ValidatorFactory validatorFactory;

    @Autowired
    private JacksonTester<NewNotificationRequest> jacksonTester;


    @BeforeAll
    public static void initAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    public static void destroy() {
        validatorFactory.close();
    }


    @Test
    void newNotificationRequestValidationTest() throws IOException {
        String json = getJSON();
        NewNotificationRequest newNotificationRequest = jacksonTester.parseObject(json);

        System.out.println(newNotificationRequest);

        Set<ConstraintViolation<NewNotificationRequest>> violations = validator.validate(newNotificationRequest);

        assertThat(violations).isEmpty();
    }


    private String getJSON() {
        return """
                {
                    "paProtocolNumber": "12345-02",
                    "subject": "Test procedura normale",
                    "recipients": [
                        {
                            "denomination": "Ilaria D`Amico D’Amico D'Amico",
                            "recipientType": "PF",
                            "taxId": "XRYZCL75D02A123X",
                            "physicalAddress": {
                                "at": "",
                                "address": "Indirizzo 10 con un apostrofo speciale ’",
                                "addressDetails": "",
                                "zip": "ABC-2 EF",
                                "municipality": "Milano con un apostrofo speciale ’",
                                "municipalityDetails": "",
                                "province": "MI",
                                "foreignState": "Spagna"
                            }
                        }
                    ],
                    "documents": [
                        {
                            "digests": {
                                "sha256": "zIWlh5jzg1abhAlrTxEcjnpha5Shz+jGN9cyYIof+es="
                            },
                            "contentType": "application/pdf",
                            "ref": {
                                "key": "PN_NOTIFICATION_ATTACHMENTS-fffc69ed1a244928ae9b986d7794411c.pdf",
                                "versionToken": ".vvcH0pmWNTHqeAsGP8RRbH8QGKrHgN4"
                            },
                            "title": "atto 1"
                        }
                    ],
                    "physicalCommunicationType": "REGISTERED_LETTER_890",
                    "group": "",
                    "taxonomyCode": "123356N",
                    "notificationFeePolicy": "FLAT_RATE",
                    "senderDenomination": "Comune di Sappada",
                    "senderTaxId": "00207190257",
                    "abstract": ""
                }
                """;
    }
}
