/**
 * Copyright (c) 2022 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import com.cronutils.utils.StringUtils;

public class ValidStringValidator implements ConstraintValidator<ValidString, String> {

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return StringUtils.isEmpty(value) || Jsoup.isValid(value, Safelist.none());
    }

}
