package it.pagopa.pn.delivery.dao;

import it.pagopa.pn.delivery.model.events.NewNotificationEvt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@ConditionalOnProperty( name="pn.mom", havingValue = "none")
@Component
public class FakeNewNotificationEvtMOM implements NewNotificationEvtMOM {

    private List<NewNotificationEvt> messages = new ArrayList<>();

    @Override
    public synchronized CompletableFuture<List<NewNotificationEvt>> poll(Duration maxPollTime) {
        CompletableFuture<List<NewNotificationEvt>> result = CompletableFuture.completedFuture( messages );
        messages = new ArrayList<>();
        return result;
    }

    @Override
    public synchronized CompletableFuture<Void> push(NewNotificationEvt msg) {
        messages.add( msg );
        return CompletableFuture.completedFuture( null );
    }
}
