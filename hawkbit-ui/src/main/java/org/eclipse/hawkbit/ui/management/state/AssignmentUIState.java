/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.state;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.hawkbit.ui.common.entity.DistributionSetIdName;
import org.eclipse.hawkbit.ui.common.entity.TargetIdName;

/**
 * State keeping track of the distribution set assignments that were done in the
 * UI.
 */
public class AssignmentUIState implements Serializable, Iterable<DistributionSetIdName> {

    private static final long serialVersionUID = 1L;

    private final Map<DistributionSetIdName, Set<TargetIdName>> assignments = new HashMap<>();

    private final Set<TargetIdName> assignedTargets = new HashSet<>();

    public Set<TargetIdName> getAssignedTargets(final DistributionSetIdName distributionSet) {
        if (!assignments.containsKey(distributionSet)) {
            return Collections.emptySet();
        }
        return assignments.get(distributionSet);
    }

    public void addAssignment(final DistributionSetIdName distributionSet, final Set<TargetIdName> targets) {
        assignedTargets.addAll(targets);
        assignments.computeIfAbsent(distributionSet, key -> new HashSet<>()).addAll(targets);
    }

    public void addAssignment(final DistributionSetIdName distributionSet, final TargetIdName target) {
        assignedTargets.add(target);
        assignments.computeIfAbsent(distributionSet, key -> new HashSet<>()).add(target);
    }

    public boolean hasAssignments(final TargetIdName target) {
        return assignedTargets.contains(target);
    }

    public boolean isAssignedTo(final DistributionSetIdName distributionSet, final TargetIdName target) {
        if (!assignments.containsKey(distributionSet)) {
            return false;
        }
        return assignments.get(distributionSet).contains(target);
    }

    public boolean isAssigned(final DistributionSetIdName distributionSet) {
        return assignments.containsKey(distributionSet);
    }

    public boolean isAssigned(final Long distributionSetId) {
        return assignments.keySet().stream().anyMatch(key -> distributionSetId.equals(key.getId()));
    }

    public void clear() {
        assignments.clear();
        assignedTargets.clear();
    }

    @Override
    public Iterator<DistributionSetIdName> iterator() {
        return assignments.keySet().iterator();
    }

}
