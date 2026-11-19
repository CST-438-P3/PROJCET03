package edu.csumb.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.csumb.api.post.Species;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class NearbyControllerTest {
    @Test
    void returnsRequestedPageAndReportsTotals() throws Exception {
        PetListingProvider provider = () -> List.of(
                pet(1, "Luna"), pet(2, "Max"), pet(3, "Bella"));
        NearbyController controller = new NearbyController(new NearbyService(provider, new ZipCoordinateService()));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).build();

        String body = mvc.perform(get("/posts/nearby?zip=93933&distance=100&page=1&size=2"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        assertEquals(true, body.contains("\"page\":1"));
        assertEquals(true, body.contains("\"totalElements\":3"));
        assertEquals(true, body.contains("\"name\":\"Bella\""));
        assertEquals(false, body.contains("\"name\":\"Luna\""));
    }

    private static PetListing pet(long id, String name) {
        return new PetListing(id, id, "Shelter", "93933", name, Species.DOG, 12, "Dog", null, 36.7068, -121.6555);
    }
}
