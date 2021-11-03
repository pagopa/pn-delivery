package it.pagopa.pn.delivery.rest.dto;

import lombok.*;


@Builder(toBuilder = true)
@AllArgsConstructor
@ToString
@Getter
public class ErrorDto {
    private String code;
    private String message;
    private String property;
}
