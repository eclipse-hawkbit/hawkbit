/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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
import javax.persistence.OneToOne;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.event.remote.TargetDeletedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetCreatedEvent;
import org.eclipse.hawkbit.repository.event.remote.entity.TargetUpdatedEvent;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityChecker;
import org.eclipse.hawkbit.repository.jpa.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.hawkbit.repository.jpa.model.helper.SystemSecurityContextHolder;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.helper.EventPublisherHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Persistable;

/**
 * JPA implementation of {@link Target}.
 *
 */
@Entity
@Table(name = "sp_target", indexes = {
        @Index(name = "sp_idx_target_01", columnList = "tenant,name,assigned_distribution_set"),
        @Index(name = "sp_idx_target_02", columnList = "tenant,name"),
        @Index(name = "sp_idx_target_03", columnList = "tenant,controller_id,assigned_distribution_set"),
        @Index(name = "sp_idx_target_04", columnList = "tenant,created_at"),
        @Index(name = "sp_idx_target_prim", columnList = "tenant,id") }, uniqueConstraints = @UniqueConstraint(columnNames = {
                "controller_id", "tenant" }, name = "uk_tenant_controller_id"))
@NamedEntityGraph(name = "Target.detail", attributeNodes = { @NamedAttributeNode("tags"),
        @NamedAttributeNode(value = "assignedDistributionSet"), @NamedAttributeNode(value = "targetInfo") })
// exception squid:S2160 - BaseEntity equals/hashcode is handling correctly for
// sub entities
@SuppressWarnings("squid:S2160")
public class JpaTarget extends AbstractJpaNamedEntity implements Persistable<Long>, Target, EventAwareEntity {

    private static final long serialVersionUID = 1L;

    @Column(name = "controller_id", length = 64)
    @Size(min = 1, max = 64)
    @NotEmpty
    @Pattern(regexp = "[.\\S]*", message = "has whitespaces which are not allowed")
    private String controllerId;

    @Transient
    private boolean entityNew;

    @CascadeOnDelete
    @ManyToMany(targetEntity = JpaTargetTag.class)
    @JoinTable(name = "sp_target_target_tag", joinColumns = {
            @JoinColumn(name = "target", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_target")) }, inverseJoinColumns = {
                    @JoinColumn(name = "tag", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_tag")) })
    private Set<TargetTag> tags;

    @CascadeOnDelete
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, targetEntity = JpaAction.class)
    private List<JpaAction> actions;

    @ManyToOne(optional = true, fetch = FetchType.LAZY, targetEntity = JpaDistributionSet.class)
    @JoinColumn(name = "assigned_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_assign_ds"))
    private JpaDistributionSet assignedDistributionSet;

    @CascadeOnDelete
    @OneToOne(cascade = { CascadeType.PERSIST,
            CascadeType.MERGE }, fetch = FetchType.LAZY, targetEntity = JpaTargetInfo.class)
    @PrimaryKeyJoinColumn
    private JpaTargetInfo targetInfo;

    /**
     * the security token of the target which allows if enabled to authenticate
     * with this security token.
     */
    @Column(name = "sec_token", updatable = true, nullable = false, length = 128)
    @Size(max = 64)
    @NotEmpty
    private String securityToken;

    @CascadeOnDelete
    @OneToMany(mappedBy = "target", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    private List<RolloutTargetGroup> rolloutTargetGroup;

    /**
     * Constructor.
     *
     * @param controllerId
     *            controller ID of the {@link Target}
     */
    public JpaTarget(final String controllerId) {
        this(controllerId, SecurityTokenGeneratorHolder.getInstance().generateToken());
    }

    /**
     * Constructor.
     *
     * @param controllerId
     *            controller ID of the {@link Target}
     * @param securityToken
     *            for target authentication if enabled
     */
    public JpaTarget(final String controllerId, final String securityToken) {
        this.controllerId = controllerId;
        setName(controllerId);
        this.securityToken = securityToken;
        targetInfo = new JpaTargetInfo(this);
    }

    JpaTarget() {
        // empty constructor for JPA.
    }

    @Override
    public DistributionSet getAssignedDistributionSet() {
        return assignedDistributionSet;
    }

    @Override
    public String getControllerId() {
        return controllerId;
    }

    @Override
    public Set<TargetTag> getTags() {
        if (tags == null) {
            return Collections.emptySet();
        }

        return Collections.unmodifiableSet(tags);
    }

    public List<RolloutTargetGroup> getRolloutTargetGroup() {
        if (rolloutTargetGroup == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(rolloutTargetGroup);
    }

    public boolean addTag(final TargetTag tag) {
        if (tags == null) {
            tags = new HashSet<>();
        }

        return tags.add(tag);
    }

    public boolean removeTag(final TargetTag tag) {
        if (tags == null) {
            return false;
        }

        return tags.remove(tag);
    }

    public void setAssignedDistributionSet(final DistributionSet assignedDistributionSet) {
        this.assignedDistributionSet = (JpaDistributionSet) assignedDistributionSet;
    }

    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    @Override
    public List<Action> getActions() {
        if (actions == null) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(actions);
    }

    public boolean addAction(final Action action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }

        return actions.add((JpaAction) action);
    }

    @Override
    @Transient
    public boolean isNew() {
        return entityNew;
    }

    /**
     * @param entityNew
     *            the isNew to set
     */
    public void setNew(final boolean entityNew) {
        this.entityNew = entityNew;
    }

    /**
     * @return the targetInfo
     */
    @Override
    public TargetInfo getTargetInfo() {
        return targetInfo;
    }

    /**
     * @param targetInfo
     *            the targetInfo to set
     */
    public void setTargetInfo(final TargetInfo targetInfo) {
        this.targetInfo = (JpaTargetInfo) targetInfo;
    }

    /**
     * @return the securityToken if the current security context contains the
     *         necessary permission {@link SpPermission#READ_TARGET_SEC_TOKEN}
     *         or the current context is executed as system code, otherwise
     *         {@code null}.
     */
    @Override
    public String getSecurityToken() {
        if (SystemSecurityContextHolder.getInstance().getSystemSecurityContext().isCurrentThreadSystemCode()
                || SecurityChecker.hasPermission(SpPermission.READ_TARGET_SEC_TOKEN)) {
            return securityToken;
        }
        return null;
    }

    /**
     * @param securityToken
     *            the securityToken to set
     */
    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }

    @Override
    public String toString() {
        return "Target [controllerId=" + controllerId + ", getId()=" + getId() + "]";
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
        EventPublisherHolder.getInstance().getEventPublisher().publishEvent(new TargetDeletedEvent(getTenant(), getId(),
                getClass().getName(), EventPublisherHolder.getInstance().getApplicationId()));
    }
}
