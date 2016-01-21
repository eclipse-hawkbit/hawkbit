/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.utils;

import static org.fest.assertions.api.Assertions.assertThat;

import org.eclipse.hawkbit.ui.components.SPUIButton;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.decorators.SPUIButtonStyleSmallNoBorderUH;
import org.junit.Test;

import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Label;

/**
 * Unit Test block for UI Component provider. Dynamic Factory Testing.
 * 
 *
 *
 */
public class SPUIComponentProviderTest {
    /**
     * Test case for check button factory.
     * 
     * @throws Exception
     */
    @Test
    public void checkButtonFactory() throws Exception {

        // Checking Dyanmic Factory
        Button placeHolderButton = null;
        placeHolderButton = SPUIComponentProvider.getButton("", "Test", "Test",
                SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE, true, null, SPUIButtonStyleSmallNoBorderUH.class);
        assertThat(placeHolderButton).isInstanceOf(SPUIButton.class);
        assertThat(placeHolderButton.getCaption()).isEqualTo("Test");
        assertThat(placeHolderButton.getStyleName()).isEqualTo(SPUIButtonDefinitions.SP_BUTTON_STATUS_STYLE);
    }

    /**
     * Test case for check Label factory.
     * 
     * @throws Exception
     */
    @Test
    public void checkLabelFactory() throws Exception {

        Label placeHolderLabel = null;
        placeHolderLabel = SPUIComponentProvider.getLabel("TestTable", SPUILabelDefinitions.SP_WIDGET_CAPTION);
        assertThat(placeHolderLabel).isInstanceOf(Label.class);
        assertThat(placeHolderLabel.getValue()).isEqualTo("TestTable");
        assertThat(placeHolderLabel.getContentMode()).isNotEqualTo(ContentMode.HTML);
        placeHolderLabel = SPUIComponentProvider.getLabel("TestMSG", SPUILabelDefinitions.SP_LABEL_MESSAGE);
        assertThat(placeHolderLabel.getValue()).isEqualTo("TestMSG");
        assertThat(placeHolderLabel.getContentMode()).isEqualTo(ContentMode.HTML);
    }

}
