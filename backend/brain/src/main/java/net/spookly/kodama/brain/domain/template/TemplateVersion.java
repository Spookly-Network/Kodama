package net.spookly.kodama.brain.domain.template;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "template_versions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TemplateVersion {

    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private Template template;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String checksum;

    @Column(nullable = false)
    private String s3Key;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public TemplateVersion(
            Template template,
            String version,
            String checksum,
            String s3Key,
            String metadataJson,
            OffsetDateTime createdAt) {
        this.template = template;
        this.version = version;
        this.checksum = checksum;
        this.s3Key = s3Key;
        this.metadataJson = metadataJson;
        this.createdAt = createdAt;
    }
}
