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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Convert;
import jakarta.persistence.Converter;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.NamedEntityGraphs;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.context.System;
import org.eclipse.hawkbit.repository.event.EventPublisherHolder;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.helper.TenantConfigHelper;
import org.eclipse.hawkbit.repository.jpa.utils.MapAttributeConverter;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.springframework.util.ObjectUtils;

/**
 * JPA implementation of {@link Target}.
 */
@NoArgsConstructor // Default constructor for JPA
@Slf4j
@Entity
@Table(name = "sp_target",
        indexes = {
                @Index(name = "sp_idx_target_01", columnList = "tenant,name,assigned_distribution_set"),
                @Index(name = "sp_idx_target_03", columnList = "tenant,controller_id,assigned_distribution_set"),
                @Index(name = "sp_idx_target_04", columnList = "tenant,created_at"),
                @Index(name = "sp_idx_target_05", columnList = "tenant,last_modified_at"),
                @Index(name = "sp_idx_target_prim", columnList = "tenant,id") },
        uniqueConstraints = @UniqueConstraint(columnNames = { "controller_id", "tenant" }, name = "uk_target_controller_id"))
@NamedEntityGraphs({
        @NamedEntityGraph(name = "Target.details", attributeNodes = {
                @NamedAttributeNode("targetType"),
                @NamedAttributeNode("installedDistributionSet"),
                @NamedAttributeNode("assignedDistributionSet") }),
        @NamedEntityGraph(name = "Target.targetType", attributeNodes = { @NamedAttributeNode("targetType") }),
        @NamedEntityGraph(name = "Target.installedDistributionSet", attributeNodes = { @NamedAttributeNode("installedDistributionSet") }),
        @NamedEntityGraph(name = "Target.assignedDistributionSet", attributeNodes = { @NamedAttributeNode("assignedDistributionSet") }),
        @NamedEntityGraph(name = "Target.autoConfirmationStatus", attributeNodes = { @NamedAttributeNode("autoConfirmationStatus") }),
        @NamedEntityGraph(name = "Target.tags", attributeNodes = { @NamedAttributeNode("tags") }),
        @NamedEntityGraph(name = "Target.actions", attributeNodes = { @NamedAttributeNode("actions") }),
})
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for sub entities
// java:S1710 - not possible to use without group annotation
@SuppressWarnings({ "squid:S2160", "java:S1710" })
public class JpaTarget extends AbstractJpaNamedEntity implements Target, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @Setter
    @Getter
    @Column(name = "controller_id", length = Target.CONTROLLER_ID_MAX_SIZE, updatable = false, nullable = false)
    @Size(min = 1, max = Target.CONTROLLER_ID_MAX_SIZE)
    @NotNull
    @Pattern(regexp = "[\\S]*", message = "has whitespaces which are not allowed")
    private String controllerId;

    /** the security token of the target which allows if enabled to authenticate with this security token */
    @Setter
    @Column(name = "sec_token", nullable = false, length = Target.SECURITY_TOKEN_MAX_SIZE)
    @Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE)
    @NotNull
    private String securityToken;

    @Setter
    @Getter
    @Column(name = "address", length = Target.ADDRESS_MAX_SIZE)
    @Size(max = Target.ADDRESS_MAX_SIZE)
    private String address;

    @Setter
    @Getter
    @Column(name = "last_target_query")
    private Long lastTargetQuery;

    @Setter
    @Getter
    @Column(name = "install_date")
    private Long installationDate;

    @Setter
    @Getter
    @Column(name = "update_status", nullable = false)
    @Convert(converter = TargetUpdateStatusConverter.class)
    @NotNull
    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "installed_distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_inst_ds"))
    private JpaDistributionSet installedDistributionSet;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_distribution_set", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_assign_ds"))
    private JpaDistributionSet assignedDistributionSet;

    @Setter
    @Getter
    @Column(name = "request_controller_attributes", nullable = false)
    // set default request controller attributes to true, because we want to request them the first time
    private boolean requestControllerAttributes = true;

    @Setter
    @Getter
    @Column(name = "target_group")
    private String group;

    // actually it is OneToOne - but lazy loading is not supported for OneToOne (at least for hibernate 6.6.2)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "target", cascade = { CascadeType.ALL }, orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private List<JpaAutoConfirmationStatus> autoConfirmationStatus;

    @Setter
    @Getter
    @ManyToOne(fetch = FetchType.LAZY, targetEntity = JpaTargetType.class)
    @JoinColumn(name = "target_type", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_relation_target_type"))
    private TargetType targetType;

    @ManyToMany(targetEntity = JpaTargetTag.class)
    @JoinTable(
            name = "sp_target_target_tag",
            joinColumns = {
                    @JoinColumn(
                            name = "target", nullable = false,
                            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_target_tag_target")) },
            inverseJoinColumns = {
                    @JoinColumn(
                            name = "tag", nullable = false,
                            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_target_tag_tag"))
            })
    private Set<TargetTag> tags;

    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent
    @Getter
    @ElementCollection
    @CollectionTable(
            name = "sp_target_attributes",
            joinColumns = { @JoinColumn(name = "target", nullable = false) },
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_attributes_target"))
    @MapKeyColumn(name = "attribute_key", length = Target.CONTROLLER_ATTRIBUTE_MAX_KEY_SIZE)
    @Column(name = "attribute_value", length = Target.CONTROLLER_ATTRIBUTE_MAX_VALUE_SIZE)
    private Map<String, String> controllerAttributes;

    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent
    @Getter
    @ElementCollection
    @CollectionTable(
            name = "sp_target_metadata",
            joinColumns = { @JoinColumn(name = "target", nullable = false) },
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_metadata_target"))
    @MapKeyColumn(name = "meta_key", length = Target.METADATA_MAX_KEY_SIZE)
    @Column(name = "meta_value", length = Target.METADATA_MAX_VALUE_SIZE)
    private Map<String, String> metadata;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, targetEntity = JpaAction.class)
    private List<JpaAction> actions;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<RolloutTargetGroup> rolloutTargetGroup;

    @Override
    public String getSecurityToken() {
        if (System.isCurrentThreadSystemCode() || SpPermission.hasPermission(SpPermission.READ_TARGET_SECURITY_TOKEN)) {
            return securityToken;
        }
        return null;
    }

    /**
     * @return the poll time which holds the last poll time of the target, the next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled before this method returns {@code null}
     */
    @Override
    public PollStatus getPollStatus() {
        // skip creating resolver
        if (lastTargetQuery == null) {
            return null;
        }
        return TenantConfigHelper.getTenantConfigurationManagement()
                .pollStatusResolver()
                .apply(this);
    }

    public JpaAutoConfirmationStatus getAutoConfirmationStatus() {
        return ObjectUtils.isEmpty(autoConfirmationStatus) ? null : autoConfirmationStatus.get(0);
    }

    public void setAutoConfirmationStatus(final JpaAutoConfirmationStatus autoConfirmationStatus) {
        if (this.autoConfirmationStatus == null) {
            this.autoConfirmationStatus = new ArrayList<>();
        } else {
            this.autoConfirmationStatus.clear();
        }
        this.autoConfirmationStatus.add(autoConfirmationStatus);
    }

    public void addTag(final TargetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
    }

    public void removeTag(final TargetTag tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    public Set<TargetTag> getTags() {
        return tags == null ? Collections.emptySet() : Collections.unmodifiableSet(tags);
    }

    public void addAction(final Action action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add((JpaAction) action);
    }

    @Override
    public void fireCreateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetCreatedEvent(this));
    }

    @Override
    public void fireUpdateEvent() {
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetUpdatedEvent(this));
    }

    @Override
    public void fireDeleteEvent() {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetDeletedEvent(getTenant(), getId(), getClass(), getControllerId(), address));
    }

    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        return rolloutTargetGroup == null ? Collections.emptyList() : Collections.unmodifiableList(rolloutTargetGroup);
    }

    @Override
    public String toString() {
        return "JpaTarget [controllerId=" + controllerId + ", revision=" + getOptLockRevision() + ", id=" + getId() + "]";
    }

    // if "lastTargetQuery", "address" only are changed - the remove events are skipped
    @Override
    public void postUpdate() {
        // do nothing - processed by EntityPropertyChangeListener
        // since we want to skip the event if only "lastTargetQuery" or "address" are changed
    }

    @Converter
    public static class TargetUpdateStatusConverter extends MapAttributeConverter<TargetUpdateStatus, Integer> {

        public TargetUpdateStatusConverter() {
            super(Map.of(
                    TargetUpdateStatus.UNKNOWN, 0,
                    TargetUpdateStatus.IN_SYNC, 1,
                    TargetUpdateStatus.PENDING, 2,
                    TargetUpdateStatus.ERROR, 3,
                    TargetUpdateStatus.REGISTERED, 4
            ), null);
        }
    }
}