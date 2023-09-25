/**
 * Copyright (c) 2022 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.tenantconfiguration.repository;

import com.vaadin.ui.VerticalLayout;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;

/**
 * This class represents the UI item for enabling/disabling the
 * User-consent flow feature as part of the repository configuration view.
 */
public class ConfirmationFlowConfigurationItem extends VerticalLayout {

  private static final long serialVersionUID = 1L;

  private static final String MSG_KEY_CHECKBOX = "label.configuration.repository.confirmationflow";

  /**
   * Constructor.
   *
   * @param i18n
   *            VaadinMessageSource
   */
  public ConfirmationFlowConfigurationItem(final VaadinMessageSource i18n) {
    this.setSpacing(false);
    this.setMargin(false);
    addComponent(SPUIComponentProvider.generateLabel(i18n, MSG_KEY_CHECKBOX));
  }
}