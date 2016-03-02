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

public class PagedListTest {

    @Test(expected = NullPointerException.class)
    public void createListWithNullContentThrowsException() {
        new PagedList<>(null, 0);
    }

    @Test
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
    public void createListWithSmallerTotalThanContentSizeIsOk() {
        final long knownTotal = 0;
        final List<String> knownContentList = new ArrayList<>();
        knownContentList.add("content1");
        knownContentList.add("content2");

        assertListSize(knownTotal, knownContentList);

    }
}
