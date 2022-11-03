package it.pagopa.pn.delivery.utils;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PreloadRequest {
    private String key;
    private String contentType;
}
