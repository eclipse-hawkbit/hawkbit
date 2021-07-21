/**
 * Copyright (c) 2021 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import org.eclipse.hawkbit.repository.FieldNameProvider;
import org.eclipse.hawkbit.repository.rsql.RsqlVisitorFactory;

import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * Factory providing {@link RSQLVisitor} instances which validate the nodes
 * based on a given {@link FieldNameProvider}.
 */
public class DefaultRsqlVisitorFactory implements RsqlVisitorFactory {

    @Override
    public <A extends Enum<A> & FieldNameProvider> RSQLVisitor<Void, String> validationRsqlVisitor(
            final Class<A> fieldNameProvider) {
        return new FieldValidationRsqlVisitor<>(fieldNameProvider);
    }

}
