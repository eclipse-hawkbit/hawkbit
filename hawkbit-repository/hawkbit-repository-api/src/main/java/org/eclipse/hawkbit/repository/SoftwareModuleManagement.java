/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import java.util.Collection;
import java.util.Map;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.eclipse.hawkbit.auth.SpPermission;
import org.eclipse.hawkbit.auth.SpringEvalExpressions;
import org.eclipse.hawkbit.repository.exception.EntityNotFoundException;
import org.eclipse.hawkbit.repository.model.DistributionSet;
import org.eclipse.hawkbit.repository.model.NamedEntity;
import org.eclipse.hawkbit.repository.model.NamedVersionedEntity;
import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModule.MetadataValue;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Service for managing {@link SoftwareModule}s.
 */
public interface SoftwareModuleManagement<T extends SoftwareModule>
        extends RepositoryManagement<T, SoftwareModuleManagement.Create, SoftwareModuleManagement.Update>, MetadataSupport<MetadataValue> {

    @Override
    default String permissionGroup() {
        return SpPermission.SOFTWARE_MODULE;
    }

    @PreAuthorize(SpringEvalExpressions.IS_SYSTEM_CODE)
    Map<Long, Map<String, String>> findMetaDataBySoftwareModuleIdsAndTargetVisible(Collection<Long> ids);

    /**
     * Locks a software module.
     *
     * @param softwareModule the software module
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T lock(SoftwareModule softwareModule);

    /**
     * Unlocks a software module.<br/>
     * Use it with extreme care! In general once software module is locked
     * it shall not be unlocked. Note that it could have been assigned / deployed to targets.
     *
     * @param softwareModule the software module
     * @throws EntityNotFoundException if software module with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_UPDATE_REPOSITORY)
    T unlock(SoftwareModule softwareModule);

    /**
     * Returns all modules assigned to given {@link DistributionSet}.
     *
     * @param distributionSetId to search for
     * @param pageable the page request to page the result set
     * @return all {@link SoftwareModule}s that are assigned to given {@link DistributionSet}.
     * @throws EntityNotFoundException if distribution set with given ID does not exist
     */
    @PreAuthorize(SpringEvalExpressions.HAS_READ_REPOSITORY)
    Page<T> findByAssignedTo(long distributionSetId, @NotNull Pageable pageable);

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Create extends UpdateCreate {

        private SoftwareModuleType type;
        private boolean encrypted;
    }

    @SuperBuilder
    @Getter
    @EqualsAndHashCode(callSuper = true)
    @ToString(callSuper = true)
    final class Update extends UpdateCreate implements Identifiable<Long> {

        @NotNull
        private Long id;
        @Builder.Default
        private Boolean locked = false;
    }

    @SuperBuilder
    @Getter
    class UpdateCreate {

        @ValidString
        @Size(min = 1, max = NamedEntity.NAME_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String name;
        @ValidString
        @Size(min = 1, max = NamedVersionedEntity.VERSION_MAX_SIZE)
        @NotNull(groups = Create.class)
        private String version;
        @ValidString
        @Size(max = NamedEntity.DESCRIPTION_MAX_SIZE)
        private String description;
        @ValidString
        @Size(max = SoftwareModule.VENDOR_MAX_SIZE)
        private String vendor;
    }
}