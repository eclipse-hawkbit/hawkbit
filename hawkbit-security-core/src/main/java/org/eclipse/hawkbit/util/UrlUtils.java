/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.util;

import java.nio.charset.StandardCharsets;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.web.util.UriUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UrlUtils {

    public static String decodeUriValue(final String value) {
        return UriUtils.decode(value, StandardCharsets.UTF_8);
    }
}