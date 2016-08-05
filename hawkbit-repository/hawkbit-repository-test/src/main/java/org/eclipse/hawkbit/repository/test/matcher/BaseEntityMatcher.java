/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.test.matcher;

import org.eclipse.hawkbit.repository.model.BaseEntity;
import org.hamcrest.Factory;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;

/**
 * Matcher for {@link BaseEntity}.
 */
public final class BaseEntityMatcher {

    private BaseEntityMatcher() {
    }

    @Factory
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
