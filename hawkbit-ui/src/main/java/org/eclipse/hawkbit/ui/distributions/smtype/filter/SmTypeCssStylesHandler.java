/** 
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.smtype.filter;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.SoftwareModuleTypeManagement;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.repository.model.Type;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIDefinitions;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;

import com.vaadin.server.Page;

/**
 * Css style handler for software module type
 */
public class SmTypeCssStylesHandler {
    private final SoftwareModuleTypeManagement softwareModuleTypeManagement;

    /**
     * Constructor for SmTypeCssStylesHandler
     *
     * @param softwareModuleTypeManagement
     *            SoftwareModuleTypeManagement
     */
    public SmTypeCssStylesHandler(final SoftwareModuleTypeManagement softwareModuleTypeManagement) {
        this.softwareModuleTypeManagement = softwareModuleTypeManagement;
    }

    /**
     * Update css styling od software module type
     */
    public void updateSmTypeStyles() {
        final String recreateStylesheetScript = String.format("const stylesheet = recreateStylesheet('%s').sheet;",
                UIComponentIdProvider.SM_TYPE_COLOR_STYLE);
        final String addStyleRulesScript = buildStyleRulesScript(getSmTypeIdWithColor(getAllSmTypes()));

        Page.getCurrent().getJavaScript().execute(recreateStylesheetScript + addStyleRulesScript);
    }

    private List<SoftwareModuleType> getAllSmTypes() {
        return HawkbitCommonUtil.getEntitiesByPageableProvider(softwareModuleTypeManagement::findAll);
    }

    private static Map<Long, String> getSmTypeIdWithColor(final List<SoftwareModuleType> smTypes) {
        return smTypes.stream().collect(Collectors.toMap(Type::getId,
                type -> Optional.ofNullable(type.getColour()).orElse(SPUIDefinitions.DEFAULT_COLOR)));
    }

    private static String buildStyleRulesScript(final Map<Long, String> typeIdWithColor) {
        return typeIdWithColor.entrySet().stream().map(entry -> {
            final String typeClass = String.join("-", UIComponentIdProvider.SM_TYPE_COLOR_CLASS,
                    String.valueOf(entry.getKey()));
            final String typeColor = entry.getValue();

            // "!important" is needed because we are overriding valo theme here
            // (alternatively we could provide more specific selector)
            return String.format(
                    "addStyleRule(stylesheet, '.%1$s, .%1$s > td, .%1$s .v-grid-cell', "
                            + "'background-color:%2$s !important; background-image: none !important;')",
                    typeClass, typeColor);
        }).collect(Collectors.joining(";"));
    }
}
