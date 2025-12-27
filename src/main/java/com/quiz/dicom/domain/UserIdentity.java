package com.quiz.dicom.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "user_identity",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_user_identity_provider_sub", columnNames = {"provider", "provider_subject"})
        },
        indexes = {
                @Index(name = "ix_user_identity_user_id", columnList = "user_id")
        }
)
public class UserIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Many identities can belong to one user
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "provider", length = 20, nullable = false)
    private String provider; // "google" or "linkedin" (enforced by DB CHECK)

    @Column(name = "provider_subject", length = 255, nullable = false)
    private String providerSubject; // OIDC "sub"

    @Column(name = "email", length = 200)
    private String email;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "display_name", length = 200)
    private String displayName;

    @Column(name = "picture_url")
    private String pictureUrl;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // getters/setters

    public Long getId() { return id; }

    public AppUser getUser() { return user; }
    public void setUser(AppUser user) { this.user = user; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderSubject() { return providerSubject; }
    public void setProviderSubject(String providerSubject) { this.providerSubject = providerSubject; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public Boolean getEmailVerified() { return emailVerified; }
    public void setEmailVerified(Boolean emailVerified) { this.emailVerified = emailVerified; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getPictureUrl() { return pictureUrl; }
    public void setPictureUrl(String pictureUrl) { this.pictureUrl = pictureUrl; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(OffsetDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }
}
