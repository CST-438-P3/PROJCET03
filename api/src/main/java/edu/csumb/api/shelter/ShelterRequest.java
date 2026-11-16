package edu.csumb.api.shelter;

/** The body of POST and PUT /admin/shelters, the ShelterCreate schema in openapi.yaml. ShelterRules checks it. */
public record ShelterRequest(String name, String address, String zip, String phone, String email) {
}
