/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.management;

import jakarta.persistence.EntityManager;

import org.eclipse.hawkbit.repository.DistributionSetTagManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.repository.DistributionSetTagRepository;
import org.eclipse.hawkbit.repository.qfields.DistributionSetTagFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "distribution-set-tag-management" }, matchIfMissing = true)
public class JpaDistributionSetTagManagement
        extends AbstractJpaRepositoryManagement<JpaDistributionSetTag, DistributionSetTagManagement.Create, DistributionSetTagManagement.Update, DistributionSetTagRepository, DistributionSetTagFields>
        implements DistributionSetTagManagement<JpaDistributionSetTag> {

    protected JpaDistributionSetTagManagement(
            final DistributionSetTagRepository distributionSetTagRepository,
            final EntityManager entityManager) {
        super(distributionSetTagRepository, entityManager);
    }
}