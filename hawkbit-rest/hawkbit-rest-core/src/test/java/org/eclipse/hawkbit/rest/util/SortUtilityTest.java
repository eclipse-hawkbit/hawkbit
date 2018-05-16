/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.rest.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.util.List;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.rest.exception.SortParameterSyntaxErrorException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedDirectionException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedFieldException;
import org.junit.Test;
import org.springframework.data.domain.Sort.Order;

import ru.yandex.qatools.allure.annotations.Description;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

/**
 *
 */
@Features("Component Tests - Management API")
@Stories("Sorting parameter")
public class SortUtilityTest {
    private static final String SORT_PARAM_1 = "NAME:ASC";
    private static final String SORT_PARAM_2 = "NAME:ASC, DESCRIPTION:DESC";
    private static final String SYNTAX_FAILURE_SORT_PARAM = "NAME:ASC DESCRIPTION:DESC";
    private static final String WRONG_DIRECTION_PARAM = "NAME:ASDF";
    private static final String CASE_INSENSITIVE_DIRECTION_PARAM = "NaME:ASC";
    private static final String CASE_INSENSITIVE_DIRECTION_PARAM_1 = "name:ASC";
    private static final String WRONG_FIELD_PARAM = "ASDF:ASC";

    @Test
    @Description("Ascending sorting based on name.")
    public void parseSortParam1() {
        final List<Order> parse = SortUtility.parse(TargetFields.class, SORT_PARAM_1);
        assertThat(1).as("Count of parsing parameter").isEqualTo(parse.size());
    }

    @Test
    @Description("Ascending sorting based on name and descending sorting based on description.")
    public void parseSortParam2() {
        final List<Order> parse = SortUtility.parse(TargetFields.class, SORT_PARAM_2);
        assertThat(2).as("Count of parsing parameter").isEqualTo(parse.size());
    }

    @Test
    @Description("Sorting with wrong syntax leads to SortParameterSyntaxErrorException.")
    public void parseWrongSyntaxParam() {
        try {
            SortUtility.parse(TargetFields.class, SYNTAX_FAILURE_SORT_PARAM);
            fail("SortParameterSyntaxErrorException expected because of wrong syntax");
        } catch (final SortParameterSyntaxErrorException e) {
        }
    }

    @Test
    @Description("Sorting based on name with case sensitive is possible.")
    public void parsingIsNotCaseSensitive() {
        SortUtility.parse(TargetFields.class, CASE_INSENSITIVE_DIRECTION_PARAM);
        SortUtility.parse(TargetFields.class, CASE_INSENSITIVE_DIRECTION_PARAM_1);
    }

    @Test
    @Description("Sorting with unknown direction order leads to SortParameterUnsupportedDirectionException.")
    public void parseWrongDirectionParam() {
        try {
            SortUtility.parse(TargetFields.class, WRONG_DIRECTION_PARAM);
            fail("SortParameterUnsupportedDirectionException expected because of unknown direction order");
        } catch (final SortParameterUnsupportedDirectionException e) {
        }

    }

    @Test
    @Description("Sorting with unknown field leads to SortParameterUnsupportedFieldException.")
    public void parseWrongFieldParam() {
        try {
            SortUtility.parse(TargetFields.class, WRONG_FIELD_PARAM);
            fail("SortParameterUnsupportedFieldException expected because of unknown field");
        } catch (final SortParameterUnsupportedFieldException e) {
        }

    }
}
