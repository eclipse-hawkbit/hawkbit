/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Unstructured tenant configuration elements. Can be used to store arbitrary
 * tenant configuration elements.
 *
 */
public interface TenantConfiguration extends TenantAwareBaseEntity {

    /**
     * @return key of the entry
     */
    String getKey();

    /**
     * @param key
     *            of the entry
     */
    void setKey(String key);

    /**
     * @return value of the entry
     */
    String getValue();

    /**
     * @param value
     *            of the entry
     */
    void setValue(String value);

}