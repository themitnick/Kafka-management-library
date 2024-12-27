package ci.ivb.libraryeventproducer.unit;

import ci.ivb.libraryeventproducer.controller.LibraryEventController;
import ci.ivb.libraryeventproducer.producer.LibraryEventProducer;
import ci.ivb.libraryeventproducer.util.TestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LibraryEventController.class)
public class LibraryEventControllerTest {

    @Autowired
    MockMvc mockMvc;
    @Autowired
    ObjectMapper objectMapper;
    @MockitoBean
    LibraryEventProducer libraryEventProducer;


    @Test
    void postLibraryEventTest() throws Exception {
        // given
        RequestBuilder requestBuilder = MockMvcRequestBuilders.post("/v1/libraryevent")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(TestUtil.libraryEventRecord()));

        when(libraryEventProducer.sendLibraryEvent(TestUtil.libraryEventRecord())).thenReturn(null);

        // when
        MvcResult mvcResult = mockMvc.perform(requestBuilder).andExpect(status().isCreated()).andReturn();
    }
}
