package it.pagopa.pn.delivery.rest.model;

import lombok.*;

import javax.validation.Path;

@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@Getter
@Setter
public class ResErrorModel {
    private String message;
    private Path path;
}
