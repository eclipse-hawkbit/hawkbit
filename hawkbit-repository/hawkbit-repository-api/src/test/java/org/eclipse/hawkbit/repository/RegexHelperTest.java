/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature("Unit Tests - Repository")
@Story("Regular expression helper")
public class RegexHelperTest {

    private final String testString = getPrintableAsciiCharacters();

    private static String getPrintableAsciiCharacters() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = 32; i < 127; i++) {
            stringBuilder.append((char) i);
        }
        stringBuilder.append("\t");
        return stringBuilder.toString();
    }

    private static String insertStringIntoString(final String baseString, final String stringToInsert,
            final int position) {
        return baseString.substring(0, position) + stringToInsert + baseString.substring(position, baseString.length());
    }

    @Test
    @Description("Verifies that given Regex characters in a String are recognized.")
    public void stringContainsCharacters() {
        final Set<RegexChar> greaterAndLessThan = RegexChar.getImmutableCharSet(RegexChar.GREATER_THAN,
                RegexChar.LESS_THAN);
        final Set<RegexChar> whitespace = RegexChar.getImmutableCharSet(RegexChar.WHITESPACE);
        final Set<RegexChar> slashes = RegexChar.getImmutableCharSet(RegexChar.SLASHES);

        assertRegexOnlyMatchesGivenCharacters(greaterAndLessThan, "<", ">");
        assertRegexOnlyMatchesGivenCharacters(whitespace, " ", "\t");
        assertRegexOnlyMatchesGivenCharacters(slashes, "/", "\\");

    }

    private void assertRegexOnlyMatchesGivenCharacters(final Set<RegexChar> regexToVerify,
            final String... charactersExpectedToBeFoundByRegex) {
        String notMatchingString = testString;
        for(final String character : charactersExpectedToBeFoundByRegex) {
            notMatchingString = notMatchingString.replace(character, "");
        }
        for(final String character : charactersExpectedToBeFoundByRegex) {
            assertThat(RegexHelper.stringContainsCharacters("", regexToVerify)).isFalse();
            assertThat(RegexHelper.stringContainsCharacters(notMatchingString, regexToVerify)).isFalse();
            assertThat(RegexHelper.stringContainsCharacters(character, regexToVerify)).isTrue();
            assertThat(RegexHelper.stringContainsCharacters(insertStringIntoString(notMatchingString, character, 0),
                    regexToVerify)).isTrue();
            assertThat(RegexHelper.stringContainsCharacters(
                    insertStringIntoString(notMatchingString, character, notMatchingString.length()), regexToVerify))
                            .isTrue();
            assertThat(RegexHelper.stringContainsCharacters(
                    insertStringIntoString(notMatchingString, character, notMatchingString.length() / 2),
                    regexToVerify)).isTrue();
            
        }
    }
}
