/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.management.state;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.hawkbit.ui.common.entity.TargetIdName;

import com.vaadin.spring.annotation.SpringComponent;
import com.vaadin.spring.annotation.VaadinSessionScope;

/**
 *
 *
 *
 */
@VaadinSessionScope
@SpringComponent
public class DistributionTableFilters implements Serializable {

    private static final long serialVersionUID = -5251492630546463593L;

    private String searchText;

    private Long distId;

    private TargetIdName pinnedTarget;

    private final List<String> distSetTags = new ArrayList<>();

    private List<String> clickedDistSetTags = new ArrayList<>();

    private Boolean noTagSelected = Boolean.FALSE;

    public void setNoTagSelected(final Boolean noTagSelected) {
        this.noTagSelected = noTagSelected;
    }

    public Boolean isNoTagSelected() {
        return noTagSelected;
    }

    public List<String> getDistSetTags() {
        return distSetTags;
    }

    public List<String> getClickedDistSetTags() {
        return clickedDistSetTags;
    }

    public void setClickedDistSetTags(final List<String> clickedDistSetTags) {
        this.clickedDistSetTags = clickedDistSetTags;
    }

    public Optional<Long> getDistId() {
        return Optional.ofNullable(distId);
    }

    public void setDistId(final Long distId) {
        this.distId = distId;
    }

    public Optional<TargetIdName> getPinnedTarget() {
        return Optional.ofNullable(pinnedTarget);
    }

    public void setPinnedTarget(final TargetIdName pinnedTarget) {
        this.pinnedTarget = pinnedTarget;
    }

    public Optional<String> getSearchText() {
        return Optional.ofNullable(searchText);
    }

    public void setSearchText(final String searchText) {
        this.searchText = searchText;
    }

}
