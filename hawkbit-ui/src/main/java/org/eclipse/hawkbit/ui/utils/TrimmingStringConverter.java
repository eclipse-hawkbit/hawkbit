/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.utils;

import org.springframework.util.StringUtils;

import com.vaadin.data.Converter;
import com.vaadin.data.Result;
import com.vaadin.data.ValueContext;

/**
 * Converter to trim whitespaces for an input e.g. to further convert it into an
 * Integer, etc.
 */
public class TrimmingStringConverter implements Converter<String, String> {
    @Override
    public Result<String> convertToModel(final String s, final ValueContext valueContext) {
        return Result.ok(trimmedString(s));
    }

    @Override
    public String convertToPresentation(final String s, final ValueContext valueContext) {
        return trimmedString(s);
    }

    private static String trimmedString(final String s){
        return s == null ? "" : StringUtils.trimWhitespace(s);
    }
}
