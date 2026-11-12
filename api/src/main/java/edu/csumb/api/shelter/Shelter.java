package edu.csumb.api.shelter;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A shelter in the shelters table. Only the columns posts need are mapped for now; S10 adds the rest. */
@Entity
@Table(name = "shelters")
public class Shelter {

    @Id
    private Long id;

    private String name;

    /** For JPA only. */
    protected Shelter() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
