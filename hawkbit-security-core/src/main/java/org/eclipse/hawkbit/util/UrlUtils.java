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

import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

public class UrlUtils {

  private UrlUtils() {
    // Util classes should not have public constructors
  }

  public static String decodeUriValue(String value) {
    return UriUtils.decode(value, StandardCharsets.UTF_8);
  }
}