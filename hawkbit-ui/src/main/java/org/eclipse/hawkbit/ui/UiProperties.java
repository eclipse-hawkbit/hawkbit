/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties for Management UI customization.
 *
 */
@Component
@ConfigurationProperties("hawkbit.server.ui")
public class UiProperties {

    private final Links links = new Links();
    private final Login login = new Login();
    private final Demo demo = new Demo();

    public Login getLogin() {
        return login;
    }

    public Links getLinks() {
        return links;
    }

    public Demo getDemo() {
        return demo;
    }

    /**
     * Demo account login information.
     *
     */
    public static class Demo {

        /**
         * Demo tenant.
         */
        private String tenant = "DEFAULT";
        /**
         * Demo user name.
         */
        private String user = "admin";

        /**
         * Demo user password.
         */
        private String password = "admin";

        public String getTenant() {
            return tenant;
        }

        public void setTenant(final String tenant) {
            this.tenant = tenant;
        }

        public String getUser() {
            return user;
        }

        public void setUser(final String user) {
            this.user = user;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(final String password) {
            this.password = password;
        }

    }

    /**
     * Links to potentially other systems (e.g. support, user management etc.).
     *
     */
    public static class Links {
        /**
         * Link to product support.
         */
        private String support = "";

        /**
         * Link to request a system account, access.
         */
        private String requestAccount = "";

        /**
         * Link to user management.
         */
        private String userManagement = "";

        public String getSupport() {
            return support;
        }

        public void setSupport(final String support) {
            this.support = support;
        }

        public String getRequestAccount() {
            return requestAccount;
        }

        public void setRequestAccount(final String requestAccount) {
            this.requestAccount = requestAccount;
        }

        public String getUserManagement() {
            return userManagement;
        }

        public void setUserManagement(final String userManagement) {
            this.userManagement = userManagement;
        }

    }

    /**
     * Configuration of login view.
     *
     */
    public static class Login {

        private final Cookie cookie = new Cookie();

        public Cookie getCookie() {
            return cookie;
        }

        /**
         * Cookie configuration for login credential cookie.
         *
         */
        public static class Cookie {
            /**
             * Secure cookie enabled.
             */
            private boolean secure = true;

            public boolean isSecure() {
                return secure;
            }

            public void setSecure(final boolean secure) {
                this.secure = secure;
            }
        }
    }

}
