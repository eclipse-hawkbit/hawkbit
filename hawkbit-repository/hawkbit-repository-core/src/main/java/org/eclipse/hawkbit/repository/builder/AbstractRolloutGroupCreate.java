/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.builder;

import org.eclipse.hawkbit.repository.ValidString;
import org.eclipse.hawkbit.repository.model.RolloutGroupConditions;
import org.springframework.util.StringUtils;

/**
 * Create builder DTO.
 *
 * @param <T> update or create builder interface
 */
public abstract class AbstractRolloutGroupCreate<T> extends AbstractNamedEntityBuilder<T> {

    @ValidString
    protected String targetFilterQuery;
    protected Float targetPercentage;
    protected RolloutGroupConditions conditions;
    protected boolean confirmationRequired;

    public T targetFilterQuery(final String targetFilterQuery) {
        this.targetFilterQuery = StringUtils.trimWhitespace(targetFilterQuery);
        return (T) this;
    }

    public T targetPercentage(final Float targetPercentage) {
        this.targetPercentage = targetPercentage;
        return (T) this;
    }

    public T conditions(final RolloutGroupConditions conditions) {
        this.conditions = conditions;
        return (T) this;
    }

    public T confirmationRequired(final boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
        return (T) this;
    }
}