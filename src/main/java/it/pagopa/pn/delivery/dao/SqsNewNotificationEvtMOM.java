package it.pagopa.pn.delivery.dao;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.delivery.model.events.NewNotificationEvt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ConditionalOnProperty( name="pn.mom", havingValue = "sqs")
@Component
public class SqsNewNotificationEvtMOM implements NewNotificationEvtMOM {

    private final String queueName = "new_notification_evt";
    private final SqsAsyncClient sqs;

    private final ObjectMapper objMapper;
    private final String queueUrl;


    public SqsNewNotificationEvtMOM(SqsAsyncClient sqs, ObjectMapper objMapper) {
        this.sqs = sqs;
        this.objMapper = objMapper;
        queueUrl = getQueueUrl( sqs );
    }

    private String getQueueUrl(SqsAsyncClient sqs) {
        GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();

        try {
            return sqs.getQueueUrl(getQueueRequest).get().queueUrl();
        } catch (InterruptedException | ExecutionException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    @Override
    public synchronized CompletableFuture<List<NewNotificationEvt>> poll(Duration maxPollTime) {
        ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();

        return sqs
                .receiveMessage(receiveRequest)
                .thenApply( (sqsMessages) ->
                        sqsMessages
                                .messages()
                                .stream()
                                .map( awsMsg -> {
                                    NewNotificationEvt evt = parseJson( awsMsg.body() );
                                    deleteMessage( awsMsg );
                                    return evt;
                                } )
                                .collect(Collectors.toList())
            );
    }

    private void deleteMessage(Message awsMsg) {
        DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle( awsMsg.receiptHandle())
                .build();
        try {
            sqs.deleteMessage(deleteMessageRequest).get();
        } catch (ExecutionException | InterruptedException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    private NewNotificationEvt parseJson(String body) {
        try {
            return objMapper.readValue( body, NewNotificationEvt.class );
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }

    @Override
    public synchronized CompletableFuture<Void> push(NewNotificationEvt msg) {
        String jsonMessage = objToJson_handleExceptions(msg);

        SendMessageRequest sendMsgRequest = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody( jsonMessage )
                .build();

        return sqs.sendMessage(sendMsgRequest).thenApply( (r) -> null);
    }

    private String objToJson_handleExceptions(NewNotificationEvt msg) {
        try {
            return objMapper.writeValueAsString(msg);
        } catch (JsonProcessingException exc) {
            throw new IllegalStateException( exc ); // FIXME Definre trattazione eccezioni
        }
    }
}
