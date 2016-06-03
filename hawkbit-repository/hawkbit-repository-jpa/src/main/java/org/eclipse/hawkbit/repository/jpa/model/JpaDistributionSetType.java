/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

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

import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * A distribution set type defines which software module types can or have to be
 * {@link DistributionSet}.
 *
 */
@Entity
@Table(name = "sp_distribution_set_type", indexes = {
        @Index(name = "sp_idx_distribution_set_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_distribution_set_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
                @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_dst_name"),
                @UniqueConstraint(columnNames = { "type_key", "tenant" }, name = "uk_dst_key") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaDistributionSetType extends AbstractJpaNamedEntity implements DistributionSetType {
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
    private boolean deleted;

    public JpaDistributionSetType() {
        // default public constructor for JPA
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
    public JpaDistributionSetType(final String key, final String name, final String description) {
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
    public JpaDistributionSetType(final String key, final String name, final String description, final String color) {
        super(name, description);
        this.key = key;
        colour = color;
    }

    @Override
    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(final boolean deleted) {
        this.deleted = deleted;
    }

    @Override
    public Set<SoftwareModuleType> getMandatoryModuleTypes() {
        return elements.stream().filter(element -> element.isMandatory()).map(element -> element.getSmType())
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SoftwareModuleType> getOptionalModuleTypes() {
        return elements.stream().filter(element -> !element.isMandatory()).map(element -> element.getSmType())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean areModuleEntriesIdentical(final DistributionSetType dsType) {
        return new HashSet<DistributionSetTypeElement>(((JpaDistributionSetType) dsType).elements).equals(elements);
    }

    @Override
    public DistributionSetType addOptionalModuleType(final SoftwareModuleType smType) {
        elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, false));

        return this;
    }

    @Override
    public DistributionSetType addMandatoryModuleType(final SoftwareModuleType smType) {
        elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, true));

        return this;
    }

    @Override
    public DistributionSetType removeModuleType(final Long smTypeId) {
        // we search by id (standard equals compares also revison)
        final Optional<DistributionSetTypeElement> found = elements.stream()
                .filter(element -> element.getSmType().getId().equals(smTypeId)).findFirst();

        if (found.isPresent()) {
            elements.remove(found.get());
        }

        return this;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public boolean checkComplete(final DistributionSet distributionSet) {
        return distributionSet.getModules().stream().map(module -> module.getType()).collect(Collectors.toList())
                .containsAll(getMandatoryModuleTypes());
    }

    @Override
    public String getColour() {
        return colour;
    }

    @Override
    public void setColour(final String colour) {
        this.colour = colour;
    }

    public Set<DistributionSetTypeElement> getElements() {
        return elements;
    }

    @Override
    public String toString() {
        return "DistributionSetType [key=" + key + ", isDeleted()=" + isDeleted() + ", getId()=" + getId() + "]";
    }

}
