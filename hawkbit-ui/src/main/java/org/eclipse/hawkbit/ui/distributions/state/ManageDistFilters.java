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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.repository.model.DistributionSetType;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 * Distribution table filter state.
 */
@SpringComponent
@VaadinSessionScope
public class ManageDistFilters implements Serializable {

    private static final long serialVersionUID = 8264263498423250738L;

    private String searchText;

    private List<String> distSetTags = new ArrayList<>();

    private List<String> clickedDistSetTags = new ArrayList<>();

    private DistributionSetType clickedDistSetType;

    public DistributionSetType getClickedDistSetType() {
        return clickedDistSetType;
    }

    public void setClickedDistSetType(final DistributionSetType clickedDistSetType) {
        this.clickedDistSetType = clickedDistSetType;
    }

    public List<String> getDistSetTags() {
        return distSetTags;
    }

    public void setDistSetTags(final List<String> distSetTags) {
        this.distSetTags = distSetTags;
    }

    public List<String> getClickedDistSetTags() {
        return clickedDistSetTags;
    }

    public void setClickedDistSetTags(final List<String> clickedDistSetTags) {
        this.clickedDistSetTags = clickedDistSetTags;
    }

    public Optional<String> getSearchText() {
        return Optional.ofNullable(searchText);
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }
}
