package edu.csumb.api.shelter;

import java.util.List;

/** One page of shelters, the ShelterPage schema in openapi.yaml. page starts at 0. */
public record ShelterPage(int page, int size, long totalElements, int totalPages, List<ShelterResponse> content) {
}
