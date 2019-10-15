/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.RepositoryProperties;
import org.eclipse.hawkbit.repository.model.Action;
import org.springframework.data.domain.PageRequest;

/**
 * Implements utility methods for managing {@link Action}s
 */
public class ActionManagement {

    protected final ActionRepository actionRepository;
    protected final RepositoryProperties repositoryProperties;

    protected ActionManagement(final ActionRepository actionRepository,
            final RepositoryProperties repositoryProperties) {
        this.actionRepository = actionRepository;
        this.repositoryProperties = repositoryProperties;
    }
    
    protected List<Action> findActiveActionsWithHighestWeightConsideringDefault(final String controllerId,
            final int maxActionCount) {
        if (!actionRepository.activeActionExistsForControllerId(controllerId)) {
            return Collections.emptyList();
        }
        final List<Action> actions = new ArrayList<>();
        final PageRequest pageable = PageRequest.of(0, maxActionCount);
        actions.addAll(actionRepository
                .findByTargetControllerIdAndActiveIsTrueAndWeightIsNotNullOrderByWeightDescIdAsc(pageable, controllerId)
                .getContent());
        actions.addAll(actionRepository
                .findByTargetControllerIdAndActiveIsTrueAndWeightIsNullOrderByIdAsc(pageable, controllerId)
                .getContent());
        final Comparator<Action> actionImportance = Comparator.comparingInt(this::getWeightConsideringDefault)
                .reversed().thenComparing(Action::getId);
        return actions.stream().sorted(actionImportance).limit(maxActionCount).collect(Collectors.toList());
    }

    protected int getWeightConsideringDefault(final Action action) {
        return action.getWeight().orElse(repositoryProperties.getActionWeightIfAbsent());
    }

}
