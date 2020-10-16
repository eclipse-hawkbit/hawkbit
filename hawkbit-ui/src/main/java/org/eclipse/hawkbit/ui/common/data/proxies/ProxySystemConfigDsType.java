package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;

public class ProxySystemConfigDsType extends ProxySystemConfigWindow implements Serializable {
    private static final long serialVersionUID = 1L;

    private ProxyTypeInfo dsTypeInfo;

    public ProxyTypeInfo getDsTypeInfo() {
        return dsTypeInfo;
    }

    public void setDsTypeInfo(final ProxyTypeInfo dsTypeInfo) {
        this.dsTypeInfo = dsTypeInfo;
    }

}
