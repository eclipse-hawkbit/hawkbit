/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common;

import java.util.Set;

/**
 * Interface for all entity states UI to show the details to a entity.
 */
public interface ManagementEntityState {

    /**
     * The selected entities for the detail.
     * 
     * @param values
     *            the selected entities.
     * 
     */
    void setSelectedEnitities(Set<Long> values);

    /**
     * The last selected value.
     * 
     * @param value
     *            the value
     */
    void setLastSelectedEntityId(Long value);

}
