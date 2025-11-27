/**
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.audit;

import java.util.Optional;

import lombok.NoArgsConstructor;
import org.eclipse.hawkbit.context.AccessContext;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
@SuppressWarnings("java:S6548") // java:S6548 - singleton holder ensures static access to spring resources in some places
public class AuditContextProvider {

    public static AuditContext getAuditContext() {
        return new AuditContext(
                Optional.ofNullable(AccessContext.tenant()).orElse("n/a"),
                Optional.ofNullable(AccessContext.actor()).orElse(AccessContext.SYSTEM_ACTOR));
    }

    public record AuditContext(String tenant, String username) {}
}