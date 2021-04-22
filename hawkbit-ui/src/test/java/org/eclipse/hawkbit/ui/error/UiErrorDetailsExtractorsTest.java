/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.ui.error.extractors.ConstraintViolationErrorExtractor;
import org.eclipse.hawkbit.ui.error.extractors.UiErrorDetailsExtractor;
import org.eclipse.hawkbit.ui.error.extractors.UploadErrorExtractor;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.google.gwt.validation.client.impl.ConstraintViolationImpl;
import com.google.gwt.validation.client.impl.PathImpl;
import com.vaadin.server.UploadException;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Management UI")
@Story("Test correct behaviour of the UI error details extractors")
@ExtendWith(MockitoExtension.class)
class UiErrorDetailsExtractorsTest {

    private final static String DEFAULT_CAPTION = "defaultCaption";
    private final static String VIOLATION_DESCRIPTION = "violationDescription";

    @Mock
    private VaadinMessageSource i18n;

    @Test
    @Description("Extractor finds the matching exception in case of a nested error")
    void nestedExceptionIsFoundByExtractor() {
        final UiErrorDetailsExtractor extractor = (error) -> Collections.emptyList();
        final MatchingException error = new MatchingException(
                new MatchingSecondLevelException(new MatchingThirdLevelException()));

        final Optional<MatchingException> matchingError = extractor.findExceptionOf(error, MatchingException.class);
        final Optional<MatchingSecondLevelException> parentError = extractor.findExceptionOf(error,
                MatchingSecondLevelException.class);
        final Optional<MatchingThirdLevelException> grandparentError = extractor.findExceptionOf(error,
                MatchingThirdLevelException.class);

        assertThat(matchingError).isPresent().hasValue(error);
        assertThat(parentError).isPresent().hasValue((MatchingSecondLevelException) error.getCause());
        assertThat(grandparentError).isPresent().hasValue((MatchingThirdLevelException) error.getCause().getCause());
    }

    @Test
    @Description("Extractor finds the matching exception in case of parent/child relationships between error classes")
    void parentExceptionIsFoundByExtractor() {
        final UiErrorDetailsExtractor extractor = (error) -> Collections.emptyList();
        final MatchingException error = new MatchingException();

        final Optional<ParentMatchingException> matchingError = extractor.findExceptionOf(error,
                ParentMatchingException.class);

        assertThat(matchingError).isPresent().hasValue(error);
    }

    @Test
    @Description("Extractor finds details of constraint violation exception")
    void extractsConstraintViolationException() {
        when(i18n.getMessage("caption.error")).thenReturn(DEFAULT_CAPTION);

        final UiErrorDetailsExtractor extractor = new ConstraintViolationErrorExtractor(i18n);
        final MatchingException emptyMessageError = new MatchingException(new ConstraintViolationException(null));
        final MatchingException emptyViolationsError = new MatchingException(
                new ConstraintViolationException(VIOLATION_DESCRIPTION, null));
        final ConstraintViolationException violationsError = new ConstraintViolationException(VIOLATION_DESCRIPTION,
                buildConstraintViolations());

        final List<UiErrorDetails> emptyMessageErrorDetails = extractor.extractErrorDetailsFrom(emptyMessageError);
        final List<UiErrorDetails> emptyViolationsErrorDetails = extractor
                .extractErrorDetailsFrom(emptyViolationsError);
        final List<UiErrorDetails> violationsErrorDetails = extractor.extractErrorDetailsFrom(violationsError);

        assertThat(emptyMessageErrorDetails).hasSize(1).first().satisfies(details -> {
            assertThat(details.getCaption()).isEqualTo(DEFAULT_CAPTION);
            assertThat(details.getDescription()).isEqualTo("MatchingException");
        });
        assertThat(emptyViolationsErrorDetails).hasSize(1).first().satisfies(details -> {
            assertThat(details.getCaption()).isEqualTo(DEFAULT_CAPTION);
            assertThat(details.getDescription()).isEqualTo(VIOLATION_DESCRIPTION);
        });
        assertThat(violationsErrorDetails).hasSize(1).first().satisfies(details -> {
            assertThat(details.getCaption()).isEqualTo(DEFAULT_CAPTION);
            assertThat(details.getDescription()).contains(VIOLATION_DESCRIPTION).contains("firstProperty invalid")
                    .contains("secondProperty invalid");
        });
    }

    @Test
    @Description("Extractor ignores details of upload exception (errors are handled explicitely)")
    void ignoresUploadException() {
        final UiErrorDetailsExtractor extractor = new UploadErrorExtractor();
        final UploadException uploadError = new UploadException("ignored");

        final List<UiErrorDetails> errorDetails = extractor.extractErrorDetailsFrom(uploadError);

        assertThat(errorDetails).hasSize(1).first().isEqualTo(UiErrorDetails.empty());
    }

    private Set<ConstraintViolation<Object>> buildConstraintViolations() {
        final ConstraintViolationImpl<Object> violation1 = ConstraintViolationImpl.builder()
                .setPropertyPath(new PathImpl().append("firstProperty")).setMessage("invalid").build();
        final ConstraintViolationImpl<Object> violation2 = ConstraintViolationImpl.builder()
                .setPropertyPath(new PathImpl().append("secondProperty")).setMessage("invalid").build();

        return new HashSet<>(Arrays.asList(violation1, violation2));
    }

    private static class ParentMatchingException extends Exception {
        private static final long serialVersionUID = 1L;

        public ParentMatchingException() {
            super();
        }

        public ParentMatchingException(final Throwable cause) {
            super(cause);
        }
    }

    private static class MatchingException extends ParentMatchingException {
        private static final long serialVersionUID = 1L;

        public MatchingException() {
            super();
        }

        public MatchingException(final Throwable cause) {
            super(cause);
        }
    }

    private static class MatchingSecondLevelException extends Exception {
        private static final long serialVersionUID = 1L;

        public MatchingSecondLevelException(final Throwable cause) {
            super(cause);
        }
    }

    private static class MatchingThirdLevelException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
