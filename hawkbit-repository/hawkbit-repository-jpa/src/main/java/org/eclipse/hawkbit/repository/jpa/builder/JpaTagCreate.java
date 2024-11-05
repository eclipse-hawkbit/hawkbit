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

import org.eclipse.hawkbit.repository.builder.AbstractTagUpdateCreate;
import org.eclipse.hawkbit.repository.builder.TagCreate;
import org.eclipse.hawkbit.repository.jpa.model.JpaDistributionSetTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTag;
import org.eclipse.hawkbit.repository.jpa.model.JpaTargetTag;
import org.eclipse.hawkbit.repository.model.Tag;

/**
 * Create/build implementation.
 */
public class JpaTagCreate extends AbstractTagUpdateCreate<TagCreate> implements TagCreate {

    JpaTagCreate() {

    }

    public JpaDistributionSetTag buildDistributionSetTag() {
        return new JpaDistributionSetTag(name, description, colour);
    }

    public JpaTargetTag buildTargetTag() {
        return new JpaTargetTag(name, description, colour);
    }

    @Override
    public Tag build() {
        return new JpaTag(name, description, colour);
    }
}
