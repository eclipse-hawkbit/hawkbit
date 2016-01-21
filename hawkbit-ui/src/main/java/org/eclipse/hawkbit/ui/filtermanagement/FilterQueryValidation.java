/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.filtermanagement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.hawkbit.repository.TargetFields;
import org.eclipse.hawkbit.repository.TargetManagement;
import org.eclipse.hawkbit.repository.rsql.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.rsql.RSQLParameterUnsupportedFieldException;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import cz.jirutka.rsql.parser.ParseException;
import cz.jirutka.rsql.parser.RSQLParserException;

/**
 * 
 * Validates the target filter query.
 * 
 *
 *
 */

public final class FilterQueryValidation {

    private static final Logger LOGGER = LoggerFactory.getLogger(FilterQueryValidation.class);

    private FilterQueryValidation() {

    }

    /**
     * method for get ExpectedTokens.
     * 
     * @param input
     * @param entityManager
     * @return
     */
    public static ValidationResult getExpectedTokens(final String input) {

        final TokenDescription tokenDesc = new TokenDescription();
        final ValidationResult result = new ValidationResult();
        final List<String> expectedTokens = new ArrayList<>();
        try {

            final TargetManagement management = SpringContextHelper.getBean(TargetManagement.class);
            management.findTargetsAll(input, new PageRequest(0, 100));
        } catch (final RSQLParameterSyntaxException ex) {
            setExceptionDetails(new Exception(ex.getCause().getCause()), expectedTokens, result, tokenDesc);
            result.setMessage(getCustomMessage(ex.getCause().getMessage(), result.getExpectedTokens()));
            result.setIsValidationFailed(Boolean.TRUE);
            LOGGER.info("Syntax exception on parsing :", ex);
        } catch (final RSQLParserException ex) {
            setExceptionDetails(ex, expectedTokens, result, tokenDesc);
            result.setMessage(getCustomMessage(ex.getMessage(), result.getExpectedTokens()));
            result.setIsValidationFailed(Boolean.TRUE);
            LOGGER.info("Exception on parsing :", ex);
        } catch (final IllegalArgumentException ex) {
            result.setMessage(getCustomMessage(ex.getMessage(), null));
            result.setIsValidationFailed(Boolean.TRUE);
            LOGGER.info("Illegal argument on parsing :", ex);
        } catch (final RSQLParameterUnsupportedFieldException ex) {
            result.setMessage(getCustomMessage(ex.getMessage(), null));
            result.setIsValidationFailed(Boolean.TRUE);
            LOGGER.info("Unsupported field on parsing :", ex);
        }
        return result;

    }

    private static void setExceptionDetails(final Exception ex, final List<String> expectedTokens,
            final ValidationResult result, final TokenDescription tokenDesc) {
        for (final Integer node : getNextTokens(ex)) {
            if (node != 12) {
                expectedTokens.add(tokenDesc.getTokenImage()[node]);
            }
        }
        final List<String> customExpectTokenList = processExpectedTokens(getNextTokens(ex));
        if (!customExpectTokenList.isEmpty()) {
            result.setExpectedTokens(customExpectTokenList);
        } else {
            result.setExpectedTokens(expectedTokens);
        }
    }

    /**
     * method for process ExpectedTokens.
     * 
     * @param expectedTokens
     * @return
     */
    public static List<String> processExpectedTokens(final List<Integer> expectedTokens) {
        final List<String> expectToken = new ArrayList<>();
        if (expectedTokens.size() == 2 && expectedTokens.contains(9) && expectedTokens.contains(4)) {
            final List<String> expectedFieldList = Arrays.stream(TargetFields.values())
                    .map(v -> v.name().toLowerCase()).collect(Collectors.toList());
            expectToken.addAll(expectedFieldList);
            expectToken.add("assignedds.name");
            expectToken.add("assignedds.version");
        }
        return expectToken;
    }

    /**
     * Method To Get Next Token.
     * 
     * @param e
     *            .
     * @return list.
     */
    public static List<Integer> getNextTokens(final Exception e) {
        Throwable parseException = e.getCause();
        final List<Integer> listTokens = new ArrayList<>();
        if (parseException != null) {
            do {
                if (parseException instanceof ParseException) {
                    try {
                        Field declaredField;
                        declaredField = parseException.getClass().getDeclaredField("expectedTokenSequences");
                        int[][] tokens;
                        tokens = (int[][]) declaredField.get(parseException);
                        for (final int[] is : tokens) {
                            for (final int i : is) {
                                listTokens.add(i);
                            }
                        }
                        return listTokens;
                    } catch (SecurityException | NoSuchFieldException | IllegalArgumentException
                            | IllegalAccessException ex) {
                        LOGGER.info("Exception on parsing :", ex);
                    }

                } else {
                    return listTokens;
                }
            } while ((parseException = parseException.getCause()) != null);
        }
        return Collections.emptyList();
    }

    /**
     * To Get Custom Message.
     * 
     * @param message
     * @param expectedTokens
     * @return String.
     */
    public static String getCustomMessage(final String message, final List<String> expectedTokens) {
        String builder = message;
        if (message.contains(":")) {
            builder = message.substring(message.indexOf(':') + 1, message.length());
            if (builder.indexOf("Was expecting") != -1) {
                builder = builder.substring(0, builder.lastIndexOf("Was expecting"));
            }
            if (null != expectedTokens && !expectedTokens.isEmpty()) {
                final StringBuilder tokens = new StringBuilder();
                expectedTokens.stream().forEach(value -> tokens.append(value + ","));
                builder = builder.concat("Was expecting :" + tokens.toString().substring(0, tokens.length() - 1));
            }
            builder = builder.replace('\r', ' ');
            builder = builder.replace('\n', ' ');
            builder = builder.replaceAll(">", " ");
            builder = builder.replaceAll("<", " ");
        }
        return builder;
    }

}
