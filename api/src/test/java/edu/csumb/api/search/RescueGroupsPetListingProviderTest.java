package edu.csumb.api.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import edu.csumb.api.post.Species;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RescueGroupsPetListingProviderTest {
    @Test
    void mapsAnimalShelterSpeciesPhotoAndDistance() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://rescue.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://rescue.test/public/animals/search/available?page=1&limit=250&include=orgs,pictures,species"))
                .andRespond(withSuccess(response(), MediaType.APPLICATION_JSON));

        RescueGroupsPetListingProvider provider = new RescueGroupsPetListingProvider(builder.build());
        List<PetListingProvider.PetDistance> result = provider.search("93933", 10, null, new ZipCoordinateService());

        assertEquals(1, result.size());
        assertEquals("Luna", result.get(0).pet().name());
        assertEquals(Species.CAT, result.get(0).pet().species());
        assertEquals("Monterey Rescue", result.get(0).pet().shelterName());
        assertEquals("https://img.test/luna.jpg", result.get(0).pet().photoUrl());
        assertEquals(4.2, result.get(0).distanceMiles());
        server.verify();
    }

    private static String response() {
        return """
                {"data":[{"type":"animals","id":"42","attributes":{"name":"Luna","descriptionText":"Friendly","locationDistance":4.2},"relationships":{"orgs":{"data":{"type":"orgs","id":"7"}},"species":{"data":{"type":"species","id":"3"}},"pictures":{"data":[{"type":"pictures","id":"9"}]}}}],"included":[{"type":"orgs","id":"7","attributes":{"name":"Monterey Rescue"}},{"type":"species","id":"3","attributes":{"singular":"Cat"}},{"type":"pictures","id":"9","attributes":{"largeUrl":"https://img.test/luna.jpg"}}]}
                """;
    }
}
