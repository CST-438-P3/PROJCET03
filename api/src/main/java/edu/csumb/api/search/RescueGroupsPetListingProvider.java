package edu.csumb.api.search;

import com.fasterxml.jackson.databind.JsonNode;
import edu.csumb.api.post.Species;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** Live Rescue Groups adapter. Keep the API key on the server, never in Expo. */
@Component
@Profile("pet-provider")
public class RescueGroupsPetListingProvider implements PetListingProvider {
    private final RestClient client;
    public RescueGroupsPetListingProvider(
            @Value("${app.rescue-groups.base-url:https://api.rescuegroups.org/v5}") String baseUrl,
            @Value("${app.rescue-groups.api-key:}") String apiKey) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("RESCUE_GROUPS_API_KEY must be set when pet-provider is active");
        }
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", apiKey)
                .defaultHeader("Content-Type", "application/vnd.api+json")
                .build();
    }

    @Override
    public List<PetListing> findAvailablePets() {
        return List.of();
    }

    @Override
    public List<PetDistance> search(String zip, int distanceMiles, Species species,
                                    ZipCoordinateService ignoredZipCoordinates) {
        Map<String, Object> data = new HashMap<>();
        data.put("geodistance", Map.of("postalcode", zip, "miles", distanceMiles));
        if (species != null) {
            data.put("filters", List.of(Map.of(
                    "fieldName", "species.singular",
                    "operation", "equal",
                    "criteria", species == Species.CAT ? "Cat" : "Dog")));
        }

        JsonNode response = client.post()
                .uri("/public/animals/search/available?page=1&limit=250&include=orgs,pictures,species")
                .body(Map.of("data", data))
                .retrieve()
                .body(JsonNode.class);
        if (response == null || !response.has("data")) {
            throw new IllegalArgumentException("Rescue Groups returned an invalid response");
        }

        Map<String, JsonNode> included = new HashMap<>();
        response.path("included").forEach(item -> included.put(item.path("type").asText() + ":" + item.path("id").asText(), item));
        List<PetDistance> pets = new java.util.ArrayList<>();
        response.path("data").forEach(item -> {
            JsonNode attributes = item.path("attributes");
            JsonNode orgRef = item.path("relationships").path("orgs").path("data");
            JsonNode org = included.get("orgs:" + orgRef.path("id").asText());
            JsonNode speciesRef = item.path("relationships").path("species").path("data");
            JsonNode speciesNode = included.get("species:" + speciesRef.path("id").asText());
            String speciesName = speciesNode == null ? "" : speciesNode.path("attributes").path("singular").asText(
                    speciesNode.path("attributes").path("name").asText());
            Species normalizedSpecies = "Cat".equalsIgnoreCase(speciesName) ? Species.CAT : Species.DOG;
            String shelterName = org == null ? "Rescue Groups shelter" : org.path("attributes").path("name").asText("Rescue Groups shelter");
            long shelterId = orgRef.path("id").asLong(0);
            double distance = attributes.path("locationDistance").asDouble(0);
            String photoUrl = firstPictureUrl(item, included);
            PetListing pet = new PetListing(
                    item.path("id").asLong(), shelterId, shelterName, "", attributes.path("name").asText("Unnamed pet"),
                    normalizedSpecies, null, attributes.path("descriptionText").asText(null), photoUrl, 0, 0);
            pets.add(new PetDistance(pet, distance));
        });
        return pets;
    }

    private String firstPictureUrl(JsonNode animal, Map<String, JsonNode> included) {
        JsonNode pictures = animal.path("relationships").path("pictures").path("data");
        if (!pictures.isArray() || pictures.isEmpty()) return null;
        JsonNode picture = included.get("pictures:" + pictures.get(0).path("id").asText());
        if (picture == null) return null;
        JsonNode attributes = picture.path("attributes");
        String url = attributes.path("largeUrl").asText(null);
        return url == null ? attributes.path("originalUrl").asText(null) : url;
    }
}
