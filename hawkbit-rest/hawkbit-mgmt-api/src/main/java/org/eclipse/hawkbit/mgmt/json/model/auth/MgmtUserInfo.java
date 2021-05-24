package org.eclipse.hawkbit.mgmt.json.model.auth;

public class MgmtUserInfo {

    private String username;
    private String tenant;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

}
