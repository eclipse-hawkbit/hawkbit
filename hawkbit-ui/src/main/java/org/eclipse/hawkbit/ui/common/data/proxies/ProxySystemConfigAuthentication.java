/**
 * Copyright (c) 2020 Bosch.IO GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.common.data.proxies;

/**
 * Proxy for the Authentication view of system config window
 */
public class ProxySystemConfigAuthentication extends ProxySystemConfigWindow {
    private static final long serialVersionUID = 1L;

    private Long caRootAuthorityId;
    private boolean certificateAuth;
    private boolean targetSecToken;
    private boolean gatewaySecToken;
    private boolean downloadAnonymous;
    private String caRootAuthority;
    private String gatewaySecurityToken;

    public Long getCaRootAuthorityId() {
        return caRootAuthorityId;
    }

    public void setCaRootAuthorityId(final Long caRootAuthorityId) {
        this.caRootAuthorityId = caRootAuthorityId;
    }

    /**
     * Flag that indicates if the certificateAuth option is enabled.
     *
     * @return <code>true</code> if the certificateAuth is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isCertificateAuth() {
        return certificateAuth;
    }

    /**
     * Sets the flag that indicates if the certificateAuth option is enabled.
     *
     * @param certificateAuth
     *            <code>true</code> if the certificateAuth is enabled, otherwise
     *            <code>false</code>
     */
    public void setCertificateAuth(final boolean certificateAuth) {
        this.certificateAuth = certificateAuth;
    }

    /**
     * Flag that indicates if the targetSecToken option is enabled.
     *
     * @return <code>true</code> if the targetSecToken is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isTargetSecToken() {
        return targetSecToken;
    }

    /**
     * Sets the flag that indicates if the targetSecToken option is enabled.
     *
     * @param targetSecToken
     *            <code>true</code> if the targetSecToken is enabled, otherwise
     *            <code>false</code>
     */
    public void setTargetSecToken(final boolean targetSecToken) {
        this.targetSecToken = targetSecToken;
    }

    /**
     * Flag that indicates if the gatewaySecToken option is enabled.
     *
     * @return <code>true</code> if the gatewaySecToken is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isGatewaySecToken() {
        return gatewaySecToken;
    }

    /**
     * Sets the flag that indicates if the gatewaySecToken option is enabled.
     *
     * @param gatewaySecToken
     *            <code>true</code> if the gatewaySecToken is enabled, otherwise
     *            <code>false</code>
     */
    public void setGatewaySecToken(final boolean gatewaySecToken) {
        this.gatewaySecToken = gatewaySecToken;
    }

    /**
     * Flag that indicates if the downloadAnonymous option is enabled.
     *
     * @return <code>true</code> if the downloadAnonymous is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isDownloadAnonymous() {
        return downloadAnonymous;
    }

    /**
     * Sets the flag that indicates if the downloadAnonymous option is enabled.
     *
     * @param downloadAnonymous
     *            <code>true</code> if the downloadAnonymous is enabled, otherwise
     *            <code>false</code>
     */
    public void setDownloadAnonymous(final boolean downloadAnonymous) {
        this.downloadAnonymous = downloadAnonymous;
    }

    /**
     * Gets the caRootAuthority
     *
     * @return caRootAuthority
     */
    public String getCaRootAuthority() {
        return caRootAuthority;
    }

    /**
     * Sets the caRootAuthority
     *
     * @param caRootAuthority
     *            System config window caRootAuthority
     */
    public void setCaRootAuthority(final String caRootAuthority) {
        this.caRootAuthority = caRootAuthority;
    }

    /**
     * Gets the gatewaySecurityToken
     *
     * @return gatewaySecurityToken
     */
    public String getGatewaySecurityToken() {
        return gatewaySecurityToken;
    }

    /**
     * Sets the gatewaySecurityToken
     *
     * @param gatewaySecurityToken
     *            System config window gatewaySecurityToken
     */
    public void setGatewaySecurityToken(final String gatewaySecurityToken) {
        this.gatewaySecurityToken = gatewaySecurityToken;
    }

}
