/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import org.eclipse.hawkbit.repository.model.ExternalArtifactProvider;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository for {@link ExternalArtifactProvider}.
 *
 *
 *
 *
 */
@Transactional(readOnly = true)
public interface ExternalArtifactProviderRepository extends BaseEntityRepository<ExternalArtifactProvider, Long> {

}
