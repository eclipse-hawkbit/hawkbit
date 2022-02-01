/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.springframework.util.ReflectionUtils;

import cz.jirutka.rsql.parser.ParseException;

/**
 * A {@link ParseException} wrapper which allows to access the parsing
 * information from the exception using reflection due there is no other access
 * of this information. See issue for requesting feature
 * <a href="https://github.com/jirutka/rsql-parser/issues/22">https://github.com
 * /jirutka/rsql-parser/issues/22</a>
 */
public class ParseExceptionWrapper {

    private final ParseException parseException;

    /**
     * Constructor.
     * 
     * @param parseException
     *            the original parsing exception object to access its field
     *            using reflection
     */
    public ParseExceptionWrapper(final ParseException parseException) {
        this.parseException = parseException;
    }

    public int[][] getExpectedTokenSequence() {
        return (parseException.expectedTokenSequences != null) // unclear if this can happen
                ? parseException.expectedTokenSequences
                : new int[0][0];
    }

    /**
     * Get the current token
     *
     * @return the current token or {@code null} if there is non.
     */
    public TokenWrapper getCurrentToken() {
        return (parseException.currentToken != null) // unclear if this can happen
                ? new TokenWrapper(parseException.currentToken)
                : null;

    }

    @Override
    public String toString() {
        return "ParseExceptionWrapper [getExpectedTokenSequence()=" + Arrays.toString(getExpectedTokenSequence())
                + ", getCurrentToken()=" + getCurrentToken() + "]";
    }


    /**
     * A {@link TokenWrapper} which wraps the
     * {@code cz.jirutka.rsql.parser.Token} class of the {@link ParseException}
     * which otherwise is not accessible.
     */
    public static final class TokenWrapper {

        private static final String FIELD_NEXT = "next";
        private static final String FIELD_KIND = "kind";
        private static final String FIELD_IMAGE = "image";
        private static final String FIELD_BEGIN_COL = "beginColumn";
        private static final String FIELD_END_COL = "endColumn";

        private final Object tokenInstance;

        private Field nextTokenField;
        private Field kindTokenField;
        private Field imageTokenField;
        private Field beginColumnTokenField;
        private Field endColumnTokenField;

        private TokenWrapper(final Object tokenField) {
            this.tokenInstance = tokenField;

            try {
                nextTokenField = getAccessibleField(FIELD_NEXT);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                nextTokenField = null;
            }
            try {
                kindTokenField = getAccessibleField(FIELD_KIND);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                kindTokenField = null;
            }

            try {
                imageTokenField = getAccessibleField(FIELD_IMAGE);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                imageTokenField = null;
            }

            try {
                beginColumnTokenField = getAccessibleField(FIELD_BEGIN_COL);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                beginColumnTokenField = null;
            }

            try {
                endColumnTokenField = getAccessibleField(FIELD_END_COL);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                endColumnTokenField = null;
            }
        }

        public TokenWrapper getNext() {
            final Object nextToken = getValue(nextTokenField);
            return nextToken != null ? new TokenWrapper(nextToken) : null;

        }

        public int getKind() {
            if (kindTokenField == null) {
                return 0;
            }
            return (int) getValue(kindTokenField);
        }

        public String getImage() {
            if (imageTokenField == null) {
                return null;
            }
            return (String) getValue(imageTokenField);
        }

        public int getBeginColumn() {
            if (beginColumnTokenField == null) {
                return 0;
            }
            return (int) getValue(beginColumnTokenField);
        }

        public int getEndColumn() {
            if (endColumnTokenField == null) {
                return 0;
            }
            return (int) getValue(endColumnTokenField);
        }

        private Field getAccessibleField(final String field) throws NoSuchFieldException {
            final Field declaredField = tokenInstance.getClass().getDeclaredField(field);
            ReflectionUtils.makeAccessible(declaredField);
            return declaredField;
        }

        private Object getValue(final Field field) {
            try {
                return field.get(tokenInstance);
            } catch (IllegalArgumentException | IllegalAccessException e) {
                throw new IllegalFieldAccessExeption(e);
            }
        }

        @Override
        public String toString() {
            return "TokenWrapper [tokenInstance=" + tokenInstance + ", getNext()=" + getNext() + ", getKind()="
                    + getKind() + ", getImage()=" + getImage() + ", getBeginColumn()=" + getBeginColumn()
                    + ", getEndColumn()=" + getEndColumn() + "]";
        }
    }

    static class IllegalFieldAccessExeption extends RuntimeException {
        public IllegalFieldAccessExeption(Throwable e) {
            super(e);
        }
    }
}

