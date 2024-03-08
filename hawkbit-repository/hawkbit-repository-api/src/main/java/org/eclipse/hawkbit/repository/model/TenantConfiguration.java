/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.model;

/**
 * Unstructured tenant configuration elements. Can be used to store arbitrary
 * tenant configuration elements.
 *
 */
public interface TenantConfiguration extends TenantAwareBaseEntity {

    /**
     * Maximum length of tenant configuration key.
     */
    int KEY_MAX_SIZE = 128;

    /**
     * Maximum length of tenant configuration value.
     */
    int VALUE_MAX_SIZE = 512;

    /**
     * @return key of the entry
     */
    String getKey();

    /**
     * @return value of the entry
     */
    String getValue();
}