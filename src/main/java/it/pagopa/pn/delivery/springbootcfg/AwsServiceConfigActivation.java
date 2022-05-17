package it.pagopa.pn.delivery.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AwsS3FileStorage;
import it.pagopa.pn.commons.configs.RuntimeMode;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsServiceConfigActivation {

    @Bean
    public AwsS3FileStorage awsS3FileStorage(S3Client client, AwsConfigs props) {
        return new AwsS3FileStorage(client, props, RuntimeMode.PROD);
    }
}
