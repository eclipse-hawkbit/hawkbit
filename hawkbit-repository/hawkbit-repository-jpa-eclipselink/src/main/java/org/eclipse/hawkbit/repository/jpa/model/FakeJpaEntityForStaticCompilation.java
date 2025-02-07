/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.model;

import java.io.Serial;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import lombok.NoArgsConstructor;

/**
 * Fake JPA entity to allow proper static compilation. It seems that if there is no actual (class) entity using AbstractJpaBaseEntity,
 * the static compilation is not proper.
 */
@NoArgsConstructor(access = lombok.AccessLevel.PACKAGE)
@Table(name = "fake_for_static_compilation")
@Entity
public class FakeJpaEntityForStaticCompilation extends AbstractJpaTenantAwareBaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;
}