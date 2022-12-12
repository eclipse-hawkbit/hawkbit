/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.mgmt.json.model.action.MgmtAction;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRepresentationMode;
import org.eclipse.hawkbit.repository.model.Action;
import org.eclipse.hawkbit.rest.data.ResponseList;

/**
 * A mapper which maps repository model to RESTful model representation and
 * back.
 */
public final class MgmtActionMapper {

    private MgmtActionMapper() {
        // Utility class
    }

    /**
     * Create a response for actions.
     *
     * @param actions
     *            list of actions
     * @param repMode
     *            the representation mode
     * 
     * @return the response
     */
    public static List<MgmtAction> toResponse(final Collection<Action> actions, final MgmtRepresentationMode repMode) {
        if (actions == null) {
            return Collections.emptyList();
        }
        return new ResponseList<>(actions.stream().map(action -> MgmtActionMapper.toResponse(action, repMode))
                .collect(Collectors.toList()));
    }

    static MgmtAction toResponse(final Action action, final MgmtRepresentationMode repMode) {
        final String controllerId = action.getTarget().getControllerId();
        if (repMode == MgmtRepresentationMode.COMPACT) {
            return MgmtTargetMapper.toResponse(controllerId, action);
        }
        return MgmtTargetMapper.toResponseWithLinks(controllerId, action);
    }

}
