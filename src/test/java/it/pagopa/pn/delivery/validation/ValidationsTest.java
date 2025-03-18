package it.pagopa.pn.delivery.validation;

import it.pagopa.pn.delivery.generated.openapi.server.v1.dto.NewNotificationRequestV25;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class ValidationsTest {
  
    private static Validator validator;

    private static ValidatorFactory validatorFactory;

    @Autowired
    private JacksonTester<NewNotificationRequestV25> jacksonTester;


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
        NewNotificationRequestV25 newNotificationRequest = jacksonTester.parseObject(json);

        System.out.println(newNotificationRequest);

        Set<ConstraintViolation<NewNotificationRequestV25>> violations = validator.validate(newNotificationRequest);

        assertThat(violations).isEmpty();
    }

    @Test
    void regexTest() {
        String url1 = "https://api-app.io.pagopa.it/api/v1/third-party-messages/asdasd123qwasdasdasdvxvs23";
        String url2 = "https://api-app.io.pagopa.it/api/v1/third-party-messages/asdasd123qwasdasdasdvxvs23/attachments/delivery/notifications/received/ENZE-EUJU-UMGL-202306-H-1/attachments/documents/0";
        String url3 = "https://api-app.io.pagopa.it/api/v1/third-party-messages/asdasd123qwasdasdasdvxvs23/attachments/delivery/notifications/received/ENZE-EUJU-UMGL-202306-H-1/attachments/payment/F24/?attachmentIdx=0";
        String regex = "^https://api-app.io.pagopa.it/api/v1/third-party-messages/[a-zA-Z0-9]{26}(:?/attachments/delivery/notifications/received/([A-Z]{4}-[A-Z]{4}-[A-Z]{4}-[0-9]{6}-[A-Z]-[0-9])/attachments/documents/[0-9])?(:?/attachments/delivery/notifications/received/([A-Z]{4}-[A-Z]{4}-[A-Z]{4}-[0-9]{6}-[A-Z]-[0-9])/attachments/payment/(F24|PAGOPA)(/?\\?attachmentIdx\\x3D\\d+))?$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher1 = pattern.matcher(url1);
        Matcher matcher2 = pattern.matcher(url2);
        Matcher matcher3 = pattern.matcher(url3);

        boolean match1 = matcher1.matches();
        boolean match2 = matcher2.matches();
        boolean match3 = matcher3.matches();
        Assertions.assertTrue( match1 );
        Assertions.assertTrue( match2 );
        Assertions.assertTrue( match3 );

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
                            },
                            "digitalDomicile": {
                                "type": "PEC",
                                "address": "mail_ok@abc.it"
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
