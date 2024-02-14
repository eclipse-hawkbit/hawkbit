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

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.remote.SoftwareModuleDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.SoftwareModuleUpdatedEvent;
import org.eclipse.hawkbit.repository.model.Artifact;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;

/**
 * Base Software Module that is supported by OS level provisioning mechanism on
 * the edge controller, e.g. OS, JVM, AgentHub.
 */
@NoArgsConstructor // Default constructor for JPA
@Getter
@ToString(callSuper = true)
@Entity
@Table(name = "sp_base_software_module", uniqueConstraints = @UniqueConstraint(columnNames = { "module_type", "name",
        "version", "tenant" }, name = "uk_base_sw_mod"), indexes = {
                @Index(name = "sp_idx_base_sw_module_01", columnList = "tenant,deleted,name,version"),
                @Index(name = "sp_idx_base_sw_module_02", columnList = "tenant,deleted,module_type"),
                @Index(name = "sp_idx_base_sw_module_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "SoftwareModule.artifacts", attributeNodes = { @NamedAttributeNode("artifacts") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaSoftwareModule extends AbstractJpaNamedVersionedEntity implements SoftwareModule, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @ManyToOne
    @JoinColumn(name = "module_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_module_type"))
    @NotNull
    private JpaSoftwareModuleType type;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "softwareModule", cascade = {
            CascadeType.PERSIST }, targetEntity = JpaArtifact.class, orphanRemoval = true)
    private List<JpaArtifact> artifacts;

    @Column(name = "vendor", nullable = true, length = SoftwareModule.VENDOR_MAX_SIZE)
    @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
    private String vendor;

    @ToString.Exclude
    @CascadeOnDelete
    @OneToMany(mappedBy = "softwareModule", fetch = FetchType.LAZY, targetEntity = JpaSoftwareModuleMetadata.class)
    private List<JpaSoftwareModuleMetadata> metadata;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "encrypted")
    private boolean encrypted;

    @ToString.Exclude
    @Getter(AccessLevel.NONE)
    @CascadeOnDelete
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

    public void setType(final JpaSoftwareModuleType type) {
        this.type = type;
    }

    @Override
    public List<Artifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    public void addArtifact(final Artifact artifact) {
        if (artifacts == null) {
            artifacts = new ArrayList<>(4);
            artifacts.add((JpaArtifact) artifact);
            return;
        }

        if (!artifacts.contains(artifact)) {
            artifacts.add((JpaArtifact) artifact);
        }
    }

    /**
     * @param artifact is removed from the assigned {@link Artifact}s.
     */
    public void removeArtifact(final Artifact artifact) {
        if (artifacts != null) {
            artifacts.remove(artifact);
        }
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    public void lock() {
        locked = true;
    }

    /**
     * Marks or un-marks this software module as deleted.
     * 
     * @param deleted
     *            {@code true} if the software module should be marked as deleted
     *            otherwise {@code false}
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Marks this software module as encrypted.
     * 
     * @param encrypted
     *            {@code true} if the software module should be marked as encrypted
     *            otherwise {@code false}
     */
    public void setEncrypted(final boolean encrypted) {
        this.encrypted = encrypted;
    }

    @Override
    public List<DistributionSet> getAssignedTo() {
        if (assignedTo == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(assignedTo);
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new SoftwareModuleUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));

        if (isSoftDeleted(descriptorEvent)) {
            EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(
                    getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(getTenant(),
                getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(DirectToFieldChangeRecord.class::isInstance).map(DirectToFieldChangeRecord.class::cast)
                .toList();

        return changes.stream().anyMatch(changeRecord -> DELETED_PROPERTY.equals(changeRecord.getAttribute())
                && Boolean.parseBoolean(changeRecord.getNewValue().toString()));
    }
}