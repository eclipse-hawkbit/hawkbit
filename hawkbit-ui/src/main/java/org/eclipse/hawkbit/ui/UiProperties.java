/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Properties for Management UI customization.
 *
 */
@ConfigurationProperties("hawkbit.server.ui")
public class UiProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean gravatar;

    private String fixedTimeZone;

    private final Localization localization = new Localization();

    private final Links links = new Links();

    private final Demo demo = new Demo();

    private final Event event = new Event();

    /**
     * @return True if menu item has gravatar else false
     */
    public boolean isGravatar() {
        return gravatar;
    }

    /**
     * Sets the gravatar
     *
     * @param gravatar
     *            Menu icon
     */
    public void setGravatar(final boolean gravatar) {
        this.gravatar = gravatar;
    }

    /**
     * @return Fixed time zone if unknown then GMT
     */
    public String getFixedTimeZone() {
        return fixedTimeZone;
    }

    /**
     * Sets the fixed time zone
     *
     * @param fixedTimeZone
     *            Date time zone
     */
    public void setFixedTimeZone(final String fixedTimeZone) {
        this.fixedTimeZone = fixedTimeZone;
    }

    /**
     * Localization information
     */
    public static class Localization implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Default localization
         */
        private Locale defaultLocal = Locale.ENGLISH;

        /**
         * List of available localizations
         */
        private List<Locale> availableLocals = Collections.singletonList(Locale.ENGLISH);

        /**
         * @return Default locale
         */
        public Locale getDefaultLocal() {
            return defaultLocal;
        }

        /**
         * @return List of available locale
         */
        public List<Locale> getAvailableLocals() {
            return availableLocals;
        }

        /**
         * Sets the default locale
         *
         * @param defaultLocal
         *            Locale
         */
        public void setDefaultLocal(final Locale defaultLocal) {
            this.defaultLocal = defaultLocal;
        }

        /**
         * Sets the all available locale
         *
         * @param availableLocals
         *            List of locale
         */
        public void setAvailableLocals(final List<Locale> availableLocals) {
            this.availableLocals = availableLocals;
        }
    }

    /**
     * Demo account login information.
     *
     */
    public static class Demo implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Demo tenant.
         */
        private String tenant = "";
        /**
         * Demo user name.
         */
        private String user = "";

        /**
         * Demo user password.
         */
        // Exception squid:S2068 - Empty password
        @SuppressWarnings({ "squid:S2068" })
        private String password = "";

        private String disclaimer = "";

        /**
         * @return password
         */
        public String getPassword() {
            return password;
        }

        /**
         * @return tenant
         */
        public String getTenant() {
            return tenant;
        }

        /**
         * @return username
         */
        public String getUser() {
            return user;
        }

        /**
         * Sets the login password
         *
         * @param password
         *            Password value
         */
        public void setPassword(final String password) {
            this.password = password;
        }

        /**
         * Sets the tenant
         *
         * @param tenant
         *            Tenant value
         */
        public void setTenant(final String tenant) {
            this.tenant = tenant;
        }

        /**
         * Sets the login user
         *
         * @param user
         *            username
         */
        public void setUser(final String user) {
            this.user = user;
        }

        /**
         * @return disclaimer
         */
        public String getDisclaimer() {
            return disclaimer;
        }

        /**
         * Sets the disclaimer
         *
         * @param disclaimer
         *            Disclaimer value
         */
        public void setDisclaimer(final String disclaimer) {
            this.disclaimer = disclaimer;
        }
    }

    /**
     * Links to potentially other systems (e.g. support, user management,
     * documentation etc.).
     *
     */
    public static class Links implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * Configuration of UI documentation links.
         *
         */
        public static class Documentation implements Serializable {
            private static final long serialVersionUID = 1L;
            /**
             * Link to root of documentation and user guides.
             */
            private String root = "";

            /**
             * Link to documentation of deployment view.
             */
            private String deploymentView = "";

            /**
             * Link to documentation of distribution view.
             */
            private String distributionView = "";

            /**
             * Link to documentation of upload view.
             */
            private String uploadView = "";

            /**
             * Link to documentation of maintenance window view.
             */
            private String maintenanceWindowView = "";

            /**
             * Link to documentation of the user consent and confirmation flow.
             */
            private String userConsentAndConfirmationGuide = "";

            public String getAutoConfirmationView() {
                return autoConfirmationView;
            }

            /**
             * Link to documentation of system configuration view.
             */
            private String systemConfigurationView = "";

            /**
             * Link to security related documentation.
             */
            private String security = "";

            /**
             * Link to rollout related documentation.
             */
            private String rollout = "";

            /**
             * Link to target filter view.
             */
            private String targetfilterView = "";

            /**
             * Link to documentation of rollout view.
             */
            private String rolloutView = "";

            /**
             * Link to documentation of auto confirmation view.
             */
            private String autoConfirmationView = "";

            /**
             * Link to documentation of state machine
             */
            private String provisioningStateMachine = "";

            /**
             * Link to documentation of distribution set invalidation
             */
            private String distributionSetInvalidation = "";

            /**
             * @return Link to documentation of deployment view
             */
            public String getDeploymentView() {
                return deploymentView;
            }

            public String getDistributionView() {
                return distributionView;
            }

            /**
             * @return Link to documentation of rollout view
             */
            public String getRolloutView() {
                return rolloutView;
            }

            /**
             * @return Link to documentation of root
             */
            public String getRoot() {
                return root;
            }

            /**
             * @return Link to documentation of security
             */
            public String getSecurity() {
                return security;
            }

            /**
             * @return Link to documentation of rollout
             */
            public String getRollout() {
                return rollout;
            }

            /**
             * @return Link to documentation of system config
             */
            public String getSystemConfigurationView() {
                return systemConfigurationView;
            }

            /**
             * @return Link to documentation of target filter
             */
            public String getTargetfilterView() {
                return targetfilterView;
            }

            public String getUploadView() {
                return uploadView;
            }

            /**
             * @return Link to documentation of maintenance window
             */
            public String getMaintenanceWindowView() {
                return maintenanceWindowView;
            }

            /**
             * @return Link to documentation of the user consent and confirmation flow.
             */
            public String getUserConsentAndConfirmationGuide() {
                return userConsentAndConfirmationGuide;
            }

            /**
             * @return Link to documentation of provisioning state machine
             */
            public String getProvisioningStateMachine() {
                return provisioningStateMachine;
            }

            /**
             * @return Link to documentation of distribution set invalidation
             */
            public String getDistributionSetInvalidation() {
                return distributionSetInvalidation;
            }

            public void setDeploymentView(final String deploymentView) {
                this.deploymentView = deploymentView;
            }

            public void setDistributionView(final String distributionView) {
                this.distributionView = distributionView;
            }

            public void setRolloutView(final String rolloutView) {
                this.rolloutView = rolloutView;
            }

            /**
             * Sets the root documentation link
             *
             * @param root
             *            link
             */
            public void setRoot(final String root) {
                this.root = root;
            }

            /**
             * Sets the security documentation link
             *
             * @param security
             *            link
             */
            public void setSecurity(final String security) {
                this.security = security;
            }

            /**
             * Sets the rollout documentation link
             *
             * @param rollout
             *            link
             */
            public void setRollout(final String rollout) {
                this.rollout = rollout;
            }

            public void setSystemConfigurationView(final String systemConfigurationView) {
                this.systemConfigurationView = systemConfigurationView;
            }

            public void setTargetfilterView(final String targetfilterView) {
                this.targetfilterView = targetfilterView;
            }

            public void setUploadView(final String uploadView) {
                this.uploadView = uploadView;
            }

            public void setMaintenanceWindowView(final String maintenanceWindowView) {
                this.maintenanceWindowView = maintenanceWindowView;
            }

            public void setUserConsentAndConfirmationGuide(final String userConsentAndConfirmationGuide) {
                this.userConsentAndConfirmationGuide = userConsentAndConfirmationGuide;
            }

            public void setProvisioningStateMachine(final String provisioningStateMachine) {
                this.provisioningStateMachine = provisioningStateMachine;
            }

            /**
             * Sets the link to the distribution set invalidation documentation
             * 
             * @param distributionSetInvalidation
             *            Link
             */
            public void setDistributionSetInvalidation(String distributionSetInvalidation) {
                this.distributionSetInvalidation = distributionSetInvalidation;
            }
        }

        private final Documentation documentation = new Documentation();

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

        /**
         * @return Link to documentation
         */
        public Documentation getDocumentation() {
            return documentation;
        }

        /**
         * @return Link to request a system account, access
         */
        public String getRequestAccount() {
            return requestAccount;
        }

        /**
         * @return Link to product support
         */
        public String getSupport() {
            return support;
        }

        /**
         * @return Link to user management
         */
        public String getUserManagement() {
            return userManagement;
        }

        /**
         * Sets the link to request a system account, access
         *
         * @param requestAccount
         *            Link
         */
        public void setRequestAccount(final String requestAccount) {
            this.requestAccount = requestAccount;
        }

        /**
         * Sets the link to product support
         *
         * @param support
         *            Link
         */
        public void setSupport(final String support) {
            this.support = support;
        }

        /**
         * Sets the link to user management
         *
         * @param userManagement
         *            Link
         */
        public void setUserManagement(final String userManagement) {
            this.userManagement = userManagement;
        }

    }

    /**
     * Configuration of the UI event bus.
     */
    public static class Event implements Serializable {
        private static final long serialVersionUID = 1L;

        /**
         * 
         * Configuration of the UI push.
         *
         */
        public static class Push implements Serializable {
            private static final long serialVersionUID = 1L;

            /**
             * The delay for the ui event forwarding.
             */
            private long delay = TimeUnit.SECONDS.toMillis(2);

            public long getDelay() {
                return delay;
            }

            public void setDelay(final long delay) {
                this.delay = delay;
            }
        }

        private final Push push = new Push();

        public Push getPush() {
            return push;
        }
    }

    /**
     * @return Demo account details
     */
    public Demo getDemo() {
        return demo;
    }

    /**
     * @return Document links
     */
    public Links getLinks() {
        return links;
    }

    /**
     * @return Events
     */
    public Event getEvent() {
        return event;
    }

    /**
     * @return Localization
     */
    public Localization getLocalization() {
        return localization;
    }

}
