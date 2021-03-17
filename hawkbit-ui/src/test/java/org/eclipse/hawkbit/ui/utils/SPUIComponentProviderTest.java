/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmall;

import com.vaadin.ui.Button;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

/**
 * Unit Test block for UI Component provider. Dynamic Factory Testing.
 * 
 */
@Feature("Unit Tests - Management UI")
@Story("UI components")
public class SPUIComponentProviderTest {
    /**
     * Test case for check button factory.
     * 
     * @throws Exception
     */
    @Test
    public void checkButtonFactory() {

        // Checking Dyanmic Factory
        Button placeHolderButton;
        placeHolderButton = SPUIComponentProvider.getButton("", "Test", "Test", SPUIDefinitions.SP_BUTTON_STATUS_STYLE,
                true, null, SPUIButtonStyleSmall.class);
        assertThat(placeHolderButton).isInstanceOf(SPUIButton.class);
        assertThat(placeHolderButton.getCaption()).isEqualTo("Test");
        assertThat(placeHolderButton.getStyleName()).isEqualTo(SPUIDefinitions.SP_BUTTON_STATUS_STYLE);
    }

}
