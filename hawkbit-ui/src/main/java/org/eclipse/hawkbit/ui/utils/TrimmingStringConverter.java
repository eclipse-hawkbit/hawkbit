/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        return Result.ok(StringUtils.trimWhitespace(s));
    }

    @Override
    public String convertToPresentation(final String s, final ValueContext valueContext) {
        return StringUtils.trimWhitespace(s);
    }
}
