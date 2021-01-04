/**
 * Copyright (c) 2019 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;

import java.util.Arrays;
import java.util.Locale;

import org.eclipse.hawkbit.ui.UiProperties;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.ui.UI;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Localization helper")
@Story("Test the locale configuration and prioritization")
public class HawkbitCommonUtilTest {

    @Test
    @Description("getCurrentLocale should return the set Locale in the UI if found, otherwise the default System Locale")
    public void getCurrentLocaleShouldReturnSetUILocaleOrDefaultSystemLocale() {
        final UI ui = Mockito.mock(UI.class);

        // GIVEN
        UI.setCurrent(null);
        // WHEN
        final Locale currentLocale = HawkbitCommonUtil.getCurrentLocale();
        // THEN
        assertEquals(Locale.getDefault(), currentLocale);

        // GIVEN
        UI.setCurrent(ui);
        doReturn(Locale.GERMAN).when(ui).getLocale();
        // WHEN
        final Locale currentLocale2 = HawkbitCommonUtil.getCurrentLocale();
        // THEN
        assertEquals(Locale.GERMAN, currentLocale2);
    }

    @Test
    @Description("If a default locale is set in the environment, then it should take perceedence over requested browser locale")
    public void getLocaleToBeUsedShouldReturnDefaultLocalIfSet() {
        final UiProperties.Localization localizationProperties = Mockito.mock(UiProperties.Localization.class);

        // GIVEN
        doReturn(Locale.GERMAN).when(localizationProperties).getDefaultLocal();
        // WHEN
        final Locale localeToBeUsed = HawkbitCommonUtil.getLocaleToBeUsed(localizationProperties, Locale.CHINESE);
        // THEN
        assertEquals(Locale.GERMAN, localeToBeUsed);
    }

    @Test
    @Description("If no default locale is set in the environment, then the requested browser locale may be used if supported")
    public void getLocaleToBeUsedShouldReturnRequestedLocalIfSupportedAndNoDefaultIsSet() {
        final UiProperties.Localization localizationProperties = Mockito.mock(UiProperties.Localization.class);

        // GIVEN
        doReturn(null).when(localizationProperties).getDefaultLocal();
        doReturn(Arrays.asList(Locale.ENGLISH, Locale.GERMAN)).when(localizationProperties).getAvailableLocals();

        // WHEN
        final Locale localeToBeUsed = HawkbitCommonUtil.getLocaleToBeUsed(localizationProperties, Locale.GERMAN);
        // THEN
        assertEquals(Locale.GERMAN, localeToBeUsed);
    }

}
