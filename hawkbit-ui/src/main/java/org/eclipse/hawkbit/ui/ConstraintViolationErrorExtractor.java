package org.eclipse.hawkbit.ui;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;

import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

public class ConstraintViolationErrorExtractor extends AbstractUiErrorDetailsExtractor {
    private final VaadinMessageSource i18n;

    public ConstraintViolationErrorExtractor(final VaadinMessageSource i18n) {
        this.i18n = i18n;
    }

    @Override
    protected Optional<UiErrorDetails> processError(final Throwable error) {
        return findExceptionFrom(error, ConstraintViolationException.class).map(ex -> {
            final Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();
            final String description = violations == null ? error.getClass().getSimpleName()
                    : formatViolations(violations);

            return UiErrorDetails.create(i18n.getMessage("caption.error"), description);
        });
    }

    private String formatViolations(final Set<ConstraintViolation<?>> violations) {
        return violations.stream().map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .collect(Collectors.joining(System.lineSeparator()));
    }
}
