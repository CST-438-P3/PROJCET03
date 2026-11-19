package edu.csumb.api.shelter;

import com.fasterxml.jackson.annotation.JsonInclude;

/** A user who is staff for a shelter, the StaffMember schema in openapi.yaml; a missing display name is left out. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record StaffMember(long id, String email, String displayName) {
}
