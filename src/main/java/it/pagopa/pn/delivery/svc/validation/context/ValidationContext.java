package it.pagopa.pn.delivery.svc.validation.context;

public interface ValidationContext<T> {
    T getPayload();
}