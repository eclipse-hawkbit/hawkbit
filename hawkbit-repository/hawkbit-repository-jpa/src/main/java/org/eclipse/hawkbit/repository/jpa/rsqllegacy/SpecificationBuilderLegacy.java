/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsqllegacy;

import static org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder.RsqlToSpecBuilder.LEGACY_G1;

import java.util.List;

import jakarta.persistence.criteria.Predicate;

import cz.jirutka.rsql.parser.RSQLParser;
import cz.jirutka.rsql.parser.RSQLParserException;
import cz.jirutka.rsql.parser.ast.Node;
import cz.jirutka.rsql.parser.ast.RSQLVisitor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.hawkbit.repository.RsqlQueryField;
import org.eclipse.hawkbit.repository.exception.RSQLParameterSyntaxException;
import org.eclipse.hawkbit.repository.rsql.RsqlConfigHolder;
import org.eclipse.hawkbit.repository.rsql.VirtualPropertyReplacer;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.util.CollectionUtils;

@Slf4j
public class SpecificationBuilderLegacy<A extends Enum<A> & RsqlQueryField, T> {

    private final Class<A> rsqlQueryFieldType;
    private final VirtualPropertyReplacer virtualPropertyReplacer;
    private final Database database;

    public SpecificationBuilderLegacy(
            final Class<A> rsqlQueryFieldType, final VirtualPropertyReplacer virtualPropertyReplacer, final Database database) {
        this.rsqlQueryFieldType = rsqlQueryFieldType;
        this.virtualPropertyReplacer = virtualPropertyReplacer;
        this.database = database;
    }

    public Specification<T> specification(final String rsql) {
        return (root, query, cb) -> {
            final Node rootNode = parseRsql(rsql);
            query.distinct(true);

            final RSQLVisitor<List<Predicate>, String> jpqQueryRSQLVisitor =
                    RsqlConfigHolder.getInstance().getRsqlToSpecBuilder() == LEGACY_G1
                            ? new JpaQueryRsqlVisitor<>(
                            root, cb, rsqlQueryFieldType,
                            virtualPropertyReplacer, database, query,
                            !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance().isIgnoreCase())
                            : new JpaQueryRsqlVisitorG2<>(
                                    rsqlQueryFieldType, root, query, cb,
                                    database, virtualPropertyReplacer,
                                    !RsqlConfigHolder.getInstance().isCaseInsensitiveDB() && RsqlConfigHolder.getInstance()
                                            .isIgnoreCase());
            final List<Predicate> accept = rootNode.accept(jpqQueryRSQLVisitor);

            if (CollectionUtils.isEmpty(accept)) {
                return cb.conjunction();
            } else {
                return cb.and(accept.toArray(new Predicate[0]));
            }
        };
    }

    private static Node parseRsql(final String rsql) {
        log.debug("Parsing rsql string {}", rsql);
        try {
            return new RSQLParser(AbstractRSQLVisitor.OPERATORS).parse(
                    RsqlConfigHolder.getInstance().isCaseInsensitiveDB() || RsqlConfigHolder.getInstance().isIgnoreCase()
                            ? rsql.toLowerCase()
                            : rsql);
        } catch (final IllegalArgumentException e) {
            throw new RSQLParameterSyntaxException("RSQL filter must not be null", e);
        } catch (final RSQLParserException e) {
            throw new RSQLParameterSyntaxException(e);
        }
    }
}
