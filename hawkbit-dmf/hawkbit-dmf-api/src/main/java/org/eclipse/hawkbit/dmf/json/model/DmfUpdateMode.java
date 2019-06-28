/**
 * Copyright (c) 2018 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.dmf.json.model;

/**
 * Enumerates the supported update modes. Each mode represents an attribute
 * update strategy.
 * 
 * @see DmfAttributeUpdate
 */
public enum DmfUpdateMode {

    /**
     * Merge update strategy
     */
    MERGE,

    /**
     * Replacement update strategy
     */
    REPLACE,

    /**
     * Removal update strategy
     */
    REMOVE

}
