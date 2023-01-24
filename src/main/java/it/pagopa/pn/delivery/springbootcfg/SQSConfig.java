package it.pagopa.pn.delivery.springbootcfg;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.AmazonSQSAsyncClientBuilder;
import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SQSConfig {

    private final AwsConfigs awsConfigs;

    public SQSConfig(AwsConfigs awsConfigs) {
        this.awsConfigs = awsConfigs;
    }

    @Bean
    public AmazonSQSAsync amazonSQS() {
        return AmazonSQSAsyncClientBuilder.standard()
                .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                .withRegion(awsConfigs.getRegionCode())
                .build();
    }

}
