package it.pagopa.pn.delivery.utils.io;

import com.nimbusds.jose.jwk.JWK;
import net.visma.autopay.http.signature.*;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;

public class SignatureTest {

    @Test
    @Disabled("create signature for test e2e")
    @DisplayName("Signing a Response using ecdsa-p256-sha256")
    public void signingResponseEcdsaP256Sha256() throws Exception {
        // setup
        var signatureLabel = "sig123";
        var keyId ="sha256-a7qE0Y0DyqeOFFREIQSLKfu5WlbckdxVXKFasfcI-Dg";
        var algorithm = SignatureAlgorithm.ECDSA_P256_SHA_256;
        Map<String, String> requestHeaders = Map.of(
                //"Content-Digest", "sha-256=:cpyRqJ1VhoVC+MSs9fq4/4wXs4c46EyEFriskys43Zw=:",
                "x-pagopa-lollipop-original-method", "GET",
                "x-pagopa-lollipop-original-url", "https://localhost:8080/delivery/notifications/received/*",
                "X-io-sign-qtspclauses", "anIoSignClauses"

        );
        SignatureContext requestContext = SignatureContext.builder()
                .headers(requestHeaders)
                .build();
        SignatureParameters signatureParams =  SignatureParameters.builder()
                .created(1678299228)
                .nonce("aNonce")
                .visibleAlgorithm(algorithm)
                .keyId(keyId)
                .build();
        var signatureComponents = SignatureComponents.builder()
                .headers(/*"Content-Digest",*/"x-pagopa-lollipop-original-method","x-pagopa-lollipop-original-url")
                .build();
        var signatureSpec = SignatureSpec.builder()
                .signatureLabel(signatureLabel)
                .privateKey(JWK.parse("{\n" +
                        "  kty: 'EC',\n" +
                        "  x: 'ShyYkFr7QwxOk8NAEqzjIdNw8tEJ89Y9PeXQuyUNX5c',\n" +
                        "  y: 'yT-Rq5g6VUZtCTwFgDLC3dgxcn3dlJcFF8gXgqah2KE',\n" +
                        "  crv: 'P-256',\n" +
                        "  d: 'JdBOP8JUhgZpQZoAQ8jWuSsQ8wodvxFkFoUfbCWIjRw'\n" +
                        "}").toECKey().toPrivateKey())
                .context(requestContext)
                .parameters(signatureParams)
                .components(signatureComponents)
                .build();


        // execute
        var signatureResult = signatureSpec.sign();
        var signature = signatureResult.getSignature();
        Assertions.assertNotNull(signature);
    }
}
