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

import java.io.Serializable;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.ConstraintMode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.persistence.annotations.ExistenceChecking;
import org.eclipse.persistence.annotations.ExistenceType;

/**
 * Entity with JPA annotation to store the information which {@link Target} is
 * in a specific {@link RolloutGroup}.
 */
@IdClass(RolloutTargetGroupId.class)
@Entity
@Table(name = "sp_rollouttargetgroup")
@ExistenceChecking(ExistenceType.ASSUME_NON_EXISTENCE)
public class RolloutTargetGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, targetEntity = JpaRolloutGroup.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST })
    @JoinColumn(name = "rolloutGroup_Id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_group"))
    private JpaRolloutGroup rolloutGroup;

    @Id
    @ManyToOne(optional = false, targetEntity = JpaTarget.class, fetch = FetchType.LAZY, cascade = {
            CascadeType.PERSIST })
    @JoinColumn(name = "target_id", nullable = false, updatable = false, foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_target"))
    private JpaTarget target;

    @OneToMany(targetEntity = JpaAction.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumns(value = {
            @JoinColumn(name = "rolloutgroup", nullable = false, insertable = false, updatable = false, referencedColumnName = "rolloutGroup_Id"),
            @JoinColumn(name = "target", nullable = false, insertable = false, updatable = false, referencedColumnName = "target_id") })
    private List<JpaAction> actions;

    /**
     * default constructor for JPA.
     */
    public RolloutTargetGroup() {
        // JPA constructor
    }

    public RolloutTargetGroup(final RolloutGroup rolloutGroup, final Target target) {
        this.rolloutGroup = (JpaRolloutGroup) rolloutGroup;
        this.target = (JpaTarget) target;
    }

    public RolloutTargetGroupId getId() {
        return new RolloutTargetGroupId(rolloutGroup, target);
    }
}
