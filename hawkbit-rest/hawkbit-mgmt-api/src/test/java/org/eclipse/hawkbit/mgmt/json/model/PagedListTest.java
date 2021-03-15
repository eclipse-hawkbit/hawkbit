/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.mgmt.json.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Management API")
@Story("Paged List Handling")
public class PagedListTest {

    @Test
    @Description("Ensures that a null payload entitiy throws an exception.")
    public void createListWithNullContentThrowsException() {
        try {
            new PagedList<>(null, 0);
            Assertions.fail("as content is null");
        } catch (final NullPointerException e) {
        }
    }

    @Test
    @Description("Create list with payload and verify content.")
    public void createListWithContent() {
        final long knownTotal = 2;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        assertListSize(knownTotal, knownContentList);
    }

    private void assertListSize(final long knownTotal, final List<String> knownContentList) {
        final PagedList<String> pagedList = new PagedList<>(knownContentList, knownTotal);
        assertThat(pagedList.getTotal()).as("total size is wrong").isEqualTo(knownTotal);
        assertThat(pagedList.getSize()).as("list size is wrong").isEqualTo(knownContentList.size());
    }

    @Test
    @Description("Create list with payload and verify size values.")
    public void createListWithSmallerTotalThanContentSizeIsOk() {
        final long knownTotal = 0;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        assertListSize(knownTotal, knownContentList);
    }
}
