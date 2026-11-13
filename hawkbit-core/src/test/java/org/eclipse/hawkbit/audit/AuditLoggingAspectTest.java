/**
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Feature: Unit Tests - Audit Log<br/>
 * Story: Mask secrets in audit logged parameters
 */
@ExtendWith(OutputCaptureExtension.class)
class AuditLoggingAspectTest {

    private static final String SECRET_KEY = "secret.key";
    private static final String SECRET = "s3cr3tV4lu3";

    private Audited audited;

    @BeforeEach
    void setUp() {
        final AspectJProxyFactory factory = new AspectJProxyFactory(new Audited());
        factory.setProxyTargetClass(true);
        factory.addAspect(new AuditLoggingAspect());
        audited = factory.getProxy();
    }

    /**
     * Verifies that a parameter is masked when the condition over another parameter matches, and logged otherwise.
     */
    @Test
    void masksParamIfConditionMatches(final CapturedOutput output) {
        audited.keyValue(SECRET_KEY, SECRET, "other");
        assertThat(output).doesNotContain(SECRET).contains("[" + SECRET_KEY + ", ***, other]");

        audited.keyValue("plain.key", "plainValue", "other");
        assertThat(output).contains("[plain.key, plainValue, other]");
    }

    /**
     * Verifies that only the addressed (nested) map entries are masked and the real argument is not modified.
     */
    @Test
    void masksMapPath(final CapturedOutput output) {
        final Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("key2", SECRET);
        nested.put("plain", "nestedPlain");
        final Map<String, Object> map = new LinkedHashMap<>();
        map.put(SECRET_KEY, SECRET);
        map.put("key1", nested);
        map.put("other", "otherValue");

        audited.map(map);

        assertThat(output)
                .doesNotContain(SECRET)
                .contains("{" + SECRET_KEY + "=***, key1={key2=***, plain=nestedPlain}, other=otherValue}");
        assertThat(map).containsEntry(SECRET_KEY, SECRET);
        assertThat(nested).containsEntry("key2", SECRET);
    }

    /**
     * Verifies that missing map keys are ignored and a path going below a non-map value masks the whole value.
     */
    @Test
    void missingKeyIgnoredAndNonMapMaskedWhole(final CapturedOutput output) {
        final Map<String, Object> map = new HashMap<>();
        map.put("key1", "notAMap" + SECRET);
        audited.map(map);
        assertThat(output).doesNotContain(SECRET).contains("{key1=***}");
    }

    /**
     * Verifies that a misconfigured mask (unknown parameter) masks all parameters.
     */
    @Test
    void misconfiguredMaskMasksAll(final CapturedOutput output) {
        audited.misconfigured(SECRET, "other");
        assertThat(output).doesNotContain(SECRET).contains("[***, ***]");
    }

    /**
     * Verifies that a condition referencing an unknown (e.g. misspelled) parameter masks all parameters instead of evaluating to
     * false, while a known parameter with null value is evaluated normally.
     */
    @Test
    void unknownParamInConditionMasksAll(final CapturedOutput output) {
        audited.misspelledCondition(SECRET_KEY, SECRET);
        assertThat(output).doesNotContain(SECRET).contains("[***, ***]");

        audited.keyValue(null, "plainValue", "other");
        assertThat(output).contains("[null, plainValue, other]");
    }

    /**
     * Verifies that masked values are redacted from the exception message of a failed call.
     */
    @Test
    void redactsSecretFromErrorMessage(final CapturedOutput output) {
        assertThatThrownBy(() -> audited.failing(SECRET_KEY, SECRET))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(SECRET); // the real exception is not modified
        assertThat(output).doesNotContain(SECRET).contains("Invalid value ***");
    }

    /**
     * Verifies that the arguments are redacted from the message of a failing mask condition.
     */
    @Test
    void redactsSecretFromFailingConditionMessage(final CapturedOutput output) {
        audited.failingCondition(SECRET);
        assertThat(output).doesNotContain(SECRET).contains("[***]");
    }

    /**
     * Verifies that map path segments match non-String keys by their string form.
     */
    @Test
    void masksNonStringMapKey(final CapturedOutput output) {
        final Map<Object, Object> map = new LinkedHashMap<>();
        map.put(42, SECRET);
        map.put(43, "plainValue");
        audited.nonStringKeyMap(map);
        assertThat(output).doesNotContain(SECRET).contains("{42=***, 43=plainValue}");
    }

    /**
     * Verifies that masking a container which contains itself doesn't break the audit logging.
     */
    @Test
    void masksSelfContainingMap(final CapturedOutput output) {
        final Map<String, Object> map = new HashMap<>();
        map.put("self", map);
        map.put(SECRET_KEY, SECRET);
        audited.wholeMap(map);
        assertThat(output).doesNotContain(SECRET).contains("Parameters: [***]");
    }

    /**
     * Verifies that conditions can reference parameters by index (#a0 / #p0), while an index out of range masks all parameters.
     */
    @Test
    void conditionByIndexAlias(final CapturedOutput output) {
        audited.indexAliasA(SECRET_KEY, SECRET);
        audited.indexAliasP(SECRET_KEY, SECRET);
        assertThat(output).doesNotContain(SECRET).doesNotContain("Invalid @AuditLog masks").contains("[" + SECRET_KEY + ", ***]");

        audited.indexOutOfRange(SECRET_KEY, SECRET);
        assertThat(output).doesNotContain(SECRET).contains("[***, ***]");
    }

    static class Audited {

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#a0 == '" + SECRET_KEY + "'", mask = "value"))
        public void indexAliasA(final String key, final String value) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#p0 == '" + SECRET_KEY + "'", mask = "value"))
        public void indexAliasP(final String key, final String value) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#a2 == '" + SECRET_KEY + "'", mask = "value"))
        public void indexOutOfRange(final String key, final String value) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE, masks = @AuditLog.Mask(mask = "map"))
        public void wholeMap(final Map<String, Object> map) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#key == '" + SECRET_KEY + "'", mask = "value"))
        public void failing(final String key, final String value) {
            throw new IllegalArgumentException("Invalid value " + value);
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "T(java.lang.Integer).parseInt(#value) > 0", mask = "value"))
        public void failingCondition(final String value) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE, masks = @AuditLog.Mask(mask = "map/42"))
        public void nonStringKeyMap(final Map<Object, Object> map) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#key == '" + SECRET_KEY + "'", mask = "value"))
        public void keyValue(final String key, final String value, final String other) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(mask = { "map/" + SECRET_KEY, "map/key1/key2" }))
        public void map(final Map<String, Object> map) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE, masks = @AuditLog.Mask(mask = "unknown"))
        public void misconfigured(final String value, final String other) {
            // audited only
        }

        @AuditLog(entity = "Test", type = AuditLog.Type.UPDATE,
                masks = @AuditLog.Mask(when = "#keyNam == '" + SECRET_KEY + "'", mask = "value"))
        public void misspelledCondition(final String key, final String value) {
            // audited only
        }
    }
}
