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

import org.eclipse.hawkbit.repository.RegexCharacterCollection.RegexChar;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.jupiter.api.Test;

@Feature("Unit Tests - Repository")
@Story("Regular expression helper")
public class RegexCharTest {

    private static final String TEST_STRING = getPrintableAsciiCharacters();

    private static final int INDEX_FIRST_PRINTABLE_ASCII_CHAR = 32;
    private static final int INDEX_LAST_PRINTABLE_ASCII_CHAR = 127;

    @Test
    @Description("Verifies every RegexChar can be used to exclusively find the desired characters in a String.")
    public void allRegexCharsOnlyFindExpectedChars() {
        for (final RegexChar character : RegexChar.values()) {
            switch (character) {
            case DIGITS:
                assertRegexCharExclusivelyFindsGivenCharacters(character, "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
                break;
            case WHITESPACE:
                assertRegexCharExclusivelyFindsGivenCharacters(character, " ", "\t");
                break;
            case SLASHES:
                assertRegexCharExclusivelyFindsGivenCharacters(character, "/", "\\");
                break;
            case QUOTATION_MARKS:
                assertRegexCharExclusivelyFindsGivenCharacters(character, "\"", "'");
                break;
            default:
                assertRegexCharExclusivelyFindsGivenCharacters(character, character.getRegExp());
                break;
            }
        }
    }

    @Test
    @Description("Verifies that combinations of RegexChars can be used to find the desired characters in a String.")
    public void combinedRegexCharsFindExpectedChars() {
        final RegexCharacterCollection greaterAndLessThan = new RegexCharacterCollection(RegexChar.GREATER_THAN,
                RegexChar.LESS_THAN);
        final RegexCharacterCollection equalsAndQuestionMark = new RegexCharacterCollection(RegexChar.EQUALS_SYMBOL,
                RegexChar.QUESTION_MARK);
        final RegexCharacterCollection colonAndWhitespace = new RegexCharacterCollection(RegexChar.COLON,
                RegexChar.WHITESPACE);

        assertRegexCharsExclusivelyFindsGivenCharacters(greaterAndLessThan, "<", ">");
        assertRegexCharsExclusivelyFindsGivenCharacters(equalsAndQuestionMark, "=", "?");
        assertRegexCharsExclusivelyFindsGivenCharacters(colonAndWhitespace, ":", " ", "\t");
    }

    private void assertRegexCharExclusivelyFindsGivenCharacters(final RegexChar characterToVerify,
            final String... charactersExpectedToBeFoundByRegex) {
        assertRegexCharsExclusivelyFindsGivenCharacters(new RegexCharacterCollection(characterToVerify),
                charactersExpectedToBeFoundByRegex);
    }

    private void assertRegexCharsExclusivelyFindsGivenCharacters(final RegexCharacterCollection regexToVerify,
            final String... charactersExpectedToBeFoundByRegex) {
        String notMatchingString = TEST_STRING;
        for(final String character : charactersExpectedToBeFoundByRegex) {
            notMatchingString = notMatchingString.replace(character, "");
        }
        for(final String character : charactersExpectedToBeFoundByRegex) {
            assertThat(RegexCharacterCollection.stringContainsCharacter("", regexToVerify)).isFalse();
            assertThat(RegexCharacterCollection.stringContainsCharacter(notMatchingString, regexToVerify)).isFalse();
            assertThat(RegexCharacterCollection.stringContainsCharacter(character, regexToVerify)).isTrue();
            assertThat(RegexCharacterCollection
                    .stringContainsCharacter(insertStringIntoString(notMatchingString, character, 0), regexToVerify))
                            .isTrue();
            assertThat(RegexCharacterCollection.stringContainsCharacter(
                    insertStringIntoString(notMatchingString, character, notMatchingString.length()), regexToVerify)).isTrue();
            assertThat(RegexCharacterCollection.stringContainsCharacter(
                    insertStringIntoString(notMatchingString, character, notMatchingString.length() / 2), regexToVerify)).isTrue();
            
        }
    }

    private static String getPrintableAsciiCharacters() {
        final StringBuilder stringBuilder = new StringBuilder();
        for (int i = INDEX_FIRST_PRINTABLE_ASCII_CHAR; i < INDEX_LAST_PRINTABLE_ASCII_CHAR; i++) {
            stringBuilder.append((char) i);
        }
        stringBuilder.append("\t");
        return stringBuilder.toString();
    }

    private static String insertStringIntoString(final String baseString, final String stringToInsert,
            final int position) {
        final StringBuilder stringBuilder = new StringBuilder(baseString);
        return stringBuilder.insert(position, stringToInsert).toString();
    }
}
