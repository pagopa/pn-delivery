package it.pagopa.pn.commons.mom;

import java.util.concurrent.CompletableFuture;

public interface MomProducer<T> {

    public CompletableFuture<Void> push(T msg );

}
