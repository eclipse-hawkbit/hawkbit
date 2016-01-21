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
import org.eclipse.hawkbit.repository.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.SoftwareModuleMetadataFields;
import org.eclipse.hawkbit.repository.TargetFields;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;

/**
 * Utility class for for paged body generation.
 *
 *
 *
 *
 *
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
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(TargetFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, TargetFields.NAME.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeSoftwareModuleSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(SoftwareModuleFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, SoftwareModuleFields.NAME.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeDistributionSetSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(DistributionSetFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, DistributionSetFields.NAME.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeActionSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(ActionFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, ActionFields.ID.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeActionStatusSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(ActionStatusFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, ActionStatusFields.ID.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeDistributionSetMetadataSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(DistributionSetMetadataFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, DistributionSetMetadataFields.KEY.getFieldName());
        }
        return sorting;
    }

    static Sort sanitizeSoftwareModuleMetadataSortParam(final String sortParam) {
        final Sort sorting;
        if (sortParam != null) {
            sorting = new Sort(SortUtility.parse(SoftwareModuleMetadataFields.class, sortParam));
        } else {
            // default sort
            sorting = new Sort(Direction.ASC, SoftwareModuleMetadataFields.KEY.getFieldName());
        }
        return sorting;
    }

}
