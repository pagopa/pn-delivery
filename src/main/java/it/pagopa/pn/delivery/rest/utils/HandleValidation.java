package it.pagopa.pn.delivery.rest.utils;

import it.pagopa.pn.commons.exceptions.PnValidationException;
import it.pagopa.pn.delivery.rest.dto.ErrorDto;
import it.pagopa.pn.delivery.rest.dto.ResErrorDto;
import org.springframework.http.ResponseEntity;

import javax.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Collectors;

public class HandleValidation {
    private HandleValidation(){}
    public static ResponseEntity<ResErrorDto> handleValidationException(ConstraintViolationException ex, String statusError){
        List<ErrorDto> listErrorDto = ex.getConstraintViolations().stream()
                .map(msg ->
                        ErrorDto.builder()
                                .message(msg.getMessage())
                                .property(msg.getPropertyPath()!= null ? msg.getPropertyPath().toString() : "")
                                .code(msg.getConstraintDescriptor()!= null ? msg.getConstraintDescriptor()
                                        .getAnnotation()
                                        .getClass()
                                        .getSimpleName() : "")
                                .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ResErrorDto.builder()
                        .status(statusError)
                        .errorDtoList(listErrorDto)
                        .build());
    }

    public static ResponseEntity<ResErrorDto> handleValidationException(PnValidationException ex, String statusError){
        List<ErrorDto> listErrorDto = ex.getValidationErrors().stream()
                .map(msg ->
                        ErrorDto.builder()
                                .message(msg.getMessage())
                                .property(msg.getPropertyPath()!= null ? msg.getPropertyPath().toString() : "")
                                .code(msg.getConstraintDescriptor()!= null ? msg.getConstraintDescriptor()
                                        .getAnnotation()
                                        .getClass()
                                        .getSimpleName() : "")
                                .build()
                )
                .collect(Collectors.toList());

        return ResponseEntity.badRequest()
                .body(ResErrorDto.builder()
                        .identificationId(ex.getValidationTargetId())
                        .status(statusError)
                        .errorDtoList(listErrorDto)
                        .build());
    }
}