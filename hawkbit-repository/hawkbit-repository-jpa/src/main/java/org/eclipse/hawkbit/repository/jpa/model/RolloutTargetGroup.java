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
import java.io.Serializable;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.model.RolloutGroup;
import org.eclipse.hawkbit.repository.model.Target;

/**
 * Entity with JPA annotation to store the information which {@link Target} is in a specific {@link RolloutGroup}.
 */
@NoArgsConstructor // Default constructor for JPA
@IdClass(RolloutTargetGroupId.class)
@Entity
@Table(name = "sp_rollout_target_group")
public class RolloutTargetGroup implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @ManyToOne(optional = false, targetEntity = JpaRolloutGroup.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "rollout_group", nullable = false, updatable = false)
    private JpaRolloutGroup rolloutGroup;

    @Id
    @ManyToOne(optional = false, targetEntity = JpaTarget.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumn(name = "target", nullable = false, updatable = false)
    private JpaTarget target;

    @OneToMany(targetEntity = JpaAction.class, fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST })
    @JoinColumns(value = {
            @JoinColumn(name = "rollout_group", nullable = false, insertable = false, updatable = false, referencedColumnName = "rollout_group"),
            @JoinColumn(name = "target", nullable = false, insertable = false, updatable = false, referencedColumnName = "target") })
    private List<JpaAction> actions;

    public RolloutTargetGroup(final RolloutGroup rolloutGroup, final Target target) {
        this.rolloutGroup = (JpaRolloutGroup) rolloutGroup;
        this.target = (JpaTarget) target;
    }

    public RolloutTargetGroupId getId() {
        return new RolloutTargetGroupId(rolloutGroup, target);
    }
}