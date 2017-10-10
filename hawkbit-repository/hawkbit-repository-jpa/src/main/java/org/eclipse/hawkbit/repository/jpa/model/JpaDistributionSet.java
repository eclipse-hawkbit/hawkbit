/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedAttributeNode;
import javax.persistence.NamedEntityGraph;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotNull;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.queries.UpdateObjectQuery;
import org.eclipse.persistence.sessions.changesets.DirectToFieldChangeRecord;
import org.eclipse.persistence.sessions.changesets.ObjectChangeSet;
import org.springframework.context.ApplicationEvent;

/**
 * Jpa implementation of {@link DistributionSet}.
 *
 */
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
    private static final long serialVersionUID = 1L;

    private static final String DELETED_PROPERTY = "deleted";

    @Column(name = "required_migration_step")
    private boolean requiredMigrationStep;

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

    @Column(name = "deleted")
    private boolean deleted;

    @OneToMany(mappedBy = "assignedDistributionSet", targetEntity = JpaTarget.class, fetch = FetchType.LAZY)
    private List<JpaTarget> assignedToTargets;

    @OneToMany(mappedBy = "autoAssignDistributionSet", targetEntity = JpaTargetFilterQuery.class, fetch = FetchType.LAZY)
    private List<TargetFilterQuery> autoAssignFilters;

    @OneToMany(mappedBy = "installedDistributionSet", targetEntity = JpaTarget.class, fetch = FetchType.LAZY)
    private List<JpaTarget> installedAtTargets;

    @OneToMany(mappedBy = "distributionSet", targetEntity = JpaAction.class, fetch = FetchType.LAZY)
    private List<JpaAction> actions;

    @CascadeOnDelete
    @OneToMany(mappedBy = "distributionSet", fetch = FetchType.LAZY, targetEntity = JpaDistributionSetMetadata.class)
    private List<DistributionSetMetadata> metadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, targetEntity = JpaDistributionSetType.class)
    @JoinColumn(name = "ds_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstype_ds"))
    @NotNull
    private DistributionSetType type;

    @Column(name = "complete")
    private boolean complete;

    /**
     * Default constructor.
     */
    public JpaDistributionSet() {
        // Default constructor for JPA
    }

    /**
     * Parameterized constructor.
     *
     * @param name
     *            of the {@link DistributionSet}
     * @param version
     *            of the {@link DistributionSet}
     * @param description
     *            of the {@link DistributionSet}
     * @param type
     *            of the {@link DistributionSet}
     * @param moduleList
     *            {@link SoftwareModule}s of the {@link DistributionSet}
     * @param requiredMigrationStep
     *            of the {@link DistributionSet}
     */
    public JpaDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList,
            final boolean requiredMigrationStep) {
        super(name, version, description);

        this.requiredMigrationStep = requiredMigrationStep;
        this.type = type;
        if (moduleList != null) {
            moduleList.forEach(this::addModule);
        }
        if (this.type != null) {
            complete = this.type.checkComplete(this);
        }
    }

    /**
     * Parameterized constructor.
     *
     * @param name
     *            of the {@link DistributionSet}
     * @param version
     *            of the {@link DistributionSet}
     * @param description
     *            of the {@link DistributionSet}
     * @param type
     *            of the {@link DistributionSet}
     * @param moduleList
     *            {@link SoftwareModule}s of the {@link DistributionSet}
     */
    public JpaDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList) {
        this(name, version, description, type, moduleList, false);
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

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public List<DistributionSetMetadata> getMetadata() {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(metadata);
    }

    public List<Action> getActions() {
        if (actions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(actions);
    }

    @Override
    public boolean isRequiredMigrationStep() {
        return requiredMigrationStep;
    }

    public DistributionSet setDeleted(final boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    public DistributionSet setRequiredMigrationStep(final boolean isRequiredMigrationStep) {
        requiredMigrationStep = isRequiredMigrationStep;
        return this;
    }

    @Override
    public List<TargetFilterQuery> getAutoAssignFilters() {
        return autoAssignFilters;
    }

    @Override
    public String toString() {
        return "DistributionSet [getName()=" + getName() + ", getOptLockRevision()=" + getOptLockRevision()
                + ", getId()=" + getId() + "]";
    }

    @Override
    public Set<SoftwareModule> getModules() {
        if (modules == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(modules);
    }

    public boolean addModule(final SoftwareModule softwareModule) {
        if (modules == null) {
            modules = new HashSet<>();
        }

        checkTypeCompatability(softwareModule);

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findAny();

        if (found.isPresent()) {
            return false;
        }

        final long allready = modules.stream()
                .filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).count();

        if (allready >= softwareModule.getType().getMaxAssignments()) {
            modules.stream().filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey()))
                    .findAny().ifPresent(modules::remove);
        }

        if (modules.add(softwareModule)) {
            complete = type.checkComplete(this);
            return true;
        }

        return false;
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

    public boolean removeModule(final SoftwareModule softwareModule) {
        if (modules == null) {
            return false;
        }

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findAny();

        if (found.isPresent()) {
            modules.remove(found.get());
            complete = type.checkComplete(this);
            return true;
        }

        return false;

    }

    @Override
    public DistributionSetType getType() {
        return type;
    }

    public void setType(final DistributionSetType type) {
        this.type = type;
    }

    @Override
    public boolean isComplete() {
        return complete;
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
            publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass().getName(),
                    EventPublisherHolder.getInstance().getApplicationId()));
        }
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass().getName(),
                EventPublisherHolder.getInstance().getApplicationId()));
    }

    private static void publishEventWithEventPublisher(final ApplicationEvent event) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event);
    }

    private static boolean isSoftDeleted(final DescriptorEvent event) {
        final ObjectChangeSet changeSet = ((UpdateObjectQuery) event.getQuery()).getObjectChangeSet();
        final List<DirectToFieldChangeRecord> changes = changeSet.getChanges().stream()
                .filter(record -> record instanceof DirectToFieldChangeRecord)
                .map(record -> (DirectToFieldChangeRecord) record).collect(Collectors.toList());

        return changes.stream().filter(record -> DELETED_PROPERTY.equals(record.getAttribute())
                && Boolean.parseBoolean(record.getNewValue().toString())).count() > 0;
    }

}
