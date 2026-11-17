package edu.csumb.api.shelter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** A shelter in the shelters table (docs/erd.md). created_at is set by the database. */
@Entity
@Table(name = "shelters")
public class Shelter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String address;

    @Column(name = "zip_code")
    private String zip;

    private String phone;

    private String email;

    /** For JPA only. */
    protected Shelter() {
    }

    /** Creates a new shelter with values that ShelterRules already checked (S10). */
    public Shelter(String name, String address, String zip, String phone, String email) {
        replace(name, address, zip, phone, email);
    }

    /** Replaces every field, with values that ShelterRules already checked (S10). */
    public void replace(String name, String address, String zip, String phone, String email) {
        this.name = name;
        this.address = address;
        this.zip = zip;
        this.phone = phone;
        this.email = email;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public String getZip() {
        return zip;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }
}
