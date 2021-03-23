/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error.extractors;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.ui.error.UiErrorDetails;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * UI error details extractor for {@link ConstraintViolationException}.
 */
public class ConstraintViolationErrorExtractor extends AbstractSingleUiErrorDetailsExtractor {
    private final VaadinMessageSource i18n;

    /**
     * Constructor for ConstraintViolationErrorExtractor.
     *
     * @param i18n
     *            Message source used for localization
     */
    public ConstraintViolationErrorExtractor(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected Optional<UiErrorDetails> findDetails(final Throwable error) {
        return findExceptionOf(error, ConstraintViolationException.class).map(ex -> {
            final Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
            final String description = violations == null ? error.getClass().getSimpleName()
                    : formatViolations(violations);

            return UiErrorDetails.create(i18n.getMessage("caption.error"), description);
        });
    }

    private static String formatViolations(final Set<ConstraintViolation<?>> violations) {
        return violations.stream().map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
