/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.mapper;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCode;
import org.eclipse.hawkbit.mgmt.json.model.actionstatuscode.MgmtActionStatusCodeSeverity;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtActionStatusCodeRestApi;
import org.eclipse.hawkbit.repository.model.ActionStatusCode;

/**
 * Mapper for action status code entities to Management API representation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MgmtActionStatusCodeMapper {

    /**
     * Maps a repository ActionStatusCode to a Management API representation.
     *
     * @param entity the repository entity
     * @return the Management API DTO
     */
    public static MgmtActionStatusCode toResponse(final ActionStatusCode entity) {
        final MgmtActionStatusCode response = new MgmtActionStatusCode();
        MgmtRestModelMapper.mapNamedToNamed(response, entity);
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        response.setSeverity(toMgmtSeverity(entity.getSeverity()));

        response.add(linkTo(methodOn(MgmtActionStatusCodeRestApi.class).getActionStatusCode(entity.getId()))
                .withSelfRel());

        return response;
    }

    /**
     * Converts repository severity enum to Management API severity enum.
     *
     * @param severity the repository severity
     * @return the Management API severity
     */
    public static MgmtActionStatusCodeSeverity toMgmtSeverity(final ActionStatusCode.Severity severity) {
        return MgmtActionStatusCodeSeverity.valueOf(severity.name());
    }

    /**
     * Converts Management API severity enum to repository severity enum.
     *
     * @param severity the Management API severity
     * @return the repository severity
     */
    public static ActionStatusCode.Severity toRepoSeverity(final MgmtActionStatusCodeSeverity severity) {
        if (severity == null) {
            return ActionStatusCode.Severity.INFO;
        }
        return ActionStatusCode.Severity.valueOf(severity.name());
    }
}
