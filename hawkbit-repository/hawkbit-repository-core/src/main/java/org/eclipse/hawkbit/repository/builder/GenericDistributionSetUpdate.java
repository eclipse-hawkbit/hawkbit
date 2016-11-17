/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.builder;

/**
 * Update implementation.
 */
public class GenericDistributionSetUpdate extends AbstractDistributionSetUpdateCreate<DistributionSetUpdate>
        implements DistributionSetUpdate {

    public GenericDistributionSetUpdate(final Long id) {
        super.id = id;
    }

}
