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
            return new Sort(Direction.ASC, TargetFields.CONTROLLERID.getFieldName());
        }
        return new Sort(SortUtility.parse(TargetFields.class, sortParam));
    }

    static Sort sanitizeTagSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, TagFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(TagFields.class, sortParam));
    }

    static Sort sanitizeTargetFilterQuerySortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, TargetFilterQueryFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(TargetFilterQueryFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, SoftwareModuleFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(SoftwareModuleFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, SoftwareModuleTypeFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(SoftwareModuleTypeFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, DistributionSetFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(DistributionSetFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, DistributionSetTypeFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(DistributionSetTypeFields.class, sortParam));
    }

    static Sort sanitizeActionSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action to match behavior
            // of management UI (last entry on top)
            return new Sort(Direction.DESC, ActionFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(ActionFields.class, sortParam));
    }

    static Sort sanitizeActionStatusSortParam(final String sortParam) {
        if (sortParam == null) {
            // default sort is DESC in case of action status to match behavior
            // of management UI (last entry on top)
            return new Sort(Direction.DESC, ActionStatusFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(ActionStatusFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, DistributionSetMetadataFields.KEY.getFieldName());
        }
        return new Sort(SortUtility.parse(DistributionSetMetadataFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleMetadataSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, SoftwareModuleMetadataFields.KEY.getFieldName());
        }
        return new Sort(SortUtility.parse(SoftwareModuleMetadataFields.class, sortParam));
    }

    static Sort sanitizeRolloutSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, RolloutFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(RolloutFields.class, sortParam));
    }

    static Sort sanitizeRolloutGroupSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, RolloutGroupFields.ID.getFieldName());
        }
        return new Sort(SortUtility.parse(RolloutGroupFields.class, sortParam));
    }
}
