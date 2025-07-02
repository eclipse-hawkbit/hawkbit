/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AccessLevel;
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
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * Base Software Module that is supported by OS level provisioning mechanism on the edge controller, e.g. OS, JVM, AgentHub.
 */
@NoArgsConstructor // Default constructor for JPA
@Setter
@Getter
@ToString(callSuper = true)
@Entity
@Table(name = "sp_software_module",
        uniqueConstraints = @UniqueConstraint(columnNames = { "sm_type", "name", "version", "tenant" }, name = "uk_software_module"),
        indexes = {
                @Index(name = "sp_idx_software_module_01", columnList = "tenant,deleted,name,version"),
                @Index(name = "sp_idx_software_module_02", columnList = "tenant,deleted,sm_type"),
                @Index(name = "sp_idx_software_module_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "SoftwareModule.artifacts", attributeNodes = { @NamedAttributeNode("artifacts") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModule extends AbstractJpaNamedVersionedEntity implements SoftwareModule, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter
    @ManyToOne
    @JoinColumn(name = "sm_type", nullable = false, updatable = false,
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_software_module_sm_type"))
    @NotNull
    private JpaSoftwareModuleType type;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "softwareModule",
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
            targetEntity = JpaArtifact.class, orphanRemoval = true)
    private List<JpaArtifact> artifacts;

    @Setter
    @Column(name = "vendor", length = SoftwareModule.VENDOR_MAX_SIZE)
    @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
    private String vendor;

    @Setter
    @Column(name = "encrypted")
    private boolean encrypted;

    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent.
    @Getter
    @ElementCollection
    @CollectionTable(
            name = "sp_sm_metadata",
            joinColumns = { @JoinColumn(name = "sm", nullable = false) },
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_sm_metadata_sm"))
    @MapKeyColumn(name = "meta_key", length = SoftwareModule.METADATA_KEY_MAX_SIZE)
    @Column(name = "meta_value", length = SoftwareModule.METADATA_VALUE_MAX_SIZE)
    private Map<String, String> metadata;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "deleted")
    private boolean deleted;

    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    @ManyToMany(mappedBy = "modules", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedTo;

    /**
     * Parameterized constructor.
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version) {
        this(type, name, version, null, null, false);
    }

    /**
     * Parameterized constructor.
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version,
            final String description, final String vendor, final boolean encrypted) {
        super(name, version, description);
        this.vendor = vendor;
        this.type = (JpaSoftwareModuleType) type;
        this.encrypted = encrypted;
    }

    @Override
    public List<Artifact> getArtifacts() {
        return artifacts == null ? Collections.emptyList() : Collections.unmodifiableList(artifacts);
    }

    public void addArtifact(final Artifact artifact) {
        if (isLocked()) {
            throw new LockedException(JpaSoftwareModule.class, getId(), "ADD_ARTIFACT");
        }

        if (artifact instanceof JpaArtifact jpaArtifact) {
            if (artifacts == null) {
                artifacts = new ArrayList<>(4);
                artifacts.add(jpaArtifact);
            } else if (!artifacts.contains(jpaArtifact)) {
                artifacts.add(jpaArtifact);
            }
        } else {
            throw new UnsupportedOperationException("Only JpaArtifact is supported");
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

    public void setDeleted(final boolean deleted) {
        if (assignedTo != null) {
            final List<DistributionSet> lockedDS = assignedTo.stream()
                    .filter(DistributionSet::isLocked)
                    .filter(ds -> !ds.isDeleted())
                    .toList();
            if (!lockedDS.isEmpty()) {
                final StringBuilder sb = new StringBuilder("Part of ");
                if (lockedDS.size() == 1) {
                    sb.append("a locked distribution set: ");
                } else {
                    sb.append(lockedDS.size()).append(" locked distribution sets: ");
                }
                for (final DistributionSet ds : lockedDS) {
                    sb.append(ds.getName()).append(":").append(ds.getVersion()).append(" (").append(ds.getId()).append("), ");
                }
                sb.delete(sb.length() - 2, sb.length());
                throw new LockedException(JpaSoftwareModule.class, getId(), "DELETE", sb.toString());
            }
        }
        this.deleted = deleted;
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
}