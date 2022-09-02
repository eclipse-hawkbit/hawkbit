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
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;

import com.cronutils.utils.StringUtils;

/**
 * Safe html constraint validator for strings submitted into the repository.
 *
 */
public class ValidStringValidator implements ConstraintValidator<ValidString, String> {

    private final Cleaner cleaner = new Cleaner(Safelist.none());

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return StringUtils.isEmpty(value) || cleaner.isValid(stringToDocument(value));
    }

    private static Document stringToDocument(final String value) {
        final Document xmlFragment = Jsoup.parse(value, "", Parser.xmlParser());
        final Document resultingDocument = Document.createShell("");

        xmlFragment.childNodes().forEach(xmlNode -> resultingDocument.body().appendChild(xmlNode.clone()));

        return resultingDocument;
    }

}
