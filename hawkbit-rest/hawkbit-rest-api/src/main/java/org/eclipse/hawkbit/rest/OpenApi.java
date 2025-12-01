/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.rest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.swagger.v3.oas.models.tags.Tag;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class OpenApi {

    public static final String HAWKBIT_SERVER_OPENAPI_ENABLED = "hawkbit.server.openapi.enabled";

    public static final String X_HAWKBIT = "x-hawkbit";
    public static final String ORDER = "order";

    public static List<Tag> sort(final List<Tag> tags) {
        tags.sort(TAG_COMPARATOR);
        return tags;
    }

    private static final Comparator<Tag> TAG_COMPARATOR = new Comparator<>() {

        @Override
        public int compare(final Tag o1, final Tag o2) {
            final int o1Order = order(o1);
            final int o2Order = order(o2);
            if (o1Order == o2Order) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return Integer.compare(o1Order, o2Order);
            }
        }

        private static int order(final Tag tag) {
            return Optional.ofNullable(tag.getExtensions())
                    .map(extensions -> extensions.get(X_HAWKBIT))
                    .filter(extension -> Map.class.isAssignableFrom(extension.getClass()))
                    .map(Map.class::cast)
                    .map(propertiesMap -> propertiesMap.get(ORDER))
                    .map(String.class::cast)
                    .map(Integer::parseInt)
                    .orElse(0);
        }
    };
}