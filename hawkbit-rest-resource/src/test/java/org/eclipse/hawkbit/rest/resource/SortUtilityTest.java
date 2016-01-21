/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.resource;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.eclipse.hawkbit.repository.TargetFields;
import org.junit.Test;
import org.springframework.data.domain.Sort.Order;

/**
 *
 */
public class SortUtilityTest {
    private static final String SORT_PARAM_1 = "NAME:ASC";
    private static final String SORT_PARAM_2 = "NAME:ASC, DESCRIPTION:DESC";
    private static final String SYNTAX_FAILURE_SORT_PARAM = "NAME:ASC DESCRIPTION:DESC";
    private static final String WRONG_DIRECTION_PARAM = "NAME:ASDF";
    private static final String CASE_INSENSITIVE_DIRECTION_PARAM = "NaME:ASC";
    private static final String CASE_INSENSITIVE_DIRECTION_PARAM_1 = "name:ASC";
    private static final String WRONG_FIELD_PARAM = "ASDF:ASC";

    @Test
    public void parseSortParam1() {

        final List<Order> parse = SortUtility.parse(TargetFields.class, SORT_PARAM_1);
        assertEquals(1, parse.size());
    }

    @Test
    public void parseSortParam2() {
        final List<Order> parse = SortUtility.parse(TargetFields.class, SORT_PARAM_2);
        assertEquals(2, parse.size());
    }

    @Test(expected = SortParameterSyntaxErrorException.class)
    public void parseWrongSyntaxParam() {
        SortUtility.parse(TargetFields.class, SYNTAX_FAILURE_SORT_PARAM);
    }

    @Test
    public void parsingIsNotCaseSensitive() {
        SortUtility.parse(TargetFields.class, CASE_INSENSITIVE_DIRECTION_PARAM);
        SortUtility.parse(TargetFields.class, CASE_INSENSITIVE_DIRECTION_PARAM_1);
    }

    @Test(expected = SortParameterUnsupportedDirectionException.class)
    public void parseWrongDirectionParam() {
        SortUtility.parse(TargetFields.class, WRONG_DIRECTION_PARAM);
    }

    @Test(expected = SortParameterUnsupportedFieldException.class)
    public void parseWrongFieldParam() {
        SortUtility.parse(TargetFields.class, WRONG_FIELD_PARAM);
    }
}
