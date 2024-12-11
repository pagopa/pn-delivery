package it.pagopa.pn.delivery.config;

import it.pagopa.pn.delivery.PnDeliveryConfigs;
import it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.api.F24ControllerApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.ApiClient;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.NotificationsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.datavault.v1.api.RecipientsApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.NotificationProcessCostApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.api.TimelineAndStatusApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.InternalOnlyApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.PaymentInfoApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.api.RootSenderIdApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.api.MandatePrivateServiceApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.api.AgenziaEntrateApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileDownloadApi;
import it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.api.FileUploadApi;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MsClientConfig {

    @Configuration
    static class BaseClients {

        @Bean
        @Primary
        RecipientsApi recipientsApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            ApiClient newApiClient = new ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );
            return new RecipientsApi(newApiClient);
        }

        @Bean
        @Primary
        NotificationsApi notificationsApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            ApiClient newApiClient = new ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getDataVaultBaseUrl() );
            return new NotificationsApi(newApiClient);
        }

        @Bean
        @Primary
        TimelineAndStatusApi timelineAndStatusApi(@Qualifier("withTracing")RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.ApiClient( restTemplate );
            newApiClient.setBasePath(cfg.getDeliveryPushBaseUrl());
            return new TimelineAndStatusApi(newApiClient);
        }

        @Bean
        @Primary
        NotificationProcessCostApi notificationProcessCostApi(@Qualifier("withTracing")RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.ApiClient processCostApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.deliverypush.v1.ApiClient(  restTemplate );
            processCostApiClient.setBasePath( cfg.getDeliveryPushBaseUrl() );
            return new NotificationProcessCostApi(processCostApiClient);
        }

        @Bean
        @Primary
        PaymentInfoApi paymentInfoApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getExternalRegistriesBaseUrl() );
            return new PaymentInfoApi(newApiClient);
        }

        @Bean
        @Primary
        InternalOnlyApi internalOnlyApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getExternalRegistriesBaseUrl() );
            return new InternalOnlyApi(newApiClient);
        }

        @Bean
        @Primary
        RootSenderIdApi rootSenderIdApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.externalregistries.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getExternalRegistriesBaseUrl() );
            return new RootSenderIdApi(newApiClient);
        }

        @Bean
        @Primary
        MandatePrivateServiceApi mandatePrivateServiceApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.mandate.v1.ApiClient(restTemplate);
            newApiClient.setBasePath(cfg.getMandateBaseUrl());
            return new MandatePrivateServiceApi(newApiClient);
        }

        @Bean
        @Primary
        F24ControllerApi f24ControllerApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.F24.v1.ApiClient(restTemplate);
            newApiClient.setBasePath(cfg.getF24BaseUrl());
            return new F24ControllerApi(newApiClient);
        }

        @Bean
        @Primary
        FileDownloadApi fileDownloadApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
            return new FileDownloadApi(newApiClient);
        }

        @Bean
        @Primary
        FileUploadApi fileUploadApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.safestorage.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getSafeStorageBaseUrl() );
            return new FileUploadApi(newApiClient);
        }

        @Bean
        @Primary
        AgenziaEntrateApi agenziaEntrateApi(@Qualifier("withTracing") RestTemplate restTemplate, PnDeliveryConfigs cfg) {
            it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.ApiClient newApiClient = new it.pagopa.pn.delivery.generated.openapi.msclient.nationalregistries.v1.ApiClient( restTemplate );
            newApiClient.setBasePath( cfg.getNationalRegistriesBaseUrl() );
            return new AgenziaEntrateApi(newApiClient);
        }
    }
}
