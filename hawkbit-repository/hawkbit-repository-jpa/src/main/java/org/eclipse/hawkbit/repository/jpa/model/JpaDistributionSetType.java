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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetTypeDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetTypeUpdatedEvent;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

/**
 * A distribution set type defines which software module types can or have to be {@link DistributionSet}.
 */
@NoArgsConstructor // Default constructor needed for JPA entities.
@Entity
@Table(name = "sp_distribution_set_type")
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

    public JpaDistributionSetType setMandatoryModuleTypes(final Set<SoftwareModuleType> smType) {
        return replaceOrAddModuleTypes(smType, true, true);
    }

    public JpaDistributionSetType setOptionalModuleTypes(final Set<SoftwareModuleType> smType) {
        return replaceOrAddModuleTypes(smType, false, true);
    }

    public JpaDistributionSetType addMandatoryModuleType(final SoftwareModuleType smType) {
        return replaceOrAddModuleTypes(Set.of(smType), true, false);
    }

    public JpaDistributionSetType addOptionalModuleType(final SoftwareModuleType smType) {
        return replaceOrAddModuleTypes(Set.of(smType), false, false);
    }

    public JpaDistributionSetType removeModuleType(final SoftwareModuleType smType) {
        elements.stream()
                .filter(element -> smType.getId().equals(element.getSmType().getId()))
                .toList() // collect to a list to avoid ConcurrentModificationException
                .forEach(element -> elements.remove(element));
        return this;
    }

    @Override
    public String toString() {
        return "DistributionSetType [key=" + getKey() + ", isDeleted()=" + isDeleted() + ", getId()=" + getId() + "]";
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTypeCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new DistributionSetTypeUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new DistributionSetTypeDeletedEvent(getTenant(), getId(), getClass()));
    }

    private JpaDistributionSetType replaceOrAddModuleTypes(final Set<SoftwareModuleType> smTypes, final boolean mandatory, final boolean replace) {
        if (smTypes == null) {
            return this; // do not change
        }

        if (elements.isEmpty()) {
            smTypes.forEach(smType -> elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory)));
            return this;
        }

        smTypes.stream()
                .filter(smType -> !elements.contains(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory)))
                .collect(Collectors.toSet())
                .forEach(smType -> elements.add(new DistributionSetTypeElement(this, (JpaSoftwareModuleType) smType, mandatory)));

        if (replace) {
            final Set<Long> smTypeIds = smTypes.stream()
                    .map(SoftwareModuleType::getId)
                    .collect(Collectors.toSet());
            elements.stream()
                    .filter(element -> element.isMandatory() == mandatory && !smTypeIds.contains(element.getSmType().getId()))
                    .collect(Collectors.toSet())
                    .forEach(element -> elements.remove(element));
        }
        return this;
    }
}