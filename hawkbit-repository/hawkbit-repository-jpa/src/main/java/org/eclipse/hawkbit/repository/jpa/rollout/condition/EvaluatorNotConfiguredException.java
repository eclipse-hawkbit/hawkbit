/**
 * Copyright (c) 2023 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rollout.condition;

/**
 * Exception indicating that a specific evaluator is missing in the application
 * context.
 */
public class EvaluatorNotConfiguredException extends RuntimeException {

    private static final String MESSAGE_FORMAT = "Cannot find any configured evaluator for action/condition '%s'. Please ensure to configure one in the application context to make use of it.";

    /**
     * Constructor
     * 
     * @param s
     *            the action/condition to evaluate
     */
    public EvaluatorNotConfiguredException(final String s) {
        super(String.format(MESSAGE_FORMAT, s));
    }
}
