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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.DistributionSetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.DistributionSetUpdatedEvent;
import org.eclipse.hawkbit.repository.exception.DistributionSetTypeUndefinedException;
import org.eclipse.hawkbit.repository.exception.LockedException;
import org.eclipse.hawkbit.repository.exception.UnsupportedSoftwareModuleForThisDistributionSetException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.DistributionSetTag;
import org.eclipse.hawkbit.repository.model.DistributionSetType;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.context.ApplicationEvent;
import org.springframework.core.annotation.Order;

/**
 * Jpa implementation of {@link DistributionSet}.
 */
@NoArgsConstructor // Default constructor for JPA
@Getter
@ToString(callSuper = true)
@Entity
@Table(name = "sp_distribution_set")
@NamedEntityGraph(name = "DistributionSet.detail",
        attributeNodes = { @NamedAttributeNode("modules"), @NamedAttributeNode("tags"), @NamedAttributeNode("type") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
@SuppressWarnings("squid:S2160")
public class JpaDistributionSet
        extends AbstractJpaNamedVersionedEntity
        implements DistributionSet, WithMetadata<String, String>, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, targetEntity = JpaDistributionSetType.class)
    @JoinColumn(name = "ds_type", nullable = false, updatable = false)
    @NotNull
    private DistributionSetType type;

    @ManyToMany(targetEntity = JpaSoftwareModule.class, fetch = FetchType.LAZY)
    @JoinTable(
            name = "sp_ds_sm",
            joinColumns = { @JoinColumn(name = "ds_id", nullable = false) },
            inverseJoinColumns = { @JoinColumn(name = "sm_id", nullable = false) })
    private Set<SoftwareModule> modules = new HashSet<>();

    @ManyToMany(targetEntity = JpaDistributionSetTag.class)
    @JoinTable(
            name = "sp_ds_tag",
            joinColumns = {
                    @JoinColumn(name = "ds", nullable = false) },
            inverseJoinColumns = {
                    @JoinColumn(
                            name = "tag", nullable = false) })
    private Set<DistributionSetTag> tags = new HashSet<>();

    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent
    @Getter
    @ElementCollection
    @CollectionTable(
            name = "sp_ds_metadata",
            joinColumns = { @JoinColumn(name = "ds", nullable = false) })
    @MapKeyColumn(name = "meta_key", length = DistributionSet.METADATA_MAX_KEY_SIZE)
    @Column(name = "meta_value", length = DistributionSet.METADATA_MAX_VALUE_SIZE)
    private Map<String, String> metadata = new HashMap<>();

    @Setter
    @Column(name = "locked")
    private boolean locked;

    @Setter
    @Column(name = "deleted")
    private boolean deleted;

    @Column(name = "valid")
    private boolean valid;

    @Column(name = "required_migration_step")
    private boolean requiredMigrationStep;

    @Order(0)
    public void setType(final DistributionSetType type) {
        this.type = type;
    }

    @Override
    public Set<SoftwareModule> getModules() {
        return Collections.unmodifiableSet(modules);
    }

    @SuppressWarnings("java:S1144") // used via reflection copy utils
    private JpaDistributionSet setModules(final Set<SoftwareModule> modules) {
        if (modules == null) {
            return this; // do not change
        }

        modules.forEach(this::addModule); // skip if already present
        this.modules.stream().filter(module -> !modules.contains(module)).toList().forEach(this::removeModule);
        return this;
    }

    @SuppressWarnings("java:S1144") // used via reflection copy utils
    private void setRequiredMigrationStep(final boolean requiredMigrationStep) {
        if (this.requiredMigrationStep != requiredMigrationStep) {
            if (isLocked()) {
                throw new LockedException(JpaDistributionSet.class, getId(), "CHANGE_REQUIRED_MIGRATION_STEP");
            }

            this.requiredMigrationStep = requiredMigrationStep;
        }
    }

    @Override
    public boolean isComplete() {
        return Optional.ofNullable(type).map(dsType -> {
            if (getModules().stream().anyMatch(module -> !module.isComplete())) {
                return false; // incomplete module
            }
            final List<SoftwareModuleType> smTypes = getModules().stream()
                    .map(SoftwareModule::getType)
                    .distinct()
                    .toList();
            return !smTypes.isEmpty() && new HashSet<>(smTypes).containsAll(dsType.getMandatoryModuleTypes());
        }).orElse(true);
    }

    public void addModule(final SoftwareModule softwareModule) {
        if (isLocked()) {
            throw new LockedException(JpaDistributionSet.class, getId(), "ADD_SOFTWARE_MODULE");
        }

        checkTypeCompatability(softwareModule);

        final Optional<SoftwareModule> found = modules.stream()
                .filter(module -> module.getId().equals(softwareModule.getId())).findAny();
        if (found.isPresent()) {
            return;
        }

        final long already = modules.stream().filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey())).count();
        if (already >= softwareModule.getType().getMaxAssignments()) {
            modules.stream().filter(module -> module.getType().getKey().equals(softwareModule.getType().getKey()))
                    .findAny().ifPresent(modules::remove);
        }

        modules.add(softwareModule);
    }

    public void removeModule(final SoftwareModule softwareModule) {
        if (isLocked()) {
            throw new LockedException(JpaDistributionSet.class, getId(), "REMOVE_SOFTWARE_MODULE");
        }

        if (modules != null) {
            modules.removeIf(m -> m.getId().equals(softwareModule.getId()));
        }
    }

    public Set<DistributionSetTag> getTags() {
        return Collections.unmodifiableSet(tags);
    }

    public void addTag(final DistributionSetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
    }

    public void removeTag(final DistributionSetTag tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    public void invalidate() {
        this.valid = false;
    }

    @Override
    public void fireCreateEvent() {
        publishEventWithEventPublisher(new DistributionSetCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        publishEventWithEventPublisher(new DistributionSetUpdatedEvent(this));

        if (deleted) {
            publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass()));
        }
    }

    @Override
    public void fireDeleteEvent() {
        publishEventWithEventPublisher(new DistributionSetDeletedEvent(getTenant(), getId(), getClass()));
    }

    private static void publishEventWithEventPublisher(final ApplicationEvent event) {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(event);
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
}