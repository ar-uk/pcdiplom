package org.example.recommendationservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "ai_saved_build")
public class AiSavedBuild {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "user_id")
    private String userId;

    @Lob
    @Column(name = "prompt", nullable = false)
    private String prompt;

    @Column(name = "currency")
    private String currency;

    @Column(name = "region")
    private String region;

    @Column(name = "strict_budget", nullable = false)
    private boolean strictBudget;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "intent_json", nullable = false, columnDefinition = "jsonb")
    private String intentJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "build_json", nullable = false, columnDefinition = "jsonb")
    private String buildJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "totals_json", nullable = false, columnDefinition = "jsonb")
    private String totalsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "checks_json", nullable = false, columnDefinition = "jsonb")
    private String checksJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "reasoning_json", nullable = false, columnDefinition = "jsonb")
    private String reasoningJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "alternatives_json", nullable = false, columnDefinition = "jsonb")
    private String alternativesJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
