package it.pagopa.pn.delivery.rest.dto;

import lombok.*;
import org.springframework.http.HttpStatus;

import java.util.List;

@AllArgsConstructor
@Builder(toBuilder = true)
@ToString
@Getter
public class ResErrorDto {
    private String paNotificationId;
    private String status;
    private List<ErrorDto> errorDtoList;
}
