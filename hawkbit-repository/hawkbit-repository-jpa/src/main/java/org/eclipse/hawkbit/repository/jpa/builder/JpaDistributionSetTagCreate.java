/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.builder;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.repository.RepositoryManagement;
import org.eclipse.hawkbit.repository.builder.AbstractTagUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;

/**
 * Create/build implementation.
 */
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class JpaDistributionSetTagCreate
        extends AbstractTagUpdateCreate<TagCreate<JpaDistributionSetTag>>
        implements TagCreate<JpaDistributionSetTag> {

    @Override
    public JpaDistributionSetTag build() {
        return new JpaDistributionSetTag(name, description, colour);
    }
}