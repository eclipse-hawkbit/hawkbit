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

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * {@link RSQLVisitor} implementation which validates the nodes (fields) based
 * on a given {@link FieldNameProvider} for a given entity type.
 *
 * @param <A>
 *            The type the {@link FieldNameProvider} refers to.
 */
public class FieldValidationRsqlVisitor<A extends Enum<A> & FieldNameProvider> extends AbstractFieldNameRSQLVisitor<A>
        implements RSQLVisitor<Void, String> {

    /**
     * Constructs the visitor and initializes it.
     * 
     * @param fieldNameProvider
     *            The {@link FieldNameProvider} to use for validation.
     */
    public FieldValidationRsqlVisitor(final Class<A> fieldNameProvider) {
        super(fieldNameProvider);
    }

    @Override
    public Void visit(final AndNode node, final String param) {
        return visitNode(node, param);
    }

    @Override
    public Void visit(final OrNode node, final String param) {
        return visitNode(node, param);
    }

    @Override
    public Void visit(final ComparisonNode node, final String param) {
        final A fieldName = getFieldEnumByName(node);
        getAndValidatePropertyFieldName(fieldName, node);
        return null;
    }

    private Void visitNode(final LogicalNode node, final String param) {
        node.getChildren().forEach(child -> child.accept(this, param));
        return null;
    }

}
