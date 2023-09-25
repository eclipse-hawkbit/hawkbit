/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common;

/**
 * Interface for getting view name
 *
 */
@FunctionalInterface
public interface ViewNameAware {

    /**
     * Provides the name of a view
     *
     * @return view name
     */
    String getViewName();
}
