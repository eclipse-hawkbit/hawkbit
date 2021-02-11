/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.rest.resource;

import org.eclipse.hawkbit.mgmt.rest.api.MgmtRestConstants;
import org.eclipse.hawkbit.repository.ActionFields;
import org.eclipse.hawkbit.repository.ActionStatusFields;
import org.eclipse.hawkbit.repository.DistributionSetFields;
import org.eclipse.hawkbit.repository.DistributionSetMetadataFields;
import org.eclipse.hawkbit.repository.DistributionSetTypeFields;
import org.eclipse.hawkbit.repository.RolloutFields;
import org.eclipse.hawkbit.repository.RolloutGroupFields;
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.SoftwareModuleTypeFields;
import org.eclipse.hawkbit.repository.TagFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetFilterQueryFields;
import org.eclipse.hawkbit.rest.util.SortUtility;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Utility class for for paged body generation.
 *
 */
public final class PagingUtility {
    /*
     * utility constructor private.
     */
    private PagingUtility() {
    }

    static int sanitizeOffsetParam(final int offset) {
        if (offset < 0) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET_VALUE;
        }
        return offset;
    }

    static int sanitizePageLimitParam(final int pageLimit) {
        if (pageLimit < 1) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT_VALUE;
        } else if (pageLimit > MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT) {
            return MgmtRestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT;
        }
        return pageLimit;
    }

    static Sort sanitizeTargetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TargetFields.CONTROLLERID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFields.class, sortParam));
    }

    static Sort sanitizeTagSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TagFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TagFields.class, sortParam));
    }

    static Sort sanitizeTargetFilterQuerySortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, TargetFilterQueryFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(TargetFilterQueryFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleTypeFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleTypeFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetTypeFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetTypeFields.class, sortParam));
    }

    static Sort sanitizeActionSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(ActionFields.class, sortParam));
    }

    static Sort sanitizeActionStatusSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action status to match behavior
            // of management UI (last entry on top)
            return Sort.by(Direction.DESC, ActionStatusFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(ActionStatusFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, DistributionSetMetadataFields.KEY.getFieldName());
        }
        return Sort.by(SortUtility.parse(DistributionSetMetadataFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, SoftwareModuleMetadataFields.KEY.getFieldName());
        }
        return Sort.by(SortUtility.parse(SoftwareModuleMetadataFields.class, sortParam));
    }

    static Sort sanitizeRolloutSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutFields.class, sortParam));
    }

    static Sort sanitizeRolloutGroupSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return Sort.by(Direction.ASC, RolloutGroupFields.ID.getFieldName());
        }
        return Sort.by(SortUtility.parse(RolloutGroupFields.class, sortParam));
    }
}
