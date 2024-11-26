package it.pagopa.pn.delivery.middleware.notificationdao;

import it.pagopa.pn.delivery.models.TaxonomyCodeDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
public class TaxonomyCodeDaoDynamoTest {
    public static final String TAXONOMY_CODE_KEY_EXISTING = "010201P";
    public static final String PA_ID_EXISTING = "01";

    @MockBean
    private TaxonomyCodeDaoDynamo taxonomyCodeDaoDynamo;

    @Test
    void testGetTaxonomyCodeByKeyAndPaId_shouldReturnTaxonomyCodeWhenExists() {
        TaxonomyCodeDto taxonomyCodeDto = new TaxonomyCodeDto();
        taxonomyCodeDto.setKey(TAXONOMY_CODE_KEY_EXISTING);
        taxonomyCodeDto.setPaId(PA_ID_EXISTING);

        when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(Mockito.eq(TAXONOMY_CODE_KEY_EXISTING), Mockito.eq(PA_ID_EXISTING)))
                .thenReturn(Optional.of(taxonomyCodeDto));

        Optional<TaxonomyCodeDto> result = taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(TAXONOMY_CODE_KEY_EXISTING, PA_ID_EXISTING);
        assertEquals(Optional.of(taxonomyCodeDto), result);

        verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(Mockito.eq(TAXONOMY_CODE_KEY_EXISTING), Mockito.eq(PA_ID_EXISTING));
    }

    @Test
    void testGetTaxonomyCodeByKeyAndPaId_notFound() {
        String key = "nonExistingKey";
        String paId = "nonExistingPaId";
        when(taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(any(), any())).thenReturn(Optional.empty());
        assertEquals(Optional.empty(), taxonomyCodeDaoDynamo.getTaxonomyCodeByKeyAndPaId(key, paId));
        verify(taxonomyCodeDaoDynamo).getTaxonomyCodeByKeyAndPaId(any(), any());
    }

    @Test
    void testGetTaxonomyCodeByKeyAndPaId_shouldThrowExceptionOnDbConnectionFailure() {
        TaxonomyCodeDaoDynamo taxonomyCodeDaoDynamoMock = Mockito.mock(TaxonomyCodeDaoDynamo.class);

        // Simulate a database connection failure by throwing an exception
        when(taxonomyCodeDaoDynamoMock.getTaxonomyCodeByKeyAndPaId(any(), any()))
                .thenThrow(new RuntimeException("Connection to database failed"));

        // Verify that the exception is thrown
        assertThatThrownBy(() -> taxonomyCodeDaoDynamoMock.getTaxonomyCodeByKeyAndPaId(TAXONOMY_CODE_KEY_EXISTING, PA_ID_EXISTING))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection to database failed");
    }
}
