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

import org.eclipse.hawkbit.repository.RsqlQueryField;

import cz.jirutka.rsql.parser.ast.AndNode;
import cz.jirutka.rsql.parser.ast.ComparisonNode;
import cz.jirutka.rsql.parser.ast.LogicalNode;
import cz.jirutka.rsql.parser.ast.OrNode;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;

/**
 * {@link RSQLVisitor} implementation which validates the nodes (fields) based
 * on a given {@link RsqlQueryField} for a given entity type.
 *
 * @param <A>
 *            The type the {@link RsqlQueryField} refers to.
 */
public class FieldValidationRsqlVisitor<A extends Enum<A> & RsqlQueryField> extends AbstractRSQLVisitor<A>
        implements RSQLVisitor<Void, String> {

    /**
     * Constructs the visitor and initializes it.
     * 
     * @param fieldNameProvider
     *            The {@link RsqlQueryField} to use for validation.
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
        // get AND validates
        getRsqlField(node);
        return null;
    }

    private Void visitNode(final LogicalNode node, final String param) {
        node.getChildren().forEach(child -> child.accept(this, param));
        return null;
    }

}
