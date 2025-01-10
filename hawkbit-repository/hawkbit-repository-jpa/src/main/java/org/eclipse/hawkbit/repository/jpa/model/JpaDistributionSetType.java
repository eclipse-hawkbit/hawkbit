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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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
 * A distribution set type defines which software module types can or have to be {@link DistributionSet}.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
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

    @OneToMany(
            mappedBy = "dsType", targetEntity = DistributionSetTypeElement.class,
            fetch = FetchType.EAGER,
            cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE }, orphanRemoval = true)
    private Set<DistributionSetTypeElement> elements = new HashSet<>();

    @Setter
    @Getter
    @Column(name = "deleted")
    private boolean deleted;

    public JpaDistributionSetType(final String key, final String name, final String description) {
        this(key, name, description, null);
    }

    public JpaDistributionSetType(final String key, final String name, final String description, final String colour) {
        super(name, description, key, colour);
    }

    @Override
    public Set<SoftwareModuleType> getMandatoryModuleTypes() {
        return elements.stream()
                .filter(DistributionSetTypeElement::isMandatory)
                .map(DistributionSetTypeElement::getSmType)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<SoftwareModuleType> getOptionalModuleTypes() {
        return elements.stream()
                .filter(element -> !element.isMandatory())
                .map(DistributionSetTypeElement::getSmType)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean checkComplete(final DistributionSet distributionSet) {
        final List<SoftwareModuleType> smTypes = distributionSet.getModules().stream()
                .map(SoftwareModule::getType)
                .distinct()
                .toList();
        return !smTypes.isEmpty() && new HashSet<>(smTypes).containsAll(getMandatoryModuleTypes());
    }

    public JpaDistributionSetType addOptionalModuleType(final SoftwareModuleType smType) {
        return setModuleType(smType, false);
    }

    public JpaDistributionSetType addMandatoryModuleType(final SoftwareModuleType smType) {
        return setModuleType(smType, true);
    }

    public JpaDistributionSetType removeModuleType(final Long smTypeId) {
        // we search by id (standard equals compares also revision)
        elements.stream()
                .filter(element -> element.getSmType().getId().equals(smTypeId))
                .findAny()
                .ifPresent(elements::remove);
        return this;
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

    private JpaDistributionSetType setModuleType(final SoftwareModuleType smType, final boolean mandatory) {
        if (elements.isEmpty()) {
            elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory));
            return this;
        }

        // check if this was in the list before
        elements.stream()
                .filter(element -> element.getSmType().getKey().equals(smType.getKey()))
                .findAny()
                .ifPresentOrElse(
                        element -> element.setMandatory(mandatory),
                        () -> elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory)));

        return this;
    }
}