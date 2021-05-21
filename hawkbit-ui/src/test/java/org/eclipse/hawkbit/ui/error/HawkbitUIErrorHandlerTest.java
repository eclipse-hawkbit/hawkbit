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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.hawkbit.ui.error.extractors.UiErrorDetailsExtractor;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.vaadin.server.ErrorEvent;
import com.vaadin.server.Page;
import com.vaadin.ui.Notification;
import com.vaadin.ui.UI;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Step;
import io.qameta.allure.Story;

@Feature("Unit Tests - Management UI")
@Story("Test the UI error handling with different error extractors")
@ExtendWith(MockitoExtension.class)
class HawkbitUIErrorHandlerTest {

    private final static String DEFAULT_CAPTION_CODE = "caption.error";
    private final static String DEFAULT_CAPTION = "defaultCaption";
    private final static String DEFAULT_DESCRIPTION_CODE = "message.error";
    private final static String DEFAULT_DESCRIPTION = "defaultDescription";
    private final static String MATCHING_CAPTION = "matchingCaption";
    private final static String MATCHING_DESCRIPTION = "matchingDescription";
    private final static String UNMATCHING_CAPTION = "unmatchingCaption";
    private final static String UNMATCHING_DESCRIPTION = "unmatchingDescription";

    @Mock
    private VaadinMessageSource i18n;

    @Mock
    private UI ui;

    @Mock
    private Page page;

    private HawkbitUIErrorHandler errorHandler;

    @Test
    @Description("Generic error notification is shown in case no error details extractors are provided")
    void showsGenericErrorNotificationOnMissingExtractors() {
        initUi();
        initI18n();

        handleErrorWithExtractors(new UnmatchingException(), Collections.emptyList());

        verifyDefaultI18nCalled();

        final Notification shownNotification = captureShownNotification().getValue();
        verifyNotificationContent(shownNotification, DEFAULT_CAPTION, DEFAULT_DESCRIPTION);
    }

    @Step
    private void handleErrorWithExtractors(final Throwable error, final List<UiErrorDetailsExtractor> extractors) {
        errorHandler = Mockito.spy(new HawkbitUIErrorHandler(i18n, extractors));
        errorHandler.error(new ErrorEvent(error));
    }

    @Step
    private void verifyDefaultI18nCalled() {
        verify(i18n).getMessage(eq(DEFAULT_CAPTION_CODE));
        verify(i18n).getMessage(eq(DEFAULT_DESCRIPTION_CODE));
    }

    @Step
    private ArgumentCaptor<Notification> captureShownNotification() {
        return captureShownNotifications(1);
    }

    @Step
    private ArgumentCaptor<Notification> captureShownNotifications(final int count) {
        final ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
        verify(errorHandler, times(count)).showErrorNotification(eq(page), notificationCaptor.capture());

        return notificationCaptor;
    }

    @Step
    private void verifyNotificationContent(final Notification notification, final String caption,
            final String description) {
        assertThat(notification.getCaption()).isEqualTo(caption);
        assertThat(notification.getDescription()).isEqualTo(description);
    }

    @Test
    @Description("Generic error notification is shown in case error doesn't match any provided error details extractor")
    void showsGenericErrorNotificationOnNonMatchingExtractor() {
        initUi();
        initI18n();

        final UiErrorDetailsExtractor matchingExtractor = getMatchingErrorDetailsExtractor();
        handleErrorWithExtractors(new UnmatchingException(), Collections.singletonList(matchingExtractor));

        verifyDefaultI18nCalled();

        final Notification shownNotification = captureShownNotification().getValue();
        verifyNotificationContent(shownNotification, DEFAULT_CAPTION, DEFAULT_DESCRIPTION);
    }

    @Test
    @Description("Nothing is shown in case error details extractor ignored the error")
    void showsNothingOnIgnoreErrorDetailsExtractor() {
        final UiErrorDetailsExtractor ignoreMatchingExtractor = getMatchingIgnoreErrorDetailsExtractor();
        handleErrorWithExtractors(new MatchingException(), Collections.singletonList(ignoreMatchingExtractor));

        verify(errorHandler, times(0)).showErrorNotification(eq(page), any());
    }

