/**
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.repository.jpa.rsql;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.NONE;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.eclipse.hawkbit.repository.qfields.QueryField;
import org.eclipse.hawkbit.repository.qfields.SoftwareModuleFields;
import org.eclipse.hawkbit.repository.qfields.TargetFields;
import org.eclipse.hawkbit.repository.jpa.JpaRepositoryConfiguration;
import org.eclipse.hawkbit.repository.jpa.model.JpaSoftwareModule;
import org.eclipse.hawkbit.repository.jpa.model.JpaTarget;
import org.eclipse.hawkbit.repository.jpa.ql.utils.HawkbitQlToSql;
import org.eclipse.hawkbit.repository.test.TestConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = NONE, properties = {
        "hawkbit.rsql.caseInsensitiveDB=true",
        "spring.main.allow-bean-definition-overriding=true",
        "spring.main.banner-mode=off",
        "logging.level.root=ERROR" })
@ContextConfiguration(classes = { JpaRepositoryConfiguration.class, TestConfiguration.class })
@Disabled("For manual run only, while playing around with RSQL to SQL")
@SuppressWarnings("java:S2699") // java:S2699 - manual test, don't actually does assertions
class RsqlToSqlTest {

    private static final boolean FULL = Boolean.getBoolean("full");
    private HawkbitQlToSql rsqlToSQL;

    @Test
    void printPG() {
        printFrom(JpaSoftwareModule.class, SoftwareModuleFields.class, "metadata.x!=y");
        printFrom(JpaSoftwareModule.class, SoftwareModuleFields.class, "metadata.x==y");
        printFrom(JpaTarget.class, TargetFields.class, "metadata.x==y");
        printFrom(JpaTarget.class, TargetFields.class, "tag!=TAG1 and tag==TAG2");
        printFrom(JpaTarget.class, TargetFields.class, "tag==TAG1 and tag!=TAG2");
    }

    @Test
    void print() {
        print(JpaTarget.class, TargetFields.class, "tag==tag1 and tag==tag2");
        print(JpaTarget.class, TargetFields.class, "tag==tag1 or tag==tag2 or tag==tag3");
        print(JpaTarget.class, TargetFields.class, "targettype.key==type1 and metadata.key1==target1-value1");
        print(JpaTarget.class, TargetFields.class, "targettype.key==type1 or metadata.key1==target1-value1");
        print(JpaTarget.class, TargetFields.class, "(tag!=TAG1 or tag !=TAG2)");
    }

    @Test
    void printSimple() {
        // simple - column in table
        print(JpaTarget.class, TargetFields.class, "controllerId==ctrlr1");
        // reference - fk to a table
        print(JpaTarget.class, TargetFields.class, "assignedds.name==x and assignedds.version==y");
        // list (map table that refers main)
        print(JpaTarget.class, TargetFields.class, "metadata.key1==value1");
        // map (map table that refers main)
        print(JpaTarget.class, TargetFields.class, "attribute.key1==value1");
        // list of non-simple (with mapping table)
        print(JpaTarget.class, TargetFields.class, "tag==tag1");
    }

    @Test
    void printAnd() {
        print(JpaTarget.class, TargetFields.class, "controllerId==ctrlr1 and controllerId==ctrlr2");
        print(JpaTarget.class, TargetFields.class, "targettype.key==type1 and targettype.key==type2");
        print(JpaTarget.class, TargetFields.class, "tag==tag1 and tag==tag2 and tag==tag3");
        print(JpaTarget.class, TargetFields.class, "metadata.key1==value1 and metadata.key2==value2");
        print(JpaTarget.class, TargetFields.class, "attribute.key1==value1 and attribute.key2==value2");
    }

    @Test
    void printOr() {
        print(JpaTarget.class, TargetFields.class, "controllerId==ctrlr1 or controllerId==ctrlr2");
        print(JpaTarget.class, TargetFields.class, "targettype.key==type1 or targettype.key==type2");
        print(JpaTarget.class, TargetFields.class, "tag==tag1 or tag==tag2 or tag==tag3");
        print(JpaTarget.class, TargetFields.class, "metadata.key1==value1 or metadata.key2==value2");
        print(JpaTarget.class, TargetFields.class, "attribute.key1==value1 or attribute.key2==value2");
    }

    @Test
    void printSameTableMultiJoin() {
        print(JpaTarget.class, TargetFields.class, "installedds.version==1.0.0 or assignedds.version==2.0.0");
    }

    @Test
    void printEmpty() {
        print(JpaTarget.class, TargetFields.class, TargetFields.TAG.name() + "==''");
        print(JpaTarget.class, TargetFields.class, TargetFields.TAG.name() + "!=''");
    }

    @Test
    void printComplex() {
        print(JpaTarget.class, TargetFields.class, "attribute.key1==00 and (attribute.key2==02 or attribute.key2==01)");
        print(JpaTarget.class, TargetFields.class, "(attribute.key1==00 or attribute.key1==01) and (attribute.key2==02 or attribute.key2==01) and attribute.key3==01");
        print(JpaTarget.class, TargetFields.class, "(attribute.key1==00 or attribute.key1==01) and (attribute.key2==02 or attribute.key2==01) and attribute.key3==01 and updateStatus!=pending");
        print(JpaTarget.class, TargetFields.class, "((attribute.key1==00 or attribute.key1==01) and (attribute.key2==02 or attribute.key2==01) and attribute.key3==03 and updateStatus!=pending)");
        print(JpaTarget.class, TargetFields.class, "((attribute.key1==00 or attribute.key1==01) and (attribute.key2==02 or attribute.key2==01) and attribute.key3==01 and updateStatus!=pending)");
    }

    @Test
    void printVeryComplex() {
        print(JpaTarget.class, TargetFields.class, "(attribute.key1==00 or attribute.key1==01) and ((attribute.key2==02 or attribute.key2==01) and (attribute.key4==02 or attribute.key5==01)) and attribute.key3==01 and updateStatus!=pending");
        print(JpaTarget.class, TargetFields.class, "(attribute.key1==00 or attribute.key1==01) and ((attribute.key2==02 or attribute.key2==01) or (attribute.key4==02 or attribute.key5==01)) and attribute.key3==01 and updateStatus!=pending");
    }

    private static String from(final String sql) {
        return sql.substring(sql.indexOf("FROM"));
    }

    @PersistenceContext
    private void setEntityManager(final EntityManager entityManager) {
        rsqlToSQL = new HawkbitQlToSql(entityManager);
    }

    private <T, A extends Enum<A> & QueryField> void print(final Class<T> domainClass, final Class<A> fieldsClass, final String rsql) {
        System.out.println(rsql);
        System.out.println("\tSQL:\n\t\t" + useStar(rsqlToSQL.toSQL(domainClass, fieldsClass, rsql)));
    }

    private <T, A extends Enum<A> & QueryField> void printFrom(final Class<T> domainClass, final Class<A> fieldsClass, final String rsql) {
        System.out.println(rsql);
        System.out.println("\tSQL:\n\t\t" + from(rsqlToSQL.toSQL(domainClass, fieldsClass, rsql)));
    }

    private static String useStar(final String sql) {
        return FULL ? sql : sql.replaceAll("^SELECT [^(]* FROM", "SELECT * FROM");
    }
}