/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.jpa.AbstractJpaIntegrationTest;
import org.eclipse.hawkbit.repository.rsql.RsqlValidationOracle;
import org.eclipse.hawkbit.repository.rsql.SuggestToken;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Feature("Component Tests - Repository")
@Story("RSQL filter suggestion")
public class RSQLParserValidationOracleTest extends AbstractJpaIntegrationTest {

    private static final String[] OP_SUGGESTIONS = new String[] { "==", "!=", "=ge=", "=le=", "=gt=", "=lt=", "=in=",
            "=out=" };
    private static final String[] FIELD_SUGGESTIONS = Arrays.stream(TargetFields.values())
            .map(field -> field.name().toLowerCase()).toArray(String[]::new);
    private static final String[] AND_OR_SUGGESTIONS = new String[] { "and", "or" };
    private static final String[] NAME_VERSION_SUGGESTIONS = new String[] { "name", "version" };
    @Autowired
    private RsqlValidationOracle rsqlValidationOracle;

    @Test
    @Description("Verifies that suggestions contains all possible field names")
    public void suggestionContainsAllFieldNames() {
        final String rsqlQuery = "na";
        final List<String> currentSuggestions = getSuggestions(rsqlQuery);
        assertThat(currentSuggestions).containsOnly(FIELD_SUGGESTIONS);
    }

    @Test
    @Description("Verifies that suggestions only contains the allowed operators")
    public void suggestionContainsOnlyOperators() {
        final String rsqlQuery = "name";
        final List<String> currentSuggestions = getSuggestions(rsqlQuery);
        assertThat(currentSuggestions).containsOnly(OP_SUGGESTIONS);
    }

    @Test
    @Description("Verifies that suggestions only contains operator to combine RSQL filters (and, or)")
    public void suggestionContainsOnlyAndOrOperator() {
        final String rsqlQuery = "name==a ";
        final List<String> currentSuggestions = getSuggestions(rsqlQuery);
        assertThat(currentSuggestions).containsOnly(AND_OR_SUGGESTIONS);
    }

    @Test
    @Description("Verifies that sub suggestions are shown")
    public void suggestionContainsSubFieldSuggestions() {
        final String rsqlQuery = "assignedds.";
        final List<String> currentSuggestions = getSuggestions(rsqlQuery);
        assertThat(currentSuggestions).containsOnly(NAME_VERSION_SUGGESTIONS);
    }

    private List<String> getSuggestions(final String rsqlQuery) {
        return rsqlValidationOracle
                .suggest(rsqlQuery, -1).getSuggestionContext().getSuggestions().stream()
                .map(SuggestToken::getSuggestion)
                .collect(Collectors.toList());
    }
}