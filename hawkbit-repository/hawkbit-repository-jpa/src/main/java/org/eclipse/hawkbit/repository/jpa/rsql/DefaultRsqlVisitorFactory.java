/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactory;

/**
 * Factory providing {@link RSQLVisitor} instances which validate the nodes based on a given {@link RsqlQueryField}.
 */
public class DefaultRsqlVisitorFactory implements RsqlVisitorFactory {

    @Override
    public <A extends Enum<A> & RsqlQueryField> RSQLVisitor<Void, String> validationRsqlVisitor(final Class<A> fieldNameProvider) {
        return new FieldValidationRsqlVisitor<>(fieldNameProvider);
    }
}