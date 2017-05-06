/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.login;

import java.net.URI;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.Cookie;

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.TenantUserPasswordAuthenticationToken;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.AntPathMatcher;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinService;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.Position;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Login view for login credentials.
 */
@SpringView(name = "")
public class LoginView extends VerticalLayout implements View {
    private static final String TENANT_PATTERN_PLACEHOLDER = "tenant";
    private static final String USER_PATTERN_PLACEHOLDER = "user";
    private static final String LOGIN_TENANT_USER_URL_PATTERN = "**/#/{" + TENANT_PATTERN_PLACEHOLDER + "}/{"
            + USER_PATTERN_PLACEHOLDER + "}";
    private static final String LOGIN_USER_URL_PATTERN = "**/#/{" + USER_PATTERN_PLACEHOLDER + "}";

    private static final String LOGIN_TEXTFIELD = "login-textfield";
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginView.class);

    private static final String SP_LOGIN_USER = "sp-login-user";
    private static final String SP_LOGIN_TENANT = "sp-login-tenant";

    private final transient VaadinSecurity vaadinSecurity;

    private final VaadinMessageSource i18n;

    private final UiProperties uiProperties;

    private final transient MultitenancyIndicator multiTenancyIndicator;

    private final transient AntPathMatcher matcher = new AntPathMatcher();
    private boolean useCookie = true;

    private TextField username;
    private TextField tenant;
    private PasswordField password;
    private Button signin;

    @Autowired
    LoginView(final VaadinSecurity vaadinSecurity, final VaadinMessageSource i18n, final UiProperties uiProperties,
            final MultitenancyIndicator multiTenancyIndicator) {
        this.vaadinSecurity = vaadinSecurity;
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.multiTenancyIndicator = multiTenancyIndicator;
    }

    void loginAuthenticationFailedNotification() {
        final Notification notification = new Notification(i18n.getMessage("notification.login.failed.title"));
        notification.setDescription(i18n.getMessage("notification.login.failed.description"));
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("error closable");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.setDelayMsec(1000);
        notification.show(Page.getCurrent());
    }

    void loginCredentialsExpiredNotification() {
        final Notification notification = new Notification(
                i18n.getMessage("notification.login.failed.credentialsexpired.title"));
        notification.setDescription(i18n.getMessage("notification.login.failed.credentialsexpired.description"));
        notification.setDelayMsec(10000);
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("error closeable");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.show(Page.getCurrent());
    }

    /**
     * Renders the {@link View}.
     */
    @PostConstruct
    public void render() {
        final URI spURI = Page.getCurrent().getLocation();
        final String uriPath = spURI.toString();
        if (uriPath.contains("?demo")) {
            login(uiProperties.getDemo().getTenant(), uiProperties.getDemo().getUser(),
                    uiProperties.getDemo().getPassword(), false);
        }
        final Component loginForm = buildLoginForm();
        addComponent(loginForm);
        setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);

        readCredentialsFromUriPath(uriPath);
    }

    private void readCredentialsFromUriPath(final String uriPath) {
        String urlTenant = null;
        String urlUser = null;
        if (matcher.match(LOGIN_USER_URL_PATTERN, uriPath)) {
            urlUser = matcher.extractUriTemplateVariables(LOGIN_USER_URL_PATTERN, uriPath)
                    .get(USER_PATTERN_PLACEHOLDER);
        } else if (matcher.match(LOGIN_TENANT_USER_URL_PATTERN, uriPath)) {
            final Map<String, String> extractUriTemplateVariables = matcher
                    .extractUriTemplateVariables(LOGIN_TENANT_USER_URL_PATTERN, uriPath);
            urlTenant = extractUriTemplateVariables.get(TENANT_PATTERN_PLACEHOLDER);
            urlUser = extractUriTemplateVariables.get(USER_PATTERN_PLACEHOLDER);
        }

        if (urlUser != null) {
            useCookie = false;
            filloutUsernameTenantFields(urlTenant, urlUser);
        }
    }

    private void filloutUsernameTenantFields(final String tenantValue, final String userValue) {
        if (tenant != null && tenantValue != null) {
            tenant.setValue(tenantValue);
        }
        if (userValue != null) {
            username.setValue(userValue);
        }
    }

    private Component buildLoginForm() {

        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        loginPanel.setSpacing(true);
        loginPanel.addStyleName("dashboard-view");
        loginPanel.addStyleName("login-panel");
        Responsive.makeResponsive(loginPanel);
        loginPanel.addComponent(buildFields());
        loginPanel.addComponent(buildLinks());

        // Check if IE browser is not supported ( < IE11 )
        if (isUnsupportedBrowser()) {
            // Disable sign-in button and display a message
            signin.setEnabled(Boolean.FALSE);
            loginPanel.addComponent(buildUnsupportedMessage());
        }

        return loginPanel;
    }

    private Component buildFields() {
        final HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("fields");
        buildTenantField();
        buildUserField();
        buildPasswordField();
        buildSignInButton();
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            fields.addComponents(tenant, username, password, signin);
        } else {
            fields.addComponents(username, password, signin);
        }
        fields.setComponentAlignment(signin, Alignment.BOTTOM_LEFT);
        signin.addClickListener(event -> handleLogin());

        return fields;
    }

    private void handleLogin() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            final boolean textFieldsNotEmtpy = hasTenantFieldText() && hasUserFieldText() && hashPasswordFieldText();
            if (textFieldsNotEmtpy) {
                login(tenant.getValue(), username.getValue(), password.getValue(), true);
            }
        } else if (!multiTenancyIndicator.isMultiTenancySupported() && hasUserFieldText() && hashPasswordFieldText()) {
            login(null, username.getValue(), password.getValue(), true);
        }
    }

    private boolean hashPasswordFieldText() {
        return !password.isEmpty();
    }

    private boolean hasUserFieldText() {
        return !username.isEmpty();
    }

    private boolean hasTenantFieldText() {
        return !tenant.isEmpty();
    }

    private void buildSignInButton() {
        signin = new Button(i18n.getMessage("button.login.signin"));
        signin.addStyleName(ValoTheme.BUTTON_PRIMARY + " " + ValoTheme.BUTTON_SMALL + " " + "login-button");
        signin.setClickShortcut(KeyCode.ENTER);
        signin.focus();
        signin.setId("login-signin");
    }

    private void buildPasswordField() {
        password = new PasswordField(i18n.getMessage("label.login.password"));
        password.setIcon(FontAwesome.LOCK);
        password.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        password.setId("login-password");
    }

    private void buildUserField() {
        username = new TextField(i18n.getMessage("label.login.username"));
        username.setIcon(FontAwesome.USER);
        username.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        username.setId("login-username");
    }

    private void buildTenantField() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            tenant = new TextField(i18n.getMessage("label.login.tenant"));
            tenant.setIcon(FontAwesome.DATABASE);
            tenant.addStyleName(
                    ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
            tenant.addStyleName("uppercase");
            tenant.setId("login-tenant");
        }
    }

    private Component buildLinks() {

        final HorizontalLayout links = new HorizontalLayout();
        links.setSpacing(true);
        links.addStyleName("links");
        final String linkStyle = "v-link";

        if (!uiProperties.getLinks().getDocumentation().getRoot().isEmpty()) {
            final Link docuLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_DOCUMENTATION,
                    i18n.getMessage("link.documentation.name"), uiProperties.getLinks().getDocumentation().getRoot(),
                    FontAwesome.QUESTION_CIRCLE, "_blank", linkStyle);
            links.addComponent(docuLink);
            docuLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        if (!uiProperties.getDemo().getUser().isEmpty()) {
            final Link demoLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_DEMO,
                    i18n.getMessage("link.demo.name"), "?demo", FontAwesome.DESKTOP, "_top", linkStyle);
            links.addComponent(demoLink);
            demoLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        if (!uiProperties.getLinks().getRequestAccount().isEmpty()) {
            final Link requestAccountLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_REQUESTACCOUNT,
                    i18n.getMessage("link.requestaccount.name"), uiProperties.getLinks().getRequestAccount(),
                    FontAwesome.SHOPPING_CART, "", linkStyle);
            links.addComponent(requestAccountLink);
            requestAccountLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        if (!uiProperties.getLinks().getUserManagement().isEmpty()) {
            final Link userManagementLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_USERMANAGEMENT,
                    i18n.getMessage("link.usermanagement.name"), uiProperties.getLinks().getUserManagement(),
                    FontAwesome.USERS, "_blank", linkStyle);
            links.addComponent(userManagementLink);
            userManagementLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        return links;
    }

    private Component buildUnsupportedMessage() {
        final Label label = new Label(i18n.getMessage("label.unsupported.browser.ie"));
        label.addStyleName(ValoTheme.LABEL_FAILURE);
        return label;
    }

    private boolean isUnsupportedBrowser() {
        final WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
        if (webBrowser.isIE() && webBrowser.getBrowserMajorVersion() < 11) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.vaadin.navigator.View#enter(com.vaadin.navigator.ViewChangeListener.
     * ViewChangeEvent)
     */
    @Override
    public void enter(final ViewChangeEvent event) {
        if (!useCookie) {
            return;
        }

        final Cookie usernameCookie = getCookieByName(SP_LOGIN_USER);

        if (usernameCookie != null) {
            final String previousUser = usernameCookie.getValue();
            username.setValue(previousUser);
            password.focus();
        } else {
            username.focus();
        }

        final Cookie tenantCookie = getCookieByName(SP_LOGIN_TENANT);

        if (tenantCookie != null && multiTenancyIndicator.isMultiTenancySupported()) {
            final String previousTenant = tenantCookie.getValue();
            tenant.setValue(previousTenant.toUpperCase());
        } else if (multiTenancyIndicator.isMultiTenancySupported()) {
            tenant.focus();
        } else {
            username.focus();
        }
    }

    private void setCookies() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            final Cookie tenantCookie = new Cookie(SP_LOGIN_TENANT, tenant.getValue().toUpperCase());
            tenantCookie.setPath("/");
            // 100 days
            tenantCookie.setMaxAge(3600 * 24 * 100);
            tenantCookie.setHttpOnly(true);
            tenantCookie.setSecure(uiProperties.getLogin().getCookie().isSecure());
            VaadinService.getCurrentResponse().addCookie(tenantCookie);
        }

        final Cookie usernameCookie = new Cookie(SP_LOGIN_USER, username.getValue());
        usernameCookie.setPath("/");
        // 100 days
        usernameCookie.setMaxAge(3600 * 24 * 100);
        usernameCookie.setHttpOnly(true);
        usernameCookie.setSecure(uiProperties.getLogin().getCookie().isSecure());
        VaadinService.getCurrentResponse().addCookie(usernameCookie);
    }

    private Cookie getCookieByName(final String name) {
        // Fetch all cookies from the request
        final Cookie[] cookies = VaadinService.getCurrentRequest().getCookies();

        if (cookies != null) {
            // Iterate to find cookie by its name
            for (final Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }

        return null;
    }

    private void login(final String tentant, final String user, final String password, final boolean setCookies) {
        try {
            if (multiTenancyIndicator.isMultiTenancySupported()) {
                vaadinSecurity.login(new TenantUserPasswordAuthenticationToken(tentant, user, password));
            } else {
                vaadinSecurity.login(new UsernamePasswordAuthenticationToken(user, password));
            }
            /* set success login cookies */
            if (setCookies && useCookie) {
                setCookies();
            }

        } catch (final CredentialsExpiredException e) {
            LOGGER.debug("Credential expired", e);
            loginCredentialsExpiredNotification();
        } catch (final AuthenticationException e) {
            LOGGER.debug("Authentication failed", e);
            /* if not successful */
            loginAuthenticationFailedNotification();
        } catch (final Exception e) {
            LOGGER.debug("Login failed", e);
            loginAuthenticationFailedNotification();
        }
    }
}
