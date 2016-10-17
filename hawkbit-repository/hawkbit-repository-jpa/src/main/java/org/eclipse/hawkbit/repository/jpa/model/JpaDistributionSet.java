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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.persistence.CascadeType;
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

import org.eclipse.hawkbit.repository.event.remote.entity.RemotePropertyChangeEvent.PropertyChange;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdateEvent;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.jpa.model.helper.EntityPropertyChangeHelper;
import org.eclipse.hawkbit.repository.jpa.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetMetadata;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetFilterQuery;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Jpa implementation of {@link DistributionSet}.
 *
 */
@Entity
@Table(name = "sp_distribution_set", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "version", "tenant" }, name = "uk_distrib_set") }, indexes = {
                @Index(name = "sp_idx_distribution_set_01", columnList = "tenant,deleted,name,complete"),
                @Index(name = "sp_idx_distribution_set_02", columnList = "tenant,required_migration_step"),
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
            @JoinColumn(name = "ds_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_ds")) }, inverseJoinColumns = {
                    @JoinColumn(name = "module_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_module")) })
    private Set<SoftwareModule> modules;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaDistributionSetTag.class)
    @JoinTable(name = "sp_ds_dstag", joinColumns = {
            @JoinColumn(name = "ds", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_ds")) }, inverseJoinColumns = {
                    @JoinColumn(name = "TAG", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_tag")) })
    private Set<DistributionSetTag> tags;

    @Column(name = "deleted")
    private boolean deleted;

    @OneToMany(mappedBy = "assignedDistributionSet", targetEntity = JpaTarget.class, fetch = FetchType.LAZY)
    private List<Target> assignedToTargets;

    @OneToMany(mappedBy = "autoAssignDistributionSet", targetEntity = JpaTargetFilterQuery.class, fetch = FetchType.LAZY)
    private List<TargetFilterQuery> autoAssignFilters;

    @OneToMany(mappedBy = "installedDistributionSet", targetEntity = JpaTargetInfo.class, fetch = FetchType.LAZY)
    private List<TargetInfo> installedAtTargets;

    @OneToMany(mappedBy = "distributionSet", targetEntity = JpaAction.class, fetch = FetchType.LAZY)
    private List<Action> actions;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, targetEntity = JpaDistributionSetMetadata.class, cascade = {
            CascadeType.REMOVE })
    @JoinColumn(name = "ds_id", insertable = false, updatable = false)
    private List<DistributionSetMetadata> metadata;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = JpaDistributionSetType.class)
    @JoinColumn(name = "ds_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstype_ds"))
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
     */
    public JpaDistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Collection<SoftwareModule> moduleList) {
        super(name, version, description);

        this.type = type;
        if (moduleList != null) {
            moduleList.forEach(this::addModule);
        }
        if (this.type != null) {
            complete = this.type.checkComplete(this);
        }
    }

    @Override
    public Set<DistributionSetTag> getTags() {
        if (tags == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(tags);
    }

    @Override
    public boolean addTag(final DistributionSetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }

        return tags.add(tag);
    }

    @Override
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

    @Override
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

    @Override
    public DistributionSet setDeleted(final boolean deleted) {
        this.deleted = deleted;
        return this;
    }

    @Override
    public DistributionSet setRequiredMigrationStep(final boolean isRequiredMigrationStep) {
        requiredMigrationStep = isRequiredMigrationStep;
        return this;
    }

    @Override
    public List<Target> getAssignedTargets() {
        if (assignedToTargets == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(assignedToTargets);
    }

    @Override
    public List<TargetFilterQuery> getAutoAssignFilters() {
        return autoAssignFilters;
    }

    @Override
    public List<TargetInfo> getInstalledTargets() {
        if (installedAtTargets == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(installedAtTargets);
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

    @Override
    public boolean addModule(final SoftwareModule softwareModule) {
        if (modules == null) {
            modules = new HashSet<>();
        }

        checkTypeCompatability(softwareModule);

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findFirst();

        if (found.isPresent()) {
            return false;
        }

        final long allready = modules.stream()
                .filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).count();

        if (allready >= softwareModule.getType().getMaxAssignments()) {
            modules.stream().filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey()))
                    .findFirst().map(modules::remove);
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

    @Override
    public boolean removeModule(final SoftwareModule softwareModule) {
        if (modules == null) {
            return false;
        }

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findFirst();

        if (found.isPresent()) {
            modules.remove(found.get());
            complete = type.checkComplete(this);
            return true;
        }

        return false;

    }

    @Override
    public SoftwareModule findFirstModuleByType(final SoftwareModuleType type) {
        if (modules == null) {
            return null;
        }

        return modules.stream().filter(module -> module.getType().equals(type)).findFirst().orElse(null);
    }

    @Override
    public DistributionSetType getType() {
        return type;
    }

    @Override
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
                new DistributionCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {

        final Map<String, PropertyChange> changeSet = EntityPropertyChangeHelper.getChangeSet(descriptorEvent);
        publishEventWithEventPublisher(
                new DistributionSetUpdateEvent(this, EventPublisherHolder.getInstance().getApplicationId()));

        if (changeSet.containsKey(DELETED_PROPERTY)) {
            final Boolean newDeleted = (Boolean) changeSet.get(DELETED_PROPERTY).getNewValue();
            if (newDeleted) {
                publishEventWithEventPublisher(new DistributionDeletedEvent(getTenant(), getId(),
                        EventPublisherHolder.getInstance().getApplicationId()));
            }
        }

    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        publishEventWithEventPublisher(new DistributionDeletedEvent(getTenant(), getId(),
                EventPublisherHolder.getInstance().getApplicationId()));
    }

    private void publishEventWithEventPublisher(final ApplicationEvent event) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event);
    }

}
