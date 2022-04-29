package it.pagopa.pn.delivery.svc;

import it.pagopa.pn.api.dto.notification.Notification;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.IntStream;

public class IunGeneratorTest {

    private IunGenerator iunGenerator;

    @BeforeEach
    private void setup() {
        iunGenerator = new IunGenerator();
    }

    public long generatePredictedIun(long numberOfTest) {

        HashSet<String> iunSet = new HashSet<>();

        long numberOfCollision = 0L;
        for (long i=0; i < numberOfTest; i++) {
            if (!iunSet.add( iunGenerator.generatePredictedIun(Instant.now().toString() ))) {
                numberOfCollision++;
            }
        }
        System.out.println("Test ripetuto "+ numberOfTest + " volte");
        System.out.println("Numero di collisioni: "+ numberOfCollision );
        return numberOfCollision;
    }

    @Test
    void checkControlCharacter(){
        char controlChar = iunGenerator.generateControlChar( "ABCD-EFGH-ILMN", "202201");
        Assertions.assertEquals( 'N', controlChar);
    }

    @Test
    void collisionsLessThanOneInOneYear() {
        List<Long> collisions = Collections.synchronizedList( new ArrayList<>() );

        long numberOfTest = 200000L;

        int monthsInOneYear = 1 * 10;
        IntStream.range(0, monthsInOneYear ).parallel().forEach( m -> {
            collisions.add( generatePredictedIun( numberOfTest ) );
        });
        System.out.println( collisions );
        Assertions.assertTrue( collisions.stream().reduce(0L,(a,b) -> a+b) < 1 );
    }
}
