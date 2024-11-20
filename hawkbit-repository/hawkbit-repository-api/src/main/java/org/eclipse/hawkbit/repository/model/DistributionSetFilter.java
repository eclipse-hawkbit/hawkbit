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

import java.util.Collection;

import lombok.Builder;
import lombok.Data;

/**
 * Holds distribution set filter parameters.
 */
@Data
@Builder
public final class DistributionSetFilter {

    private final Boolean isDeleted;
    private final Boolean isComplete;
    private final Boolean isValid;
    private final Long typeId;
    private final String searchText;
    private final Boolean selectDSWithNoTag;
    private final Collection<String> tagNames;
}