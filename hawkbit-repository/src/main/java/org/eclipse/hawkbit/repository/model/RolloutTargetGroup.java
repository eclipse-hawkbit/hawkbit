/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.JoinColumns;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.eclipse.persistence.annotations.ExistenceChecking;
import org.eclipse.persistence.annotations.ExistenceType;

/**
 * @author Michael Hirsch
 *
 */
@IdClass(RolloutTargetGroupId.class)
@Entity
@Table(name = "sp_rollouttargetgroup")
@ExistenceChecking(ExistenceType.ASSUME_NON_EXISTENCE)
public class RolloutTargetGroup implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(targetEntity = RolloutGroup.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "rolloutGroup_Id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_group") )
    private RolloutGroup rolloutGroup;

    @Id
    @ManyToOne(targetEntity = Target.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "target_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_target") )
    private Target target;

    @OneToMany(targetEntity = Action.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumns(value = { @JoinColumn(name = "rolloutgroup", referencedColumnName = "rolloutGroup_Id"),
            @JoinColumn(name = "target", referencedColumnName = "target_id") })
    private List<Action> actions;

    /**
     * default constructor for JPA.
     */
    public RolloutTargetGroup() {
        // JPA constructor
    }

    public RolloutTargetGroup(final RolloutGroup rolloutGroup, final Target target) {
        this.rolloutGroup = rolloutGroup;
        this.target = target;
    }

    public RolloutTargetGroupId getId() {
        return new RolloutTargetGroupId(rolloutGroup, target);
    }
}
