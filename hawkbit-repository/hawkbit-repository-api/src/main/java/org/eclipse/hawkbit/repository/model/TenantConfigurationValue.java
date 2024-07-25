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

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a tenant configuration value including some meta data
 * 
 * @param <T> type of the configuration value
 */
@Data
@Builder
public final class TenantConfigurationValue<T extends Serializable> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private T value;
    private Long lastModifiedAt;
    private String lastModifiedBy;
    private Long createdAt;
    private String createdBy;
    private boolean global = true;
}