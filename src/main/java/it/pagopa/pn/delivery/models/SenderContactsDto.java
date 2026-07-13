package it.pagopa.pn.delivery.models;

import lombok.*;

import javax.annotation.Nonnull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SenderContactsDto {
    @Nonnull
    String senderId;
    String site;
    String email;
    String pec;
    String phone;
}
