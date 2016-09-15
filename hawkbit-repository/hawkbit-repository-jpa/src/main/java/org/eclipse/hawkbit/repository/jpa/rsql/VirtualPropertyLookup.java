/**
 * Copyright (c) 2016 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

/**
 * Implementations map a placeholder to the associated value.
 * <p>
 * This is used in context of string replacement.
 */
public interface VirtualPropertyLookup {

    /**
     * Looks up a placeholder to the associated value.
     *
     * @param placeholder
     *            the virtual property that should be resolved by a value
     * @return the value for the placeholder; may be <code>null</code> if no
     *         value could be found for the given placeholder;
     */
    public String lookup(String placeholder);
}
