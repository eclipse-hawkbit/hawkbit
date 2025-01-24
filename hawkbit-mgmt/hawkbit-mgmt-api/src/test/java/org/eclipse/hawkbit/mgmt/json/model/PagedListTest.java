/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Management API")
@Story("Paged List Handling")
class PagedListTest {

    @Test
    @Description("Ensures that a null payload entity throws an exception.")
    void createListWithNullContentThrowsException() {
        assertThatThrownBy(() -> new PagedList<>(null, 0))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @Description("Create list with payload and verify content.")
    void createListWithContent() {
        final long knownTotal = 2;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        assertListSize(knownTotal, knownContentList);
    }

    @Test
    @Description("Create list with payload and verify size values.")
    void createListWithSmallerTotalThanContentSizeIsOk() {
        final long knownTotal = 0;
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
}