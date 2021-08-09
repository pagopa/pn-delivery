package it.pagopa.pn.commons.abstractions;

import java.util.Collections;
import java.util.List;

public interface MomProducer<T> {

    void push( List<T> messages );

    default void push( T message ) {
        push( Collections.singletonList( message ));
    }
}
