package it.pagopa.pn.delivery.models;

import lombok.*;

import java.util.Map;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class TaxonomyCodeDto {
    private String key;
    private String paId;
    private Map<String,Object> description;
}
