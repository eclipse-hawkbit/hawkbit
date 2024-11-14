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
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ConstraintMode;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.AutoConfirmationStatus;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.PollStatus;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetMetadata;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.TargetType;
import org.eclipse.hawkbit.repository.model.TargetUpdateStatus;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.hawkbit.repository.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.helper.TenantConfigurationManagementHolder;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.persistence.annotations.ConversionValue;
import org.eclipse.persistence.annotations.Convert;
import org.eclipse.persistence.annotations.ObjectTypeConverter;
import org.eclipse.persistence.descriptors.DescriptorEvent;

/**
 * JPA implementation of {@link Target}.
 */
@Entity
@Table(name = "sp_target", indexes = {
        @Index(name = "sp_idx_target_01", columnList = "tenant,name,assigned_distribution_set"),
        @Index(name = "sp_idx_target_03", columnList = "tenant,controller_id,assigned_distribution_set"),
        @Index(name = "sp_idx_target_04", columnList = "tenant,created_at"),
        @Index(name = "sp_idx_target_05", columnList = "tenant,last_modified_at"),
        @Index(name = "sp_idx_target_prim", columnList = "tenant,id") }, uniqueConstraints = @UniqueConstraint(columnNames = {
        "controller_id", "tenant" }, name = "uk_tenant_controller_id"))
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
@Slf4j
public class JpaTarget extends AbstractJpaNamedEntity implements Target, EventAwareEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    private static final List<String> TARGET_UPDATE_EVENT_IGNORE_FIELDS = Arrays.asList("lastTargetQuery", "address",
            "optLockRevision", "lastModifiedAt", "lastModifiedBy");

    @Column(name = "controller_id", length = Target.CONTROLLER_ID_MAX_SIZE, updatable = false, nullable = false)
    @Size(min = 1, max = Target.CONTROLLER_ID_MAX_SIZE)
    @NotNull
    @Pattern(regexp = "[\\S]*", message = "has whitespaces which are not allowed")
    private String controllerId;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, targetEntity = JpaAction.class)
    private List<JpaAction> actions;

    /**
     * the security token of the target which allows if enabled to authenticate
     * with this security token.
     */
    @Column(name = "sec_token", nullable = false, length = Target.SECURITY_TOKEN_MAX_SIZE)
    @Size(min = 1, max = Target.SECURITY_TOKEN_MAX_SIZE)
    @NotNull
    private String securityToken;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE })
    private List<RolloutTargetGroup> rolloutTargetGroup;

    @Column(name = "address", length = Target.ADDRESS_MAX_SIZE)
    @Size(max = Target.ADDRESS_MAX_SIZE)
    private String address;

    @Column(name = "last_target_query")
    private Long lastTargetQuery;

    @Column(name = "install_date")
    private Long installationDate;

    @Column(name = "update_status", nullable = false)
    @ObjectTypeConverter(name = "updateStatus", objectType = TargetUpdateStatus.class, dataType = Integer.class, conversionValues = {
            @ConversionValue(objectValue = "UNKNOWN", dataValue = "0"),
            @ConversionValue(objectValue = "IN_SYNC", dataValue = "1"),
            @ConversionValue(objectValue = "PENDING", dataValue = "2"),
            @ConversionValue(objectValue = "ERROR", dataValue = "3"),
            @ConversionValue(objectValue = "REGISTERED", dataValue = "4") })
    @Convert("updateStatus")
    @NotNull
    private TargetUpdateStatus updateStatus = TargetUpdateStatus.UNKNOWN;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "installed_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_inst_ds"))
    private JpaDistributionSet installedDistributionSet;

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_assign_ds"))
    private JpaDistributionSet assignedDistributionSet;

    // set default request controller attributes to true, because we want to request them the first time
    @Column(name = "request_controller_attributes", nullable = false)
    private boolean requestControllerAttributes = true;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "target", orphanRemoval = true)
    @PrimaryKeyJoinColumn
    private JpaAutoConfirmationStatus autoConfirmationStatus;

    @ManyToOne(fetch = FetchType.LAZY, targetEntity = JpaTargetType.class)
    @JoinColumn(name = "target_type", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_relation_target_type"))
    private TargetType targetType;

    @ManyToMany(cascade = { CascadeType.REMOVE }, targetEntity = JpaTargetTag.class)
    @JoinTable(
            name = "sp_target_target_tag",
            joinColumns = {
                    @JoinColumn(
                            name = "target", nullable = false, insertable = false, updatable = false,
                            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_target")) },
            inverseJoinColumns = {
                    @JoinColumn(
                            name = "tag", nullable = false, insertable = false, updatable = false,
                            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_tag"))
            })
    private Set<TargetTag> tags;

    /**
     * Supplied / committed by the controller. Read-only via management API.
     */
    // no cascade option on an ElementCollection, the target objects are always persisted, merged, removed with their parent.
    @ElementCollection
    @Column(name = "attribute_value", length = Target.CONTROLLER_ATTRIBUTE_VALUE_SIZE)
    @MapKeyColumn(name = "attribute_key", nullable = false, length = Target.CONTROLLER_ATTRIBUTE_KEY_SIZE)
    @CollectionTable(
            name = "sp_target_attributes",
            joinColumns = { @JoinColumn(name = "target_id", nullable = false, insertable = false, updatable = false) },
            foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_attrib_target"))
    private Map<String, String> controllerAttributes;

    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE }, targetEntity = JpaTargetMetadata.class)
    private List<TargetMetadata> metadata;

    /**
     * Constructor.
     *
     * @param controllerId controller ID of the {@link Target}
     */
    public JpaTarget(final String controllerId) {
        this(controllerId, SecurityTokenGeneratorHolder.getInstance().generateToken());
    }

    /**
     * Constructor.
     *
     * @param controllerId controller ID of the {@link Target}
     * @param securityToken for target authentication if enabled
     */
    public JpaTarget(final String controllerId, final String securityToken) {
        this.controllerId = controllerId;
        // truncate controller ID to max name length (if needed)
        setName(controllerId != null && controllerId.length() > NAME_MAX_SIZE ? controllerId.substring(0, NAME_MAX_SIZE) : controllerId);
        this.securityToken = securityToken;
    }

    /**
     * Constructor
     */
    public JpaTarget() {
        // empty constructor for JPA.
    }

    /**
     * @return assigned distribution set
     */
    public DistributionSet getAssignedDistributionSet() {
        return assignedDistributionSet;
    }

    /**
     * @param assignedDistributionSet Distribution set
     */
    public void setAssignedDistributionSet(final DistributionSet assignedDistributionSet) {
        this.assignedDistributionSet = (JpaDistributionSet) assignedDistributionSet;
    }

    @Override
    public String getControllerId() {
        return controllerId;
    }

    /**
     * @param controllerId Controller ID
     */
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    /**
     * @return the securityToken if the current security context contains the
     *         necessary permission {@link SpPermission#READ_TARGET_SEC_TOKEN}
     *         or the current context is executed as system code, otherwise
     *         {@code null}.
     */
    @Override
    public String getSecurityToken() {
        final SystemSecurityContext systemSecurityContext =
                SystemSecurityContextHolder.getInstance().getSystemSecurityContext();
        if (systemSecurityContext.isCurrentThreadSystemCode() ||
                systemSecurityContext.hasPermission(SpPermission.READ_TARGET_SEC_TOKEN)) {
            return securityToken;
        }
        return null;
    }

    /**
     * @param securityToken token value
     */
    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }

    /**
     * @return the ipAddress
     */
    @Override
    public URI getAddress() {
        if (address == null) {
            return null;
        }
        try {
            return URI.create(address);
        } catch (final IllegalArgumentException e) {
            log.warn("Invalid address provided. Cloud not be configured to URI", e);
            return null;
        }
    }

    @Override
    public Long getLastTargetQuery() {
        return lastTargetQuery;
    }

    @Override
    public Long getInstallationDate() {
        return installationDate;
    }

    @Override
    public TargetUpdateStatus getUpdateStatus() {
        return updateStatus;
    }

    @Override
    public TargetType getTargetType() {
        return targetType;
    }

    /**
     * @return the poll time which holds the last poll time of the target, the
     *         next poll time and the overdue time. In case the
     *         {@link #lastTargetQuery} is not set e.g. the target never polled
     *         before this method returns {@code null}
     */
    @Override
    public PollStatus getPollStatus() {
        // skip creating resolver
        if (lastTargetQuery == null) {
            return null;
        }
        return TenantConfigurationManagementHolder.getInstance().getTenantConfigurationManagement()
                .pollStatusResolver().apply(this);
    }

    @Override
    public AutoConfirmationStatus getAutoConfirmationStatus() {
        return autoConfirmationStatus;
    }

    public void setAutoConfirmationStatus(final JpaAutoConfirmationStatus autoConfirmationStatus) {
        this.autoConfirmationStatus = autoConfirmationStatus;
    }

    @Override
    public boolean isRequestControllerAttributes() {
        return requestControllerAttributes;
    }

    /**
     * @param requestControllerAttributes Attributes
     */
    public void setRequestControllerAttributes(final boolean requestControllerAttributes) {
        this.requestControllerAttributes = requestControllerAttributes;
    }

    /**
     * @param type Target type
     */
    public void setTargetType(final TargetType type) {
        this.targetType = type;
    }

    /**
     * @param updateStatus Status
     */
    public void setUpdateStatus(final TargetUpdateStatus updateStatus) {
        this.updateStatus = updateStatus;
    }

    /**
     * @param installationDate installation date
     */
    public void setInstallationDate(final Long installationDate) {
        this.installationDate = installationDate;
    }

    /**
     * @param lastTargetQuery last query ID
     */
    public void setLastTargetQuery(final Long lastTargetQuery) {
        this.lastTargetQuery = lastTargetQuery;
    }

    /**
     * @param address Address
     */
    public void setAddress(final String address) {
        this.address = address;
    }

    /**
     * @return tags
     */
    public Set<TargetTag> getTags() {
        if (tags == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(tags);
    }

    /**
     * @return rollouts target group
     */
    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        if (rolloutTargetGroup == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(rolloutTargetGroup);
    }

    /**
     * @param tag to be added
     */
    public void addTag(final TargetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }
        tags.add(tag);
    }

    /**
     * @param tag the tag to be removed from the target
     */
    public void removeTag(final TargetTag tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    /**
     * @return list of action
     */
    public List<Action> getActions() {
        if (actions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(actions);
    }

    /**
     * @param action Action
     */
    public void addAction(final Action action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add((JpaAction) action);
    }

    /**
     * @return Distribution set
     */
    public JpaDistributionSet getInstalledDistributionSet() {
        return installedDistributionSet;
    }

    /**
     * @param installedDistributionSet Distribution set
     */
    public void setInstalledDistributionSet(final JpaDistributionSet installedDistributionSet) {
        this.installedDistributionSet = installedDistributionSet;
    }

    /**
     * @return controller attributes
     */
    public Map<String, String> getControllerAttributes() {
        return controllerAttributes;
    }

    /**
     * @return target metadata
     */
    public List<TargetMetadata> getMetadata() {
        if (metadata == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(metadata);
    }

    @Override
    public String toString() {
        return "JpaTarget [controllerId=" + controllerId + ", revision=" + getOptLockRevision() + ", id=" + getId()
                + "]";
    }

    @Override
    public void fireCreateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetCreatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireUpdateEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetUpdatedEvent(this, EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public void fireDeleteEvent(final DescriptorEvent descriptorEvent) {
        EventPublisherHolder.getInstance().getEventPublisher()
                .publishEvent(new TargetDeletedEvent(getTenant(), getId(), getControllerId(), address,
                        getClass(), EventPublisherHolder.getInstance().getApplicationId()));
    }

    @Override
    public List<String> getUpdateIgnoreFields() {
        return TARGET_UPDATE_EVENT_IGNORE_FIELDS;
    }
}
