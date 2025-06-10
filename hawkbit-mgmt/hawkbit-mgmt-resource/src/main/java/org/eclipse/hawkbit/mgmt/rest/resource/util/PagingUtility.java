/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.rest.resource.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.OffsetBasedPageRequest;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.repository.TargetTypeFields;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Utility class for for paged body generation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PagingUtility {

    public static Sort sanitizeTargetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TargetFields.CONTROLLERID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFields.class, sortParam));
    }

    public static Sort sanitizeTargetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TargetTypeFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(TargetTypeFields.class, sortParam));
    }

    public static Sort sanitizeTagSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TagFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(TagFields.class, sortParam));
    }

    public static Sort sanitizeTargetFilterQuerySortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TargetFilterQueryFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFilterQueryFields.class, sortParam));
    }

    public static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleFields.class, sortParam));
    }

    public static Sort sanitizeSoftwareModuleTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleTypeFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleTypeFields.class, sortParam));
    }

    public static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetFields.class, sortParam));
    }

    public static Sort sanitizeDistributionSetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetTypeFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetTypeFields.class, sortParam));
    }

    public static Sort sanitizeActionSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(ActionFields.class, sortParam));
    }

    public static Sort sanitizeActionStatusSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action status to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionStatusFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(ActionStatusFields.class, sortParam));
    }

    public static Sort sanitizeRolloutSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutFields.class, sortParam));
    }

    public static Sort sanitizeRolloutGroupSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutGroupFields.ID.getJpaEntityFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutGroupFields.class, sortParam));
    }

    public static Pageable toPageable(final int pagingOffsetParam, final int pagingLimitParam, final Sort sort) {
        final int sanitizedOffsetParam = sanitizeOffsetParam(pagingOffsetParam);
        final int sanitizedLimitParam = sanitizePageLimitParam(pagingLimitParam);
        return new OffsetBasedPageRequest(sanitizedOffsetParam, sanitizedLimitParam, sort);
    }

    private static int sanitizeOffsetParam(final int offset) {
        if (offset < 0) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE;
        }
        return offset;
    }

    private static int sanitizePageLimitParam(final int pageLimit) {
        if (pageLimit < 1) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE;
        } else if (pageLimit > MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT;
        }
        return pageLimit;
    }
}