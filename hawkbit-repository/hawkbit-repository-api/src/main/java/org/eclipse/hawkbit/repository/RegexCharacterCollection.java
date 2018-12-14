/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Collection of regular expression characters to check strings
 */
public class RegexCharacterCollection {

    private final EnumSet<RegexChar> characters;
    private final Pattern findAnyCharacter;

    public RegexCharacterCollection(final RegexChar... characters) {
        this.characters = EnumSet.copyOf(Arrays.asList(characters));
        this.findAnyCharacter = getPatternFindAnyCharacter();
    }

    public static boolean stringContainsCharacter(final String stringToCheck,
            final RegexCharacterCollection regexCharacterCollection) {
        return regexCharacterCollection.findAnyCharacter.matcher(stringToCheck).matches();
    }

    private Pattern getPatternFindAnyCharacter() {
        final String regexCharacters = characters.stream().map(RegexChar::getRegExp)
                .collect(Collectors.joining());
        final String regularExpression = String.format(".*[%s]+.*", regexCharacters);
        return Pattern.compile(regularExpression);
    }

    public enum RegexChar {
        WHITESPACE("\\s", "character.whitespace"), DIGITS("0-9", "character.digits"), QUOTATION_MARKS("'\"",
                "character.quotationMarks"), SLASHES("\\/\\\\", "character.slashes"), GREATER_THAN(
                        ">"), LESS_THAN("<"), EQUALS_SYMBOL("="), EXCLAMATION_MARK("!"), QUESTION_MARK("?"), COLON(":");

        private final String regExp;
        private final String l18nReferenceDescription;

        RegexChar(final String character) {
            this(character, null);
        }
    
        RegexChar(final String regExp, final String l18nReferenceDescription) {
            this.regExp = regExp;
            this.l18nReferenceDescription = l18nReferenceDescription;
        }

        public String getRegExp() {
            return regExp;
        }

        public Optional<String> getL18nReferenceDescription() {
            return Optional.ofNullable(l18nReferenceDescription);
        }
    }
}
