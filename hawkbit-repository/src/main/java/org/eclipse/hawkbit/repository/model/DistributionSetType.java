/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

/**
 * A distribution set type defines which software module types can or have to be
 * {@link DistributionSet}.
 *
 *
 *
 *
 */
@Entity
@Table(name = "sp_distribution_set_type", indexes = {
        @Index(name = "sp_idx_distribution_set_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_distribution_set_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_dst_name"),
                @UniqueConstraint(columnNames = { "type_key", "tenant" }, name = "uk_dst_key") })
public class DistributionSetType extends NamedEntity {

    /**
    *
    */
    private static final long serialVersionUID = 1L;

    @OneToMany(targetEntity = DistributionSetTypeElement.class, cascade = {
            CascadeType.ALL }, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "distribution_set_type", insertable = false, updatable = false)
    private final Set<DistributionSetTypeElement> elements = new HashSet<>();

    @Column(name = "type_key", nullable = false, length = 64)
    private String key;

    @Column(name = "colour", nullable = true, length = 16)
    private String colour;

    @Column(name = "deleted")
    private boolean deleted = false;

    public DistributionSetType() {
    }

    /**
     * Standard constructor.
     *
     * @param key
     *            of the type (unique)
     * @param name
     *            of the type (unique)
     * @param description
     *            of the type
     */
    public DistributionSetType(final String key, final String name, final String description) {
        this(key, name, description, null);
    }

    /**
     * Constructor.
     *
     * @param key
     *            of the type
     * @param name
     *            of the type
     * @param description
     *            of the type
     * @param color
     *            of the type. It will be null by default
     */
    public DistributionSetType(final String key, final String name, final String description, final String color) {
        super(name, description);
        this.key = key;
        this.colour = color;
    }

    /**
     * @return the deleted
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * @param deleted
     *            the deleted to set
     */
    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    public Set<SoftwareModuleType> getMandatoryModuleTypes() {
        return elements.stream().filter(element -> element.isMandatory()).map(element -> element.getSmType())
                .collect(Collectors.toSet());
    }

    public Set<SoftwareModuleType> getOptionalModuleTypes() {
        return elements.stream().filter(element -> !element.isMandatory()).map(element -> element.getSmType())
                .collect(Collectors.toSet());
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    public boolean containsModuleType(final SoftwareModuleType softwareModuleType) {
        for (final DistributionSetTypeElement distributionSetTypeElement : elements) {
            if (distributionSetTypeElement.getSmType().equals(softwareModuleType)) {
                return true;
            }

        }
        return false;
    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    public boolean containsMandatoryModuleType(final SoftwareModuleType softwareModuleType) {
        return elements.stream().filter(element -> element.isMandatory())
                .filter(element -> element.getSmType().equals(softwareModuleType)).findFirst().isPresent();

    }

    /**
     * Checks if the given {@link SoftwareModuleType} is in this
     * {@link DistributionSetType} and NOT defined as
     * {@link DistributionSetTypeElement#isMandatory()}.
     *
     * @param softwareModuleType
     *            search for
     * @return <code>true</code> if found
     */
    public boolean containsOptionalModuleType(final SoftwareModuleType softwareModuleType) {
        return elements.stream().filter(element -> !element.isMandatory())
                .filter(element -> element.getSmType().equals(softwareModuleType)).findFirst().isPresent();

    }

    /**
     * Compares the modules of this {@link DistributionSetType} and the given
     * one.
     *
     * @param dsType
     *            to compare with
     * @return <code>true</code> if the lists are identical.
     */
    public boolean areModuleEntriesIdentical(final DistributionSetType dsType) {
        return new HashSet<DistributionSetTypeElement>(dsType.elements).equals(elements);
    }

    /**
     * Adds {@link SoftwareModuleType} that is optional for the
     * {@link DistributionSet}.
     *
     * @param smType
     *            to add
     * @return updated instance
     */
    public DistributionSetType addOptionalModuleType(final SoftwareModuleType smType) {
        elements.add(new DistributionSetTypeElement(this, smType, false));

        return this;
    }

    /**
     * Adds {@link SoftwareModuleType} that is mandatory for the
     * {@link DistributionSet}.
     *
     * @param smType
     *            to add
     * @return updated instance
     */
    public DistributionSetType addMandatoryModuleType(final SoftwareModuleType smType) {
        elements.add(new DistributionSetTypeElement(this, smType, true));

        return this;
    }

    /**
     * Removes {@link SoftwareModuleType} from the list.
     *
     * @param smTypeId
     *            to remove
     * @return updated instance
     */
    public DistributionSetType removeModuleType(final Long smTypeId) {
        // we search by id (standard equals compares also revison)
        final Optional<DistributionSetTypeElement> found = elements.stream()
                .filter(element -> element.getSmType().getId().equals(smTypeId)).findFirst();

        if (found.isPresent()) {
            elements.remove(found.get());
        }

        return this;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key
     *            the key to set
     */
    public void setKey(final String key) {
        this.key = key;
    }

    /**
     * @param distributionSet
     *            to check for completeness
     * @return <code>true</code> if the all mandatory software module types are
     *         in the system.
     */
    public boolean checkComplete(final DistributionSet distributionSet) {
        return distributionSet.getModules().stream().map(module -> module.getType()).collect(Collectors.toList())
                .containsAll(getMandatoryModuleTypes());
    }

    /**
     *
     * @return the DistributionSet type color
     */
    public String getColour() {
        return colour;
    }

    /**
     *
     * @param colour
     *            the col
     */
    public void setColour(final String colour) {
        this.colour = colour;
    }

    public Set<DistributionSetTypeElement> getElements() {
        return elements;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "DistributionSetType [key=" + key + ", isDeleted()=" + isDeleted() + ", getId()=" + getId() + "]";
    }

}
