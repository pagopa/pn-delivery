package it.pagopa.pn.delivery.config;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Optional;
class SendActiveParameterConsumerTest {

    public static final String PA_TAX_ID_ACTIVE = "80016350821";
    public static final String PA_TAX_ID_INACTIVE = "01234567891";

    private ParameterConsumer parameterConsumer;

    private SendActiveParameterConsumer sendActiveParameterConsumer;

    @BeforeEach
    void setup() {
        this.parameterConsumer = Mockito.mock( ParameterConsumer.class );
        this.sendActiveParameterConsumer = new SendActiveParameterConsumer(parameterConsumer);
    }

    @Test
    void isSendActive() {
        SendActiveParameterConsumer.PaTaxIdIsSendActive[] paTaxIdIsSendActives = new SendActiveParameterConsumer.PaTaxIdIsSendActive[2];
        paTaxIdIsSendActives[0] = new SendActiveParameterConsumer.PaTaxIdIsSendActive(PA_TAX_ID_ACTIVE, true );
        paTaxIdIsSendActives[1] = new SendActiveParameterConsumer.PaTaxIdIsSendActive( PA_TAX_ID_INACTIVE, false );

        Mockito.when( parameterConsumer.getParameterValue( Mockito.anyString(), Mockito.any() ) )
                .thenReturn( Optional.of(paTaxIdIsSendActives) );

        Assertions.assertTrue( sendActiveParameterConsumer.isSendActive( PA_TAX_ID_ACTIVE ) );
        Assertions.assertFalse( sendActiveParameterConsumer.isSendActive( PA_TAX_ID_INACTIVE ) );

    }
}