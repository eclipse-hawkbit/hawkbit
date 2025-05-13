/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.rsql;

import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import org.eclipse.hawkbit.repository.RsqlQueryField;

/**
 * Factory to obtain {@link RSQLVisitor} instances that can be used to process the {@link Node}s representing an RSQL query.
 */
@FunctionalInterface
public interface RsqlVisitorFactory {

    /**
     * Provides a {@link RSQLVisitor} instance for validating RSQL queries based on the given {@link RsqlQueryField}.
     *
     * @param <A> The type of the {@link RsqlQueryField}.
     * @param fieldNameProvider providing accessing to the relevant field names.
     * @return An {@link RSQLVisitor} to validate the {@link Node}s of an RSQL query.
     */
    <A extends Enum<A> & RsqlQueryField> RSQLVisitor<Void, String> validationRsqlVisitor(Class<A> fieldNameProvider);
}