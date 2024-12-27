package ci.ivb.libraryeventproducer.integration;

import ci.ivb.libraryeventproducer.domain.LibraryEvent;
import ci.ivb.libraryeventproducer.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(topics = {"library-events"}, partitions = 3)
@TestPropertySource(properties = {"spring.kafka.producer.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.admin.properties.bootstrap.servers=${spring.embedded.kafka.brokers}"})
public class LibraryEventControllerIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    EmbeddedKafkaBroker embeddedKafkaBroker;
    @Autowired
    ObjectMapper objectMapper;

    private Consumer<Integer, String> consumer;

    @Test
    void contextLoads() {
    }

    @BeforeEach
    void setup() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.consumerProps("group1", "true", embeddedKafkaBroker));
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        consumer = new DefaultKafkaConsumerFactory<>(configs, new IntegerDeserializer(), new StringDeserializer()).createConsumer();
        embeddedKafkaBroker.consumeFromAllEmbeddedTopics(consumer);
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    void postLibraryEventTest() {
        //given
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        var entity = new HttpEntity<>(TestUtil.libraryEventRecord(), headers);

        //when
        var responseEntity = testRestTemplate.exchange("/v1/libraryevent", HttpMethod.POST, entity, LibraryEvent.class);

        //then
        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());

        var consumerRecords = KafkaTestUtils.getRecords(consumer);
        assertEquals(1, consumerRecords.count());

        consumerRecords.forEach(consumerRecord -> {
            var expectedRecord = TestUtil.libraryEventRecord();
            var actualRecord = TestUtil.parseLibraryEventRecord(objectMapper, consumerRecord.value());
            assertEquals(expectedRecord.libraryEventId(), actualRecord.libraryEventId());
            assertEquals(expectedRecord.libraryEventType(), actualRecord.libraryEventType());
            assertEquals(expectedRecord.book(), actualRecord.book());
        });
    }
}
