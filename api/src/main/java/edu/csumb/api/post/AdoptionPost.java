package edu.csumb.api.post;

import edu.csumb.api.shelter.Shelter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

/** An adoption post in the adoption_posts table (docs/erd.md). */
@Entity
@Table(name = "adoption_posts")
public class AdoptionPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shelter_id")
    private Shelter shelter;

    private String name;

    @Enumerated(EnumType.STRING)
    private Species species;

    private String breed;

    @Enumerated(EnumType.STRING)
    private Sex sex;

    @Enumerated(EnumType.STRING)
    private Size size;

    @Column(name = "age_months")
    private Integer ageMonths;

    private String description;

    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    private PostStatus status;

    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    /** For JPA only. */
    protected AdoptionPost() {
    }

    /** Creates a new AVAILABLE post with values that PostRules already checked. */
    public AdoptionPost(Shelter shelter, String name, Species species, String breed, Sex sex, Size size,
            Integer ageMonths, String description, String photoUrl) {
        this.shelter = shelter;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.sex = sex;
        this.size = size;
        this.ageMonths = ageMonths;
        this.description = description;
        this.photoUrl = photoUrl;
        this.status = PostStatus.AVAILABLE;
    }

    /**
     * Sets the creation time when the post is first saved, in UTC and cut to microseconds like Postgres returns
     * it, so the create response shows the same time as later reads. updated_at is kept by a database trigger.
     */
    @PrePersist
    void onCreate() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() {
        return id;
    }

    public Shelter getShelter() {
        return shelter;
    }

    public String getName() {
        return name;
    }

    public Species getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public Sex getSex() {
        return sex;
    }

    public Size getSize() {
        return size;
    }

    public Integer getAgeMonths() {
        return ageMonths;
    }

    public String getDescription() {
        return description;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public PostStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
