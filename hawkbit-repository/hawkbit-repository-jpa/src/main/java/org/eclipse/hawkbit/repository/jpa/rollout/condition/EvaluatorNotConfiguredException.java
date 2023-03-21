/**
 * Copyright (c) 2023 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
