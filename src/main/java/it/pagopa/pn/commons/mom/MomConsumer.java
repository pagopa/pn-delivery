package it.pagopa.pn.commons.mom;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface MomConsumer<T> {

    public CompletableFuture<List<T>> poll(Duration maxPollTime );

}
