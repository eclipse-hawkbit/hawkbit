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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.IncompleteDistributionSetException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;
import org.springframework.context.ApplicationEvent;

/**
 * Jpa implementation of {@link DistributionSet}.
 */
@NoArgsConstructor // Default constructor for JPA
@Getter
@ToString(callSuper = true)
@Entity
@Table(name = "sp_distribution_set", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "version", "tenant" }, name = "uk_distrib_set") }, indexes = {
                @Index(name = "sp_idx_distribution_set_01", columnList = "tenant,deleted,complete"),
                @Index(name = "sp_idx_distribution_set_prim", columnList = "tenant,id") })
@NamedEntityGraph(name = "DistributionSet.detail", attributeNodes = { @NamedAttributeNode("modules"),
        @NamedAttributeNode("tags"), @NamedAttributeNode("type") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaDistributionSet extends AbstractJpaNamedVersionedEntity implements DistributionSet, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @ManyToOne(fetch = FetchType.LAZY, optional = false, targetEntity = JpaDistributionSetType.class)
    @JoinColumn(name = "ds_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstype_ds"))
    @NotNull
    private DistributionSetType type;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaSoftwareModule.class, fetch = FetchType.LAZY)
    @JoinTable(name = "sp_ds_module", joinColumns = {
            @JoinColumn(name = "ds_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_ds")) }, inverseJoinColumns = {
                    @JoinColumn(name = "module_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_module")) })
    private Set<SoftwareModule> modules;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaDistributionSetTag.class)
    @JoinTable(name = "sp_ds_dstag", joinColumns = {
            @JoinColumn(name = "ds", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_ds")) }, inverseJoinColumns = {
            @JoinColumn(name = "TAG", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_tag")) })
    private Set<DistributionSetTag> tags;

    @ToString.Exclude
    @CascadeOnDelete
    @OneToMany(mappedBy = "distributionSet", fetch = FetchType.LAZY, targetEntity = JpaDistributionSetMetadata.class)
    private List<DistributionSetMetadata> metadata;

    @Column(name = "complete")
    private boolean complete;

    @Column(name = "locked")
    private boolean locked;

    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "valid")
    private boolean valid;

    @Column(name = "required_migration_step")
    private boolean requiredMigrationStep;

    /**
     * Parameterized constructor.
     */
    public JpaDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList,
            final boolean requiredMigrationStep) {
        super(name, version, description);

        this.type = type;
        // modules shall be set before type.checkComplete call
        if (moduleList != null) {
            moduleList.forEach(this::addModule);
        }
        if (this.type != null) {
            complete = this.type.checkComplete(this);
        }

        this.valid = true;
        this.requiredMigrationStep = requiredMigrationStep;
    }

    /**
     * Parameterized constructor.
     */
    public JpaDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList) {
        this(name, version, description, type, moduleList, false);
    }

    public void setType(final DistributionSetType type) {
        this.type = type;
    }

    @Override
    public Set<SoftwareModule> getModules() {
        if (modules == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(modules);
    }

    public boolean addModule(final SoftwareModule softwareModule) {
        if (isLocked()) {
            throw new LockedException(JpaDistributionSet.class, getId(), "ADD_SOFTWARE_MODULE");
        }

        if (modules == null) {
            modules = new HashSet<>();
        }

        checkTypeCompatability(softwareModule);

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findAny();

        if (found.isPresent()) {
            return false;
        }

        final long already = modules.stream()
                .filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).count();

        if (already >= softwareModule.getType().getMaxAssignments()) {
            modules.stream().filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey()))
                    .findAny().ifPresent(modules::remove);
        }

        if (modules.add(softwareModule)) {
            complete = type.checkComplete(this);
            return true;
        }

        return false;
    }

    public void removeModule(final SoftwareModule softwareModule) {
        if (isLocked()) {
            throw new LockedException(JpaDistributionSet.class, getId(), "REMOVE_SOFTWARE_MODULE");
        }

        if (modules != null && modules.removeIf(m -> m.getId().equals(softwareModule.getId()))) {
            complete = type.checkComplete(this);
        }
    }

    public Set<DistributionSetTag> getTags() {
        if (tags == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(tags);
    }

    public boolean addTag(final DistributionSetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }

        return tags.add(tag);
    }

    public boolean removeTag(final DistributionSetTag tag) {
        if (tags == null) {
            return false;
        }

        return tags.remove(tag);
    }

    public List<DistributionSetMetadata> getMetadata() {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(metadata);
    }

    public void lock() {
        if (!isComplete()) {
            throw new IncompleteDistributionSetException("Could not be locked while incomplete!");
        }
        locked = true;
    }

    public void unlock() {
        locked = false;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public void invalidate() {
        this.valid = false;
    }

    public void setRequiredMigrationStep(final boolean isRequiredMigrationStep) {
        requiredMigrationStep = isRequiredMigrationStep;
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        publishEventWithEventPublisher(
                new DistributionSetCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        publishEventWithEventPublisher(
                new DistributionSetUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId(), complete));

        if (isSoftDeleted(descriptorEvent)) {
            publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass(),
                    EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass(),
                EventPublisherHolder.getInstance().getApplicationId()));
    }

    private void checkTypeCompatability(final SoftwareModule softwareModule) {
        // we cannot allow that modules are added without a type defined
        if (type == null) {
            throw new DistributionSetTypeUndefinedException();
        }

        // check if it is allowed to such a module to this DS type
        if (!type.containsModuleType(softwareModule.getType())) {
            throw new UnsupportedSoftwareModuleForThisDistributionSetException();
        }
    }

    private static void publishEventWithEventPublisher(final ApplicationEvent event) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event);
    }

    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(DirectToFieldChangeRecord.class::isInstance).map(DirectToFieldChangeRecord.class::cast)
                .collect(Collectors.toList());

        return changes.stream().anyMatch(changeRecord -> DELETED_PROPERTY.equals(changeRecord.getAttribute())
                && Boolean.parseBoolean(changeRecord.getNewValue().toString()));
    }
}