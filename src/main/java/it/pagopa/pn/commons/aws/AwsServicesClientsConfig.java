package it.pagopa.pn.commons.aws;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

@Configuration
@ConditionalOnProperty( name = "pn.mom", havingValue = "sqs" )
public class AwsServicesClientsConfig {

    private final AwsConfigs props;

    public AwsServicesClientsConfig(AwsConfigs props ) {
        this.props = props;
    }

    /*@Bean
    public DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient() {
        return DynamoDbEnhancedAsyncClient.builder()
                .dynamoDbClient(
                        configureBuilder( DynamoDbAsyncClient.builder() )
                    )
                .build();
    }*/

    @Bean
    public SqsAsyncClient dynamoDbEnhancedAsyncClient() {
        return configureBuilder(
                  SqsAsyncClient.builder()
            );
    }

    private <C> C configureBuilder(AwsClientBuilder<?, C> builder) {
        if( props != null ) {

            String profileName = props.getProfileName();
            if( StringUtils.isNotBlank( profileName ) ) {
                builder.credentialsProvider( ProfileCredentialsProvider.create( profileName ));
            }

            String regionCode = props.getRegionCode();
            if( StringUtils.isNotBlank( regionCode )) {
                builder.region( Region.of( regionCode ));
            }
        }

        return builder.build();
    }


}
