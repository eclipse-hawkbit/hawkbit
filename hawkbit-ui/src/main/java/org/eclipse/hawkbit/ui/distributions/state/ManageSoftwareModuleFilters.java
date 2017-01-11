/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.distributions.state;

import java.io.Serializable;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.SoftwareModuleType;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 *
 *
 */
@SpringComponent
@VaadinSessionScope
public class ManageSoftwareModuleFilters implements Serializable {

    private static final long serialVersionUID = -1631725636290496525L;

    private SoftwareModuleType softwareModuleType;

    private String searchText;

    /**
     * @return the softwareModuleType
     */
    public Optional<SoftwareModuleType> getSoftwareModuleType() {
        return Optional.ofNullable(softwareModuleType);
    }

    /**
     * @param softwareModuleType
     *            the softwareModuleType to set
     */
    public void setSoftwareModuleType(final SoftwareModuleType softwareModuleType) {
        this.softwareModuleType = softwareModuleType;
    }

    /**
     * @return the searchText
     */
    public Optional<String> getSearchText() {
        return Optional.ofNullable(searchText);
    }

    /**
     * @param searchText
     *            the searchText to set
     */
    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }
}
