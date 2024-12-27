package ci.ivb.libraryeventproducer.producer;

import ci.ivb.libraryeventproducer.domain.LibraryEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class LibraryEventProducer {

    private static final Logger log = LoggerFactory.getLogger(LibraryEventProducer.class);

    @Value("${spring.kafka.template.default-topic}")
    private String topicName;

    private final KafkaTemplate<Integer, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public LibraryEventProducer(KafkaTemplate<Integer, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        //1. Blocking call - send metadata about the kafka message
        //2. Send message happens - Return a CompletableFuture
        var completableFuture =  kafkaTemplate.send(topicName, key, value);

        return completableFuture.whenComplete(
                (sendResult, throwable) ->{
                    if(throwable == null) {
                        // success
                        handleSuccess(key, value, sendResult);
                    } else {
                        // failure
                        handleFailure(key, value, throwable);
                    }
                }
        );
    }

    public SendResult<Integer, String> sendLibraryEvent_approach2(LibraryEvent libraryEvent) throws JsonProcessingException, ExecutionException, InterruptedException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        //1. Blocking call - send metadata about the kafka message
        //2. Bloc and wait until the message is sent to the kafka topic
        var sendResult =  kafkaTemplate.send(topicName, key, value).get();
        handleSuccess(key, value, sendResult);
        return sendResult;
    }

    public CompletableFuture<SendResult<Integer, String>> sendLibraryEvent_approach3(LibraryEvent libraryEvent) throws JsonProcessingException {
        Integer key = libraryEvent.libraryEventId();
        String value = objectMapper.writeValueAsString(libraryEvent);

        ProducerRecord<Integer, String> producerRecord = buildProducerRecord(key, value);

        //1. Blocking call - send metadata about the kafka message
        //2. Send message happens - Return a CompletableFuture
        var completableFuture =  kafkaTemplate.send(producerRecord);

        return completableFuture.whenComplete(
                (sendResult, throwable) ->{
                    if(throwable == null) {
                        // success
                        handleSuccess(key, value, sendResult);
                    } else {
                        // failure
                        handleFailure(key, value, throwable);
                    }
                }
        );
    }

    private ProducerRecord buildProducerRecord(Integer key, String value) {
        Headers recordHeaders = new RecordHeaders();
        recordHeaders.add("event-source", "scanner".getBytes());

        return new ProducerRecord(topicName, null, key, value, recordHeaders);
    }

    private void handleFailure(Integer key, String value, Throwable throwable) {
        log.error("Error sending the message and the exception is {}", throwable.getMessage());
    }

    private void handleSuccess(Integer key, String value, SendResult<Integer, String> sendResult) {
        log.info("Message sent successfully for the key: {} and the value is {}, partition is {}", key, value, sendResult.getRecordMetadata().partition());
    }
}
