/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource.model;

import static org.fest.assertions.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features("Junit Tests - Management API")
@Stories("Paged List Handling")
public class PagedListTest {

    @Test(expected = NullPointerException.class)
    @Description("Ensures that a null payload entitiy throws an exception.")
    public void createListWithNullContentThrowsException() {
        new PagedList<>(null, 0);
    }

    @Test
    @Description("Create list with payload and verify content.")
    public void createListWithContent() {
        final long knownTotal = 2;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        final PagedList<String> pagedList = new PagedList<>(knownContentList, knownTotal);

        assertThat(pagedList.getTotal()).isEqualTo(knownTotal);
        assertThat(pagedList.getSize()).isEqualTo(knownContentList.size());
    }

    @Test
    @Description("Create list with payload and verify size values.")
    public void createListWithSmallerTotalThanContentSizeIsOk() {
        final long knownTotal = 0;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        final PagedList<String> pagedList = new PagedList<>(knownContentList, knownTotal);
        assertThat(pagedList.getTotal()).isEqualTo(knownTotal);
        assertThat(pagedList.getSize()).isEqualTo(knownContentList.size());

    }
}
