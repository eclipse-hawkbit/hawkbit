/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.persistence.annotations.CascadeOnDelete;

/**
 * <p>
 * The {@link DistributionSet} is defined in the SP repository and contains at
 * least an OS and an Agent Hub.
 * </p>
 *
 * <p>
 * A {@link Target} has exactly one target {@link DistributionSet} assigned.
 * </p>
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
public class DistributionSet extends NamedVersionedEntity {
    private static final long serialVersionUID = 1L;

    @Column(name = "required_migration_step")
    private boolean requiredMigrationStep = false;

    @ManyToMany(targetEntity = SoftwareModule.class, fetch = FetchType.LAZY)
    @JoinTable(name = "sp_ds_module", joinColumns = {
            @JoinColumn(name = "ds_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_ds")) }, inverseJoinColumns = {
                    @JoinColumn(name = "module_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_module_module")) })
    private final Set<SoftwareModule> modules = new HashSet<>();

    @ManyToMany(targetEntity = DistributionSetTag.class)
    @JoinTable(name = "sp_ds_dstag", joinColumns = {
            @JoinColumn(name = "ds", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_ds")) }, inverseJoinColumns = {
                    @JoinColumn(name = "TAG", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstag_tag")) })
    private Set<DistributionSetTag> tags = new HashSet<>();

    @Column(name = "deleted")
    private boolean deleted = false;

    @OneToMany(mappedBy = "assignedDistributionSet", targetEntity = Target.class, fetch = FetchType.LAZY)
    private List<Target> assignedToTargets;

    @OneToMany(mappedBy = "installedDistributionSet", targetEntity = TargetInfo.class, fetch = FetchType.LAZY)
    private List<TargetInfo> installedAtTargets;

    @OneToMany(mappedBy = "distributionSet", targetEntity = Action.class, fetch = FetchType.LAZY)
    private List<Action> actions;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE })
    @JoinColumn(name = "ds_id", insertable = false, updatable = false)
    private final List<DistributionSetMetadata> metadata = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ds_id", nullable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_ds_dstype_ds"))
    private DistributionSetType type;

    @Column(name = "complete")
    private boolean complete = false;

    /**
     * Default constructor.
     */
    public DistributionSet() {
        super();
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
    public DistributionSet(final String name, final String version, final String description,
            final DistributionSetType type, final Iterable<SoftwareModule> moduleList) {
        super(name, version, description);

        this.type = type;
        if (moduleList != null) {
            moduleList.forEach(this::addModule);
        }
        if (this.type != null) {
            complete = this.type.checkComplete(this);
        }
    }

    public Set<DistributionSetTag> getTags() {
        return tags;
    }

    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @return immutable list of meta data elements.
     */
    public List<DistributionSetMetadata> getMetadata() {
        return Collections.unmodifiableList(metadata);
    }

    public List<Action> getActions() {
        return actions;
    }

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

    public DistributionSet setTags(final Set<DistributionSetTag> tags) {
        this.tags = tags;
        return this;
    }

    /**
     * @return the assignedTargets
     */
    public List<Target> getAssignedTargets() {
        return assignedToTargets;
    }

    /**
     * @return the installedTargets
     */
    public List<TargetInfo> getInstalledTargets() {
        return installedAtTargets;
    }

    @Override
    public String toString() {
        return "DistributionSet [getName()=" + getName() + ", getOptLockRevision()=" + getOptLockRevision()
                + ", getId()=" + getId() + "]";
    }

    /**
     *
     * @return unmodifiableSet of {@link SoftwareModule}.
     */
    public Set<SoftwareModule> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    public DistributionSetIdName getDistributionSetIdName() {
        return new DistributionSetIdName(getId(), getName(), getVersion());
    }

    /**
     * @param softwareModule
     * @return <code>true</code> if the module was added and <code>false</code>
     *         if it already existed in the set
     *
     */
    public boolean addModule(final SoftwareModule softwareModule) {

        // we cannot allow that modules are added without a type defined
        if (type == null) {
            throw new DistributionSetTypeUndefinedException();
        }

        // check if it is allowed to such a module to this DS type
        if (!type.containsModuleType(softwareModule.getType())) {
            throw new UnsupportedSoftwareModuleForThisDistributionSetException();
        }

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findFirst();

        if (found.isPresent()) {
            return false;
        }

        final long allready = modules.stream()
                .filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).count();

        if (allready >= softwareModule.getType().getMaxAssignments()) {
            final Optional<SoftwareModule> sameKey = modules.stream()
                    .filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).findFirst();
            modules.remove(sameKey.get());
        }

        if (modules.add(softwareModule)) {
            complete = type.checkComplete(this);
            return true;
        }

        return false;
    }

    /**
     * Removed given {@link SoftwareModule} from this DS instance.
     *
     * @param softwareModule
     *            to remove
     * @return <code>true</code> if element was found and removed
     */
    public boolean removeModule(final SoftwareModule softwareModule) {
        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findFirst();

        if (found.isPresent()) {
            modules.remove(found.get());
            complete = type.checkComplete(this);
            return true;
        }

        return false;

    }

    /**
     * Searches through modules for the given type.
     *
     * @param type
     *            to search for
     * @return SoftwareModule of given type or <code>null</code> if not in the
     *         list.
     */
    public SoftwareModule findFirstModuleByType(final SoftwareModuleType type) {
        final Optional<SoftwareModule> result = modules.stream().filter(module -> module.getType().equals(type))
                .findFirst();

        if (result.isPresent()) {
            return result.get();
        }

        return null;
    }

    public DistributionSetType getType() {
        return type;
    }

    public void setType(final DistributionSetType type) {
        this.type = type;
    }

    public boolean isComplete() {
        return complete;
    }
}
