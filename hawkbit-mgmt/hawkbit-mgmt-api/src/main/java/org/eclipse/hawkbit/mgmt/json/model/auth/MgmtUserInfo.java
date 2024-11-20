/**
 * Copyright (c) 2021 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.mgmt.json.model.auth;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A json annotated rest model for Userinfo to RESTful API representation.
 */
@Data
@Accessors(chain = true)
public class MgmtUserInfo {

    private String tenant;
    private String username;
}