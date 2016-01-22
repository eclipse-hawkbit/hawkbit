/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

import javax.persistence.ConstraintMode;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ForeignKey;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * @author Michael Hirsch
 *
 */
@IdClass(RolloutTargetGroupId.class)
@Entity
@Table(name = "sp_rollouttargetgroup")
public class RolloutTargetGroup {

    @Id
    @ManyToOne(targetEntity = RolloutGroup.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "rolloutGroup_Id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_group"))
    private RolloutGroup rolloutGroup;

    @Id
    @ManyToOne(targetEntity = Target.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "target_id", foreignKey = @ForeignKey(value = ConstraintMode.CONSTRAINT, name = "fk_rollouttargetgroup_target"))
    private Target target;

    public RolloutTargetGroup() {

    }

    public RolloutTargetGroup(final RolloutGroup rolloutGroup, final Target target) {
        this.rolloutGroup = rolloutGroup;
        this.target = target;
    }

    public RolloutTargetGroupId getId() {
        return new RolloutTargetGroupId(rolloutGroup, target);
    }
}
