/**
 * Copyright (c) 2011-2015 Bosch Software Innovations GmbH, Germany. All rights reserved.
 */
package org.eclipse.hawkbit.ui.filter.target;

import java.net.URI;

import org.eclipse.hawkbit.repository.model.Target;
import org.eclipse.hawkbit.ui.filter.FilterExpression;

/**
 *
 *
 *
 */
public class TargetSearchTextFilter implements FilterExpression {

    private final Target target;
    private final String searchTextUpper;

    /**
     * @param target
     *            the target to check against the search text
     * @param searchText
     *            the search text check against the given target
     */
    public TargetSearchTextFilter(final Target target, final String searchText) {
        this.target = target;
        this.searchTextUpper = searchText.toUpperCase();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.hawkbit.server.ui.filter.FilterExpression#evaluate()
     */
    @Override
    public boolean doFilter() {
        return !(descriptionIgnoreCase() || nameIgnoreCase() || controllerIdIgnoreCase() || ipAddressIgnoreCase());
    }

    private boolean descriptionIgnoreCase() {
        if (target.getDescription() == null) {
            return false;
        }
        return target.getDescription().toUpperCase().contains(searchTextUpper);
    }

    private boolean nameIgnoreCase() {
        if (target.getName() == null) {
            return false;
        }
        return target.getName().toUpperCase().contains(searchTextUpper);
    }

    private boolean controllerIdIgnoreCase() {
        return target.getControllerId().toUpperCase().contains(searchTextUpper);
    }

    private boolean ipAddressIgnoreCase() {
        final URI targetAddress = target.getTargetInfo().getAddress();
        if (targetAddress == null || targetAddress.getHost() == null) {
            return false;
        }
        return targetAddress.getHost().toUpperCase().contains(searchTextUpper);
    }
}
