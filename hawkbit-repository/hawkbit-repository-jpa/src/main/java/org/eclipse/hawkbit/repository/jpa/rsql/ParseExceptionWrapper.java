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

import com.google.common.base.Throwables;

import cz.jirutka.rsql.parser.ParseException;

/**
 * A {@link ParseException} wrapper which allows to access the parsing
 * information from the exception using reflection due there is no other access
 * of this information. See issue for requesting feature
 * <a href="https://github.com/jirutka/rsql-parser/issues/22">https://github.com
 * /jirutka/rsql-parser/issues/22</a>
 */
public class ParseExceptionWrapper {

    private static final String FIELD_EXPECTED_TOKEN_SEQ = "expectedTokenSequences";
    private static final String FIELD_CURRENT_TOKEN = "currentToken";

    private final ParseException parseException;
    private final Class<? extends ParseException> parseExceptionClass;
    private Field expectedTokenSequenceField;
    private Field currentTokenField;

    /**
     * Constructor.
     * 
     * @param parseException
     *            the original parsing exception object to access its field
     *            using reflection
     */
    public ParseExceptionWrapper(final ParseException parseException) {
        this.parseException = parseException;
        parseExceptionClass = parseException.getClass();

        try {
            expectedTokenSequenceField = getAccessibleField(parseExceptionClass, FIELD_EXPECTED_TOKEN_SEQ);
        } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
            expectedTokenSequenceField = null;
        }

        try {
            currentTokenField = getAccessibleField(parseExceptionClass, FIELD_CURRENT_TOKEN);
        } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
            currentTokenField = null;
        }
    }

    public int[][] getExpectedTokenSequence() {
        if (expectedTokenSequenceField == null) {
            return new int[0][0];
        }
        return (int[][]) getValue(expectedTokenSequenceField, parseException);
    }

    public TokenWrapper getCurrentToken() {
        if (currentTokenField == null) {
            return null;
        }
        return new TokenWrapper(getValue(currentTokenField, parseException));
    }

    @Override
    public String toString() {
        return "ParseExceptionWrapper [getExpectedTokenSequence()=" + Arrays.toString(getExpectedTokenSequence())
                + ", getCurrentToken()=" + getCurrentToken() + "]";
    }

    private static Field getAccessibleField(final Class<?> clazz, final String field) throws NoSuchFieldException {
        final Field declaredField = clazz.getDeclaredField(field);
        declaredField.setAccessible(true);
        return declaredField;
    }

    private static Object getValue(final Field field, final Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw Throwables.propagate(e);
        }
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
                nextTokenField = getAccessibleField(tokenField.getClass(), FIELD_NEXT);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                nextTokenField = null;
            }
            try {
                kindTokenField = getAccessibleField(tokenField.getClass(), FIELD_KIND);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                kindTokenField = null;
            }

            try {
                imageTokenField = getAccessibleField(tokenField.getClass(), FIELD_IMAGE);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                imageTokenField = null;
            }

            try {
                beginColumnTokenField = getAccessibleField(tokenField.getClass(), FIELD_BEGIN_COL);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                beginColumnTokenField = null;
            }

            try {
                endColumnTokenField = getAccessibleField(tokenField.getClass(), FIELD_END_COL);
            } catch (@SuppressWarnings("squid:S1166") final NoSuchFieldException e) {
                endColumnTokenField = null;
            }
        }

        public TokenWrapper getNext() {
            final Object nextToken = getValue(nextTokenField, tokenInstance);
            return nextToken != null ? new TokenWrapper(nextToken) : null;

        }

        public int getKind() {
            if (kindTokenField == null) {
                return 0;
            }
            return (int) getValue(kindTokenField, tokenInstance);
        }

        public String getImage() {
            if (imageTokenField == null) {
                return null;
            }
            return (String) getValue(imageTokenField, tokenInstance);
        }

        public int getBeginColumn() {
            if (beginColumnTokenField == null) {
                return 0;
            }
            return (int) getValue(beginColumnTokenField, tokenInstance);
        }

        public int getEndColumn() {
            if (endColumnTokenField == null) {
                return 0;
            }
            return (int) getValue(endColumnTokenField, tokenInstance);
        }

        @Override
        public String toString() {
            return "TokenWrapper [tokenInstance=" + tokenInstance + ", getNext()=" + getNext() + ", getKind()="
                    + getKind() + ", getImage()=" + getImage() + ", getBeginColumn()=" + getBeginColumn()
                    + ", getEndColumn()=" + getEndColumn() + "]";
        }
    }
}
