/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Base Software Module that is supported by OS level provisioning mechanism on the edge controller, e.g. OS, JVM, AgentHub.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
@ToString(callSuper = true)
@Entity
@Table(name = "sp_software_module")
@NamedEntityGraph(name = "SoftwareModule.artifacts", attributeNodes = { @NamedAttributeNode("artifacts") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModule
        extends AbstractJpaNamedVersionedEntity
        implements SoftwareModule, WithMetadata<MetadataValue, JpaSoftwareModule.JpaMetadataValue>, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne
    @JoinColumn(name = "sm_type", nullable = false, updatable = false)
    @NotNull
    private JpaSoftwareModuleType type;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "softwareModule",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            targetEntity = JpaArtifact.class, orphanRemoval = true)
    private List<JpaArtifact> artifacts;

    @Column(name = "vendor", length = SoftwareModule.VENDOR_MAX_SIZE)
    @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
    private String vendor;

    @Column(name = "encrypted")
    private boolean encrypted;

    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent.
    @ElementCollection
    @CollectionTable(
            name = "sp_sm_metadata",
            joinColumns = { @JoinColumn(name = "sm", nullable = false) })
    @MapKeyColumn(name = "meta_key", length = SoftwareModule.METADATA_KEY_MAX_SIZE)
    private Map<String, JpaMetadataValue> metadata;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "deleted")
    private boolean deleted;

    @ToString.Exclude
    @ManyToMany(mappedBy = "modules", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedTo;

    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version) {
        super(name, version, null);
        this.type = (JpaSoftwareModuleType) type;
    }

    @Override
    public List<Artifact> getArtifacts() {
        return artifacts == null ? Collections.emptyList() : Collections.unmodifiableList(artifacts);
    }

    @Override
    public boolean isComplete() {
        return Optional.ofNullable(type).map(smType -> {
            if (smType.getMinArtifacts() > 0) {
                return getArtifacts().size() >= smType.getMinArtifacts();
            } else {
                return true;
            }
        }).orElse(true);
    }

    public void addArtifact(final JpaArtifact artifact) {
        if (isLocked()) {
            throw new LockedException(JpaSoftwareModule.class, getId(), "ADD_ARTIFACT");
        }
        if (artifacts == null) {
            artifacts = new ArrayList<>(4);
            artifacts.add(artifact);
        } else if (!artifacts.contains(artifact)) {
            artifacts.add(artifact);
        }
    }

    public void removeArtifact(final Artifact artifact) {
        if (isLocked()) {
            throw new LockedException(JpaSoftwareModule.class, getId(), "REMOVE_ARTIFACT");
        }

        if (artifacts != null) {
            artifacts.remove(artifact);
        }
    }

    public void lock() {
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleUpdatedEvent(this));

        if (deleted) {
            EventPublisherHolder.getInstance().getEventPublisher()
                    .publishEvent(new SoftwareModuleDeletedEvent(getTenant(), getId(), getClass()));
        }
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(getTenant(), getId(), getClass()));
    }

    @Data
    @Embeddable
    public static class JpaMetadataValue implements MetadataValue, Serializable {

        @Serial
        private static final long serialVersionUID = 1L;

        @Column(name = "meta_value", length = SoftwareModule.METADATA_VALUE_MAX_SIZE)
        @Size(max = METADATA_VALUE_MAX_SIZE)
        private String value;
        @Column(name = "target_visible")
        private boolean targetVisible;
    }
}