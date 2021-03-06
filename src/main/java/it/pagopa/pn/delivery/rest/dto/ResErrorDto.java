package it.pagopa.pn.delivery.rest.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@Getter
public class ResErrorDto {
    private String identificationId;
    private String status;
    private List<ErrorDto> errorDtoList;
}
