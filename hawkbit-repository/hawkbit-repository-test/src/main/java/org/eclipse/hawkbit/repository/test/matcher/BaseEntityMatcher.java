/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.test.matcher;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Matcher for {@link BaseEntity}.
 */
public final class BaseEntityMatcher {

    private BaseEntityMatcher() {
    }

    public static Matcher<BaseEntity> hasId(final Long id) {
        return new HasIdMatcher(Matchers.equalTo(id));
    }

    private static class HasIdMatcher extends FeatureMatcher<BaseEntity, Long> {

        public HasIdMatcher(final Matcher<Long> subMatcher) {
            super(subMatcher, "getId()", "id");
        }

        @Override
        protected Long featureValueOf(final BaseEntity baseEntity) {
            return baseEntity.getId();
        }
    }

}
