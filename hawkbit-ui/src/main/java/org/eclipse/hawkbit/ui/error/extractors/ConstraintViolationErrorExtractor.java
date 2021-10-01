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

import org.apache.commons.lang3.StringUtils;
import org.eclipse.hawkbit.ui.error.UiErrorDetails;
import org.eclipse.hawkbit.ui.utils.UIMessageIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.springframework.util.CollectionUtils;

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
            final StringBuilder descriptionBuilder = new StringBuilder(getBasicDescription(ex, error));
            getViolationsDescription(ex).ifPresent(violationsDescription -> descriptionBuilder.append(":")
                    .append(System.lineSeparator()).append(violationsDescription));

            return UiErrorDetails.create(i18n.getMessage(UIMessageIdProvider.CAPTION_ERROR),
                    descriptionBuilder.toString());
        });
    }

    private static String getBasicDescription(final ConstraintViolationException ex, final Throwable error) {
        return StringUtils.isEmpty(ex.getMessage()) ? error.getClass().getSimpleName() : ex.getMessage();
    }

    private static Optional<String> getViolationsDescription(final ConstraintViolationException ex) {
        final Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
        if (!CollectionUtils.isEmpty(violations)) {
            return Optional.of(formatViolations(violations));
        }

        return Optional.empty();
    }

    private static String formatViolations(final Set<ConstraintViolation<?>> violations) {
        return violations.stream().map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
