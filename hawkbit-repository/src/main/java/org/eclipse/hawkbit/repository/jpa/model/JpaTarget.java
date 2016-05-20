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
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.eclipse.hawkbit.im.authentication.SpPermission;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.repository.model.TargetIdName;
import org.eclipse.hawkbit.repository.model.TargetInfo;
import org.eclipse.hawkbit.repository.model.TargetTag;
import org.eclipse.hawkbit.repository.model.helper.SecurityChecker;
import org.eclipse.hawkbit.repository.model.helper.SecurityTokenGeneratorHolder;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.springframework.data.domain.Persistable;

/**
 * <p>
 * The {@link Target} is the target of all provisioning operations. It contains
 * the currently installed {@link DistributionSet} (i.e. current state). In
 * addition it holds the target {@link DistributionSet} that has to be
 * provisioned next (i.e. target state).
 * </p>
 *
 * <p>
 * {@link #getStatus()}s() shows if the {@link Target} is
 * {@link TargetStatus#IN_SYNC} or a provisioning is
 * {@link TargetStatus#PENDING} or the target is only
 * {@link TargetStatus#REGISTERED}, i.e. a target {@link DistributionSet} .
 * </p>
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
public class JpaTarget extends JpaNamedEntity implements Persistable<Long>, Target {
    private static final long serialVersionUID = 1L;

    @Column(name = "controller_id", length = 64)
    @Size(min = 1)
    @NotNull
    private String controllerId;

    @Transient
    private boolean entityNew = false;

    @ManyToMany(targetEntity = JpaTargetTag.class)
    @JoinTable(name = "sp_target_target_tag", joinColumns = {
            @JoinColumn(name = "target", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_target")) }, inverseJoinColumns = {
                    @JoinColumn(name = "tag", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_targtag_tag")) })
    private Set<TargetTag> tags = new HashSet<>();

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = {
            CascadeType.REMOVE }, targetEntity = JpaAction.class)
    @JoinColumn(name = "target", insertable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_targ_act_hist_targ"))
    private final List<Action> actions = new ArrayList<>();

    @ManyToOne(optional = true, fetch = FetchType.LAZY, targetEntity = JpaDistributionSet.class)
    @JoinColumn(name = "assigned_distribution_set", nullable = true, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_target_assign_ds"))
    private JpaDistributionSet assignedDistributionSet;

    @CascadeOnDelete
    @OneToOne(cascade = { CascadeType.ALL }, fetch = FetchType.LAZY, targetEntity = JpaTargetInfo.class)
    @PrimaryKeyJoinColumn
    private JpaTargetInfo targetInfo = null;

    /**
     * the security token of the target which allows if enabled to authenticate
     * with this security token.
     */
    @Column(name = "sec_token", insertable = true, updatable = true, nullable = false, length = 128)
    private String securityToken = null;

    @CascadeOnDelete
    @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST })
    @JoinColumn(name = "target_Id", insertable = false, updatable = false)
    private final List<RolloutTargetGroup> rolloutTargetGroup = new ArrayList<>();

    /**
     * Constructor.
     *
     * @param controllerId
     *            controller ID of the {@link Target}
     */
    public JpaTarget(final String controllerId) {
        this.controllerId = controllerId;
        setName(controllerId);
        securityToken = SecurityTokenGeneratorHolder.getInstance().generateToken();
        targetInfo = new JpaTargetInfo(this);
    }

    /**
     * empty constructor for JPA.
     */
    JpaTarget() {
        controllerId = null;
        securityToken = null;
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
        return tags;
    }

    @Override
    public void setAssignedDistributionSet(final DistributionSet assignedDistributionSet) {
        this.assignedDistributionSet = (JpaDistributionSet) assignedDistributionSet;
    }

    @Override
    public void setControllerId(final String controllerId) {
        this.controllerId = controllerId;
    }

    @Override
    public void setTags(final Set<TargetTag> tags) {
        this.tags = tags;
    }

    @Override
    public List<Action> getActions() {
        return actions;
    }

    @Override
    public TargetIdName getTargetIdName() {
        return new TargetIdName(getId(), getControllerId(), getName());
    }

    @Override
    @Transient
    public boolean isNew() {
        return entityNew;
    }

    /**
     * @param isNew
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
    @Override
    public void setTargetInfo(final TargetInfo targetInfo) {
        this.targetInfo = (JpaTargetInfo) targetInfo;
    }

    /**
     * @return the securityToken
     */
    @Override
    public String getSecurityToken() {
        if (SecurityChecker.hasPermission(SpPermission.READ_TARGET_SEC_TOKEN)) {
            return securityToken;
        }
        return null;
    }

    /**
     * @param securityToken
     *            the securityToken to set
     */
    @Override
    public void setSecurityToken(final String securityToken) {
        this.securityToken = securityToken;
    }

    @Override
    public String toString() {
        return "Target [controllerId=" + controllerId + ", getId()=" + getId() + "]";
    }

}
