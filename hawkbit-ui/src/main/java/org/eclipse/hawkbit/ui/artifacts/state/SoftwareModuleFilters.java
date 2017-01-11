/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.artifacts.state;

import java.io.Serializable;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Softwrae module filters.
 *
 *
 */
@VaadinSessionScope
@SpringComponent
public class SoftwareModuleFilters implements Serializable {

    private static final long serialVersionUID = -5251492630546463593L;

    private SoftwareModuleType softwareModuleType;

    private String searchText;

    public Optional<SoftwareModuleType> getSoftwareModuleType() {
        return Optional.ofNullable(softwareModuleType);
    }

    public void setSoftwareModuleType(final SoftwareModuleType softwareModuleType) {
        this.softwareModuleType = softwareModuleType;
    }

    public Optional<String> getSearchText() {
        return Optional.ofNullable(searchText);
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

}
