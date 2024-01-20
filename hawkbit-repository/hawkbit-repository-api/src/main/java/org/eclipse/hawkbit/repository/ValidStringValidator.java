/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cronutils.utils.StringUtils;

/**
 * Safe html constraint validator for strings submitted into the repository.
 *
 */
public class ValidStringValidator implements ConstraintValidator<ValidString, String> {
    private static final Logger LOG = LoggerFactory.getLogger(ValidStringValidator.class);

    private final Cleaner cleaner = new Cleaner(Safelist.none());

    @Override
    public boolean isValid(final String value, final ConstraintValidatorContext context) {
        return StringUtils.isEmpty(value) || isValidString(value);
    }

    private boolean isValidString(final String value) {
        try {
            return cleaner.isValid(stringToDocument(value));
        } catch (final Exception ex) {
            LOG.error(String.format("There was an exception during bean field value (%s) validation", value), ex);
            return false;
        }
    }

    private static Document stringToDocument(final String value) {
        final Document xmlFragment = Jsoup.parse(value, "", Parser.xmlParser());
        final Document resultingDocument = Document.createShell("");

        xmlFragment.childNodes().forEach(xmlNode -> resultingDocument.body().appendChild(xmlNode.clone()));

        return resultingDocument;
    }

}
