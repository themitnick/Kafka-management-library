package ci.ivb.libraryeventproducer.controller;

import ci.ivb.libraryeventproducer.domain.LibraryEvent;
import ci.ivb.libraryeventproducer.producer.LibraryEventProducer;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping()
public class LibraryEventController {

    private static final Logger log = LoggerFactory.getLogger(LibraryEventController.class);

    private final LibraryEventProducer libraryEventProducer;

    public LibraryEventController(LibraryEventProducer libraryEventProducer) {
        this.libraryEventProducer = libraryEventProducer;
    }

    @PostMapping("/v1/libraryevent")
    public ResponseEntity<LibraryEvent> postLibraryEvent(@RequestBody LibraryEvent libraryEvent) throws JsonProcessingException {
        // invoke kafka producer
        log.info("before sendLibraryEvent {}", libraryEvent);
        libraryEventProducer.sendLibraryEvent(libraryEvent);
        return ResponseEntity.status(HttpStatus.CREATED).body(libraryEvent);
    }
}