    @Test
    @Description("Multiple error notifications are shown in case a list of error details is provided by "
            + "specific single error details extractor")
    void showsMultipleErrorNotificationsOnSingleMatchingExtractor() {
        initUi();

        final UiErrorDetailsExtractor matchingMultipleExtractor = getMatchingErrorDetailsExtractor(
                UiErrorDetails.create("matchingCaption1", "matchingDescription1"),
                UiErrorDetails.create("matchingCaption2", "matchingDescription2"));

        handleErrorWithExtractors(new MatchingException(), Collections.singletonList(matchingMultipleExtractor));

        final List<Notification> shownNotifications = captureShownNotifications(2).getAllValues();
        verifyNotificationContent(shownNotifications.get(0), "matchingCaption1", "matchingDescription1");
        verifyNotificationContent(shownNotifications.get(1), "matchingCaption2", "matchingDescription2");
    }

    @Test
    @Description("Specific error notification is shown only in case of multiple error details extractors")
    void showsSingleErrorNotificationOnMatchingErrorDetailsOnly() {
        initUi();

        final UiErrorDetailsExtractor matchingExtractor = getMatchingErrorDetailsExtractor();
        final UiErrorDetailsExtractor unmatchingExtractor = getUnmatchingErrorDetailsExtractor();
        handleErrorWithExtractors(new MatchingException(), Arrays.asList(matchingExtractor, unmatchingExtractor));

        final Notification shownNotification = captureShownNotification().getValue();
        verifyNotificationContent(shownNotification, MATCHING_CAPTION, MATCHING_DESCRIPTION);
    }

    @Test
    @Description("Specific error notification is shown only in case of multiple matching error details "
            + "extractors if one of the extractors ignores the error")
    void showsSingleErrorNotificationOnOneExtractorIgnore() {
        initUi();

        final UiErrorDetailsExtractor matchingExtractor = getMatchingErrorDetailsExtractor();
        final UiErrorDetailsExtractor ignoreMatchingExtractor = getMatchingIgnoreErrorDetailsExtractor();
        handleErrorWithExtractors(new MatchingException(), Arrays.asList(matchingExtractor, ignoreMatchingExtractor));

        final Notification shownNotification = captureShownNotification().getValue();
        verifyNotificationContent(shownNotification, MATCHING_CAPTION, MATCHING_DESCRIPTION);
    }

    @Test
    @Description("Multiple error notifications are shown in case of multiple matching error details extractors")
    void showsMultipleErrorNotificationsOnMatchingExtractors() {
        initUi();

        final UiErrorDetailsExtractor matchingExtractor = getMatchingErrorDetailsExtractor();
        final UiErrorDetailsExtractor anotherMatchingExtractor = getMatchingErrorDetailsExtractor(
                UiErrorDetails.create("anotherMatchingCaption", "anotherMatchingDescription"));
        handleErrorWithExtractors(new MatchingException(), Arrays.asList(matchingExtractor, anotherMatchingExtractor));

        final List<Notification> shownNotifications = captureShownNotifications(2).getAllValues();
        verifyNotificationContent(shownNotifications.get(0), MATCHING_CAPTION, MATCHING_DESCRIPTION);
        verifyNotificationContent(shownNotifications.get(1), "anotherMatchingCaption", "anotherMatchingDescription");
    }

    private void initUi() {
        when(ui.getPage()).thenReturn(page);
        when(page.getUI()).thenReturn(ui);
        UI.setCurrent(ui);
    }

    private void initI18n() {
        when(i18n.getMessage(eq(DEFAULT_CAPTION_CODE))).thenReturn(DEFAULT_CAPTION);
        when(i18n.getMessage(eq(DEFAULT_DESCRIPTION_CODE))).thenReturn(DEFAULT_DESCRIPTION);
    }

    private UiErrorDetailsExtractor getMatchingErrorDetailsExtractor() {
        return getMatchingErrorDetailsExtractor(UiErrorDetails.create(MATCHING_CAPTION, MATCHING_DESCRIPTION));
    }

    private UiErrorDetailsExtractor getMatchingErrorDetailsExtractor(final UiErrorDetails... errorDetails) {
        return (error) -> error instanceof MatchingException ? Arrays.asList(errorDetails) : Collections.emptyList();
    }

    private UiErrorDetailsExtractor getMatchingIgnoreErrorDetailsExtractor() {
        return (error) -> error instanceof MatchingException ? Collections.singletonList(UiErrorDetails.empty())
                : Collections.emptyList();
    }

    private UiErrorDetailsExtractor getUnmatchingErrorDetailsExtractor() {
        return (error) -> error instanceof UnmatchingException
                ? Collections.singletonList(UiErrorDetails.create(UNMATCHING_CAPTION, UNMATCHING_DESCRIPTION))
                : Collections.emptyList();
    }

    private static class MatchingException extends Exception {
        private static final long serialVersionUID = 1L;
    }

    private static class UnmatchingException extends Exception {
        private static final long serialVersionUID = 1L;
    }
}
