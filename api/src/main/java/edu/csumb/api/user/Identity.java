package edu.csumb.api.user;

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
import jakarta.persistence.Table;
import java.time.Instant;

/** One way a user signs in: Google's 'sub' or GitHub's numeric id, never the email. */
@Entity
@Table(name = "identities")
public class Identity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Provider provider;

    @Column(name = "provider_user_id", nullable = false, updatable = false)
    private String providerUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Identity() {
    }

    public Identity(User user, Provider provider, String providerUserId) {
        this.user = user;
        this.provider = provider;
        this.providerUserId = providerUserId;
    }

    public User getUser() {
        return user;
    }

    public Provider getProvider() {
        return provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }
}
