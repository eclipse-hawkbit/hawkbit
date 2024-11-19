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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.eclipse.hawkbit.repository.event.remote.DistributionSetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.springframework.util.CollectionUtils;

/**
 * A distribution set type defines which software module types can or have to be
 * {@link DistributionSet}.
 */
@Entity
@Table(name = "sp_distribution_set_type", indexes = {
        @Index(name = "sp_idx_distribution_set_type_01", columnList = "tenant,deleted"),
        @Index(name = "sp_idx_distribution_set_type_prim", columnList = "tenant,id") }, uniqueConstraints = {
        @UniqueConstraint(columnNames = { "name", "tenant" }, name = "uk_dst_name"),
        @UniqueConstraint(columnNames = { "type_key", "tenant" }, name = "uk_dst_key") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaDistributionSetType extends AbstractJpaTypeEntity implements DistributionSetType, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @OneToMany(mappedBy = "dsType", targetEntity = DistributionSetTypeElement.class, fetch = FetchType.EAGER, cascade = { CascadeType.PERSIST,
            CascadeType.REMOVE }, orphanRemoval = true)
    private Set<DistributionSetTypeElement> elements;

    @Column(name = "deleted")
    private boolean deleted;

    @ManyToMany(mappedBy = "distributionSetTypes", targetEntity = JpaTargetType.class, fetch = FetchType.LAZY)
    private List<TargetType> compatibleToTargetTypes;

    public JpaDistributionSetType() {
        // default public constructor for JPA
    }

    /**
     * Standard constructor.
     *
     * @param key of the type (unique)
     * @param name of the type (unique)
     * @param description of the type
     */
    public JpaDistributionSetType(final String key, final String name, final String description) {
        this(key, name, description, null);
    }

    /**
     * Constructor.
     *
     * @param key of the type
     * @param name of the type
     * @param description of the type
     * @param colour of the type. It will be null by default
     */
    public JpaDistributionSetType(final String key, final String name, final String description, final String colour) {
        super(name, description, key, colour);
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
        if (elements == null) {
            return Collections.emptySet();
        }

        return elements.stream().filter(DistributionSetTypeElement::isMandatory)
                .map(DistributionSetTypeElement::getSmType).collect(Collectors.toSet());
    }

    @Override
    public Set<SoftwareModuleType> getOptionalModuleTypes() {
        if (elements == null) {
            return Collections.emptySet();
        }

        return elements.stream().filter(element -> !element.isMandatory()).map(DistributionSetTypeElement::getSmType)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean areModuleEntriesIdentical(final DistributionSetType dsType) {
        if (!(dsType instanceof JpaDistributionSetType) || isOneModuleListEmpty(dsType)) {
            return false;
        } else if (areBothModuleListsEmpty(dsType)) {
            return true;
        }

        return new HashSet<>(((JpaDistributionSetType) dsType).elements).equals(elements);
    }

    @Override
    public boolean checkComplete(final DistributionSet distributionSet) {
        final List<SoftwareModuleType> smTypes = distributionSet.getModules().stream().map(SoftwareModule::getType)
                .distinct().toList();
        if (smTypes.isEmpty()) {
            return false;
        }
        return new HashSet<>(smTypes).containsAll(getMandatoryModuleTypes());
    }

    public JpaDistributionSetType addOptionalModuleType(final SoftwareModuleType smType) {
        return setModuleType(smType, false);
    }

    public JpaDistributionSetType addMandatoryModuleType(final SoftwareModuleType smType) {
        return setModuleType(smType, true);
    }

    public JpaDistributionSetType removeModuleType(final Long smTypeId) {
        if (elements == null) {
            return this;
        }

        // we search by id (standard equals compares also revison)
        elements.stream().filter(element -> element.getSmType().getId().equals(smTypeId)).findAny()
                .ifPresent(elements::remove);

        return this;
    }

    public Set<DistributionSetTypeElement> getElements() {
        if (elements == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(elements);
    }

    @Override
    public String toString() {
        return "DistributionSetType [key=" + getKey() + ", isDeleted()=" + isDeleted() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new DistributionSetTypeCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(
                new DistributionSetTypeUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTypeDeletedEvent(
                getTenant(), getId(), getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    private boolean isOneModuleListEmpty(final DistributionSetType dsType) {
        return (!CollectionUtils.isEmpty(((JpaDistributionSetType) dsType).elements)
                && CollectionUtils.isEmpty(elements))
                || (CollectionUtils.isEmpty(((JpaDistributionSetType) dsType).elements)
                && !CollectionUtils.isEmpty(elements));
    }

    private boolean areBothModuleListsEmpty(final DistributionSetType dsType) {
        return CollectionUtils.isEmpty(((JpaDistributionSetType) dsType).elements) && CollectionUtils.isEmpty(elements);
    }

    private JpaDistributionSetType setModuleType(final SoftwareModuleType smType, final boolean mandatory) {
        if (elements == null) {
            elements = new HashSet<>();
            elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory));
            return this;
        }

        // check if this was in the list before before
        final Optional<DistributionSetTypeElement> existing = elements.stream()
                .filter(element -> element.getSmType().getKey().equals(smType.getKey())).findAny();

        if (existing.isPresent()) {
            existing.get().setMandatory(mandatory);
        } else {
            elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory));
        }

        return this;
    }
}
