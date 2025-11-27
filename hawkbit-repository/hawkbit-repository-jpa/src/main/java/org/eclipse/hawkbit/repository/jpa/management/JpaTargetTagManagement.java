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

import org.eclipse.hawkbit.repository.TargetTagManagement;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.jpa.repository.TargetTagRepository;
import org.eclipse.hawkbit.repository.qfields.TargetTagFields;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * JPA implementation of {@link TargetTagManagement}.
 */
@Transactional(readOnly = true)
@Validated
@Service
@ConditionalOnBooleanProperty(prefix = "hawkbit.jpa", name = { "enabled", "target-tag-management" }, matchIfMissing = true)
public class JpaTargetTagManagement 
        extends AbstractJpaRepositoryManagement<JpaTargetTag, TargetTagManagement.Create, TargetTagManagement.Update, TargetTagRepository, TargetTagFields>
        implements TargetTagManagement<JpaTargetTag> {

    protected JpaTargetTagManagement(
            final TargetTagRepository targetTagRepository,
            final EntityManager entityManager) {
        super(targetTagRepository, entityManager);
    }
}