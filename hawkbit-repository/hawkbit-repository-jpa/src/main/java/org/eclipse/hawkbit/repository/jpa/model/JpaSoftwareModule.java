/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

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
 *
 */
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
    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @ManyToOne
    @JoinColumn(name = "module_type", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_module_type"))
    @NotNull
    private JpaSoftwareModuleType type;

    @CascadeOnDelete
    @ManyToMany(mappedBy = "modules", targetEntity = JpaDistributionSet.class, fetch = FetchType.LAZY)
    private List<DistributionSet> assignedTo;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "vendor", nullable = true, length = SoftwareModule.VENDOR_MAX_SIZE)
    @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
    private String vendor;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "softwareModule", cascade = {
            CascadeType.PERSIST }, targetEntity = JpaArtifact.class, orphanRemoval = true)
    private List<JpaArtifact> artifacts;

    @CascadeOnDelete
    @OneToMany(mappedBy = "softwareModule", fetch = FetchType.LAZY, targetEntity = JpaSoftwareModuleMetadata.class)
    private List<JpaSoftwareModuleMetadata> metadata;

    /**
     * Default constructor.
     */
    public JpaSoftwareModule() {
        // Default constructor for JPA
    }

    /**
     * parameterized constructor.
     *
     * @param type
     *            of the {@link SoftwareModule}
     * @param name
     *            abstract name of the {@link SoftwareModule}
     * @param version
     *            of the {@link SoftwareModule}
     * @param description
     *            of the {@link SoftwareModule}
     * @param vendor
     *            of the {@link SoftwareModule}
     */
    public JpaSoftwareModule(final SoftwareModuleType type, final String name, final String version,
            final String description, final String vendor) {
        super(name, version, description);
        this.vendor = vendor;
        this.type = (JpaSoftwareModuleType) type;
    }

    public void addArtifact(final Artifact artifact) {
        if (null == artifacts) {
            artifacts = new ArrayList<>(4);
            artifacts.add((JpaArtifact) artifact);
            return;
        }

        if (!artifacts.contains(artifact)) {
            artifacts.add((JpaArtifact) artifact);
        }
    }

    /**
     * @return the artifacts
     */
    @Override
    public List<Artifact> getArtifacts() {
        if (artifacts == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(artifacts);
    }

    @Override
    public String getVendor() {
        return vendor;
    }

    /**
     * @param artifact
     *            is removed from the assigned {@link Artifact}s.
     */
    public void removeArtifact(final Artifact artifact) {
        if (artifacts != null) {
            artifacts.remove(artifact);
        }
    }

    public void setVendor(final String vendor) {
        this.vendor = vendor;
    }

    @Override
    public SoftwareModuleType getType() {
        return type;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Marks or un-marks this software module as deleted.
     * 
     * @param deleted
     *            {@code true} if the software module should be marked as
     *            deleted otherwise {@code false}
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public void setType(final JpaSoftwareModuleType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "SoftwareModule [deleted=" + deleted + ", name=" + getName() + ", version=" + getVersion()
                + ", revision=" + getOptLockRevision() + ", Id=" + getId() + ", type=" + getType().getName() + "]";
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
                    getTenant(), getId(), getClass().getName(), EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record).collect(Collectors.toList());

        return changes.stream().filter(record -> DELETED_PROPERTY.equals(record.getAttribute())
                && Boolean.parseBoolean(record.getNewValue().toString())).count() > 0;
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new SoftwareModuleDeletedEvent(getTenant(),
                getId(), getClass().getName(), EventPublisherHolder.getInstance().getApplicationId()));
    }

}
