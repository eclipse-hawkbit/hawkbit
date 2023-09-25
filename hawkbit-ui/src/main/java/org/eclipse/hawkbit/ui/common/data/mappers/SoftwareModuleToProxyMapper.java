/**
 * Copyright (c) 2020 Bosch.IO GmbH and others
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.hawkbit.ui.common.data.mappers;

import org.eclipse.hawkbit.repository.model.SoftwareModule;
import org.eclipse.hawkbit.repository.model.SoftwareModuleType;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxySoftwareModule;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyTypeInfo;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;

/**
 * Maps {@link SoftwareModule} entities, fetched from backend, to the
 * {@link ProxySoftwareModule} entities.
 */
public class SoftwareModuleToProxyMapper
        extends AbstractNamedEntityToProxyNamedEntityMapper<ProxySoftwareModule, SoftwareModule> {

    @Override
    public ProxySoftwareModule map(final SoftwareModule softwareModule) {
        final ProxySoftwareModule proxySoftwareModule = new ProxySoftwareModule();

        mapNamedEntityAttributes(softwareModule, proxySoftwareModule);

        proxySoftwareModule.setVersion(softwareModule.getVersion());
        proxySoftwareModule.setNameAndVersion(
                HawkbitCommonUtil.concatStrings(":", softwareModule.getName(), softwareModule.getVersion()));
        proxySoftwareModule.setVendor(softwareModule.getVendor());
        proxySoftwareModule.setEncrypted(softwareModule.isEncrypted());

        final SoftwareModuleType type = softwareModule.getType();
        final ProxyTypeInfo typeInfo = new ProxyTypeInfo(type.getId(), type.getName(), type.getKey());
        proxySoftwareModule.setTypeInfo(typeInfo);

        return proxySoftwareModule;
    }
}
