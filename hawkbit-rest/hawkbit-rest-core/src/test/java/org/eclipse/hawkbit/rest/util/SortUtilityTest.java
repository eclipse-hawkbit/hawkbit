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
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.rest.exception.SortParameterSyntaxErrorException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedDirectionException;
import org.eclipse.hawkbit.rest.exception.SortParameterUnsupportedFieldException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort.Order;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 *
 */
@Feature("Component Tests - Management API")
@Story("Sorting parameter")
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
        assertThat(parse).as("Count of parsing parameter").hasSize(1);
    }

    @Test
    @Description("Ascending sorting based on name and descending sorting based on description.")
    public void parseSortParam2() {
        final List<Order> parse = SortUtility.parse(TargetFields.class, SORT_PARAM_2);
        assertThat(parse).as("Count of parsing parameter").hasSize(2);
    }

    @Test
    @Description("Sorting with wrong syntax leads to SortParameterSyntaxErrorException.")
    public void parseWrongSyntaxParam() {
        assertThrows(SortParameterSyntaxErrorException.class,
                () -> SortUtility.parse(TargetFields.class, SYNTAX_FAILURE_SORT_PARAM));
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
        assertThrows(SortParameterUnsupportedDirectionException.class,
                () -> SortUtility.parse(TargetFields.class, WRONG_DIRECTION_PARAM));
    }

    @Test
    @Description("Sorting with unknown field leads to SortParameterUnsupportedFieldException.")
    public void parseWrongFieldParam() {
        assertThrows(SortParameterUnsupportedFieldException.class,
                () -> SortUtility.parse(TargetFields.class, WRONG_FIELD_PARAM));
    }
}
