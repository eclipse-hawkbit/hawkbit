/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

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
import org.eclipse.hawkbit.repository.TargetFields;
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
            return Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_OFFSET);
        }
        return offset;
    }

    static int sanitizePageLimitParam(final int pageLimit) {
        if (pageLimit < 1) {
            return Integer.parseInt(RestConstants.REQUEST_PARAMETER_PAGING_DEFAULT_LIMIT);
        } else if (pageLimit > RestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT) {
            return RestConstants.REQUEST_PARAMETER_PAGING_MAX_LIMIT;
        }
        return pageLimit;
    }

    static Sort sanitizeTargetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, TargetFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(TargetFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, SoftwareModuleFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(SoftwareModuleFields.class, sortParam));
    }

    static Sort sanitizeSoftwareModuleTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, SoftwareModuleTypeFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(SoftwareModuleTypeFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, DistributionSetFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(DistributionSetFields.class, sortParam));
    }

    static Sort sanitizeDistributionSetTypeSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, DistributionSetTypeFields.NAME.getFieldName());
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
            return new Sort(Direction.ASC, RolloutFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(RolloutFields.class, sortParam));
    }

    static Sort sanitizeRolloutGroupSortParam(final String sortParam) {
        if (sortParam == null) {
            // default
            return new Sort(Direction.ASC, RolloutGroupFields.NAME.getFieldName());
        }
        return new Sort(SortUtility.parse(RolloutGroupFields.class, sortParam));
    }
}
