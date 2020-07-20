package org.eclipse.hawkbit.ui.common.data.proxies;

import java.io.Serializable;
import java.time.Duration;

import org.eclipse.hawkbit.ui.tenantconfiguration.repository.ActionAutoCleanupConfigurationItem.ActionStatusOption;

/**
 * Proxy for system config window.
 */
public class ProxySystemConfigWindow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String description;
    private Long distributionSetTypeId;
    private Long repositoryConfigId;
    private Long rolloutConfigId;
    private Long caRootAuthorityId;
    private String caRootAuthority;
    private String gatewaySecurityToken;
    private ActionStatusOption actionCleanupStatus;
    private boolean pollingOverdue;
    private transient Duration pollingOverdueDuration;
    private boolean pollingTime;
    private transient Duration pollingTimeDuration;
    private String actionExpiryDays;
    private boolean rolloutApproval;
    private boolean actionAutoclose;
    private boolean actionAutocleanup;
    private boolean multiAssignments;
    private boolean certificateAuth;
    private boolean targetSecToken;
    private boolean gatewaySecToken;
    private boolean downloadAnonymous;
    private Long authConfigId;
    private Long pollingConfigId;

    /**
     * Gets the id
     *
     * @return id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id
     *
     * @param id
     *            System config window id
     */
    public void setId(final Long id) {
        this.id = id;
    }

    /**
     * Gets the name
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name
     *
     * @param name
     *            System config window name
     */
    public void setName(final String name) {
        this.name = name;
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

    /**
     * Flag that indicates if the polling time option is enabled.
     *
     * @return <code>true</code> if the polling time is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isPollingTime() {
        return pollingTime;
    }

    /**
     * Sets the flag that indicates if the polling time option is enabled.
     *
     * @param pollingTime
     *            <code>true</code> if the polling time is enabled, otherwise
     *            <code>false</code>
     */
    public void setPollingTime(final boolean pollingTime) {
        this.pollingTime = pollingTime;
    }

    /**
     * Gets the pollingTimeDuration
     *
     * @return pollingTimeDuration
     */
    public Duration getPollingTimeDuration() {
        return pollingTimeDuration;
    }

    /**
     * Sets the pollingTimeDuration
     *
     * @param pollingTimeDuration
     *            System config window pollingTimeDuration
     */
    public void setPollingTimeDuration(final Duration pollingTimeDuration) {
        this.pollingTimeDuration = pollingTimeDuration;
    }

    /**
     * Flag that indicates if the polling overdue time option is enabled.
     *
     * @return <code>true</code> if the polling overdue time is enabled,
     *         otherwise <code>false</code>
     */
    public boolean isPollingOverdue() {
        return pollingOverdue;
    }

    /**
     * Sets the flag that indicates if the polling overdue time option is
     * enabled.
     *
     * @param pollingOverdue
     *            <code>true</code> if the polling overdue time is enabled,
     *            otherwise <code>false</code>
     */
    public void setPollingOverdue(final boolean pollingOverdue) {
        this.pollingOverdue = pollingOverdue;
    }

    /**
     * Gets the pollingOverdueDuration
     *
     * @return pollingOverdueDuration
     */
    public Duration getPollingOverdueDuration() {
        return pollingOverdueDuration;
    }

    /**
     * Sets the pollingOverdueDuration
     *
     * @param pollingOverdueDuration
     *            System config window pollingOverdueDuration
     */
    public void setPollingOverdueDuration(final Duration pollingOverdueDuration) {
        this.pollingOverdueDuration = pollingOverdueDuration;
    }

    /**
     * Gets the description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description
     *
     * @param description
     *            System config window description
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     * Gets the id of distributionSetType
     *
     * @return distributionSetTypeId
     */
    public Long getDistributionSetTypeId() {
        return distributionSetTypeId;
    }

    /**
     * Sets the distributionSetTypeId
     *
     * @param distributionSetTypeId
     *            System config window distributionSetTypeId
     */
    public void setDistributionSetTypeId(final Long distributionSetTypeId) {
        this.distributionSetTypeId = distributionSetTypeId;
    }

    /**
     * Gets the actionExpiryDays
     *
     * @return actionExpiryDays
     */
    public String getActionExpiryDays() {
        return actionExpiryDays;
    }

    /**
     * Sets the actionExpiryDays
     *
     * @param actionExpiryDays
     *            System config window actionExpiryDays
     */
    public void setActionExpiryDays(final String actionExpiryDays) {
        this.actionExpiryDays = actionExpiryDays;
    }

    /**
     * Gets the actionCleanupStatus
     *
     * @return actionCleanupStatus
     */
    public ActionStatusOption getActionCleanupStatus() {
        return actionCleanupStatus;
    }

    /**
     * Sets the actionCleanupStatus
     *
     * @param actionCleanupStatus
     *            System config window actionCleanupStatus
     */
    public void setActionCleanupStatus(final ActionStatusOption actionCleanupStatus) {
        this.actionCleanupStatus = actionCleanupStatus;
    }

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
     *            <code>true</code> if the downloadAnonymous is enabled,
     *            otherwise <code>false</code>
     */
    public void setDownloadAnonymous(final boolean downloadAnonymous) {
        this.downloadAnonymous = downloadAnonymous;
    }

    /**
     * Flag that indicates if the actionAutocleanup option is enabled.
     *
     * @return <code>true</code> if the actionAutocleanup is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isActionAutocleanup() {
        return actionAutocleanup;
    }

    /**
     * Sets the flag that indicates if the actionAutocleanup option is enabled.
     *
     * @param actionAutocleanup
     *            <code>true</code> if the actionAutocleanup is enabled,
     *            otherwise <code>false</code>
     */
    public void setActionAutocleanup(final boolean actionAutocleanup) {
        this.actionAutocleanup = actionAutocleanup;
    }

    /**
     * Flag that indicates if the multiAssignments option is enabled.
     *
     * @return <code>true</code> if the multiAssignments is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isMultiAssignments() {
        return multiAssignments;
    }

    /**
     * Sets the flag that indicates if the multiAssignments option is enabled.
     *
     * @param multiAssignments
     *            <code>true</code> if the multiAssignments is enabled,
     *            otherwise <code>false</code>
     */
    public void setMultiAssignments(final boolean multiAssignments) {
        this.multiAssignments = multiAssignments;
    }

    /**
     * Flag that indicates if the actionAutoclose option is enabled.
     *
     * @return <code>true</code> if the actionAutoclose is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isActionAutoclose() {
        return actionAutoclose;
    }

    /**
     * Sets the flag that indicates if the actionAutoclose option is enabled.
     *
     * @param actionAutoclose
     *            <code>true</code> if the actionAutoclose is enabled, otherwise
     *            <code>false</code>
     */
    public void setActionAutoclose(final boolean actionAutoclose) {
        this.actionAutoclose = actionAutoclose;
    }

    /**
     * Flag that indicates if the rolloutApproval option is enabled.
     *
     * @return <code>true</code> if the rolloutApproval is enabled, otherwise
     *         <code>false</code>
     */
    public boolean isRolloutApproval() {
        return rolloutApproval;
    }

    /**
     * Sets the flag that indicates if the rolloutApproval option is enabled.
     *
     * @param rolloutApproval
     *            <code>true</code> if the rolloutApproval is enabled, otherwise
     *            <code>false</code>
     */
    public void setRolloutApproval(final boolean rolloutApproval) {
        this.rolloutApproval = rolloutApproval;
    }

    public Long getRepositoryConfigId() {
        return repositoryConfigId;
    }

    public void setRepositoryConfigId(final Long repositoryConfigId) {
        this.repositoryConfigId = repositoryConfigId;
    }

    public Long getRolloutConfigId() {
        return rolloutConfigId;
    }

    public void setRolloutConfigId(final Long rolloutConfigId) {
        this.rolloutConfigId = rolloutConfigId;
    }

    public Long getAuthConfigId() {
        return authConfigId;
    }

    public void setAuthConfigId(final Long authConfigId) {
        this.authConfigId = authConfigId;
    }

    public Long getPollingConfigId() {
        return pollingConfigId;
    }

    public void setPollingConfigId(final Long pollingConfigId) {
        this.pollingConfigId = pollingConfigId;
    }
}
