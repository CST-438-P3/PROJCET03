package edu.csumb.api.shelter;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A shelter as the API returns it, the Shelter schema in openapi.yaml; a missing phone or email is left out. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ShelterResponse(long id, String name, String address, String zip, String phone, String email) {

    /** Builds the response for a saved shelter. */
    public static ShelterResponse from(Shelter shelter) {
        return new ShelterResponse(shelter.getId(), shelter.getName(), shelter.getAddress(), shelter.getZip(),
            shelter.getPhone(), shelter.getEmail());
    }
}
