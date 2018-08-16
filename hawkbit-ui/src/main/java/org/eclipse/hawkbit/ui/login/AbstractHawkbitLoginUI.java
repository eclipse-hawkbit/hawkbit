/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.ui.login;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.servlet.http.Cookie;

import com.vaadin.shared.ui.label.ContentMode;
import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.TenantUserPasswordAuthenticationToken;
import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.VaadinMessageSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.Position;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
import com.vaadin.ui.Notification;
import com.vaadin.ui.PasswordField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.ValoTheme;

/**
 * Login UI window that is independent of the {@link AbstractHawkbitUI} itself.
 *
 */
@Title("hawkBit UI - Login")
@Widgetset(value = HawkbitTheme.WIDGET_SET_NAME)
@Theme(HawkbitTheme.THEME_NAME)
public abstract class AbstractHawkbitLoginUI extends UI {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHawkbitLoginUI.class);

    private static final long serialVersionUID = 1L;

    private static final String USER_PARAMETER = "user";
    private static final String TENANT_PARAMETER = "tenant";
    private static final int HUNDRED_DAYS_IN_SECONDS = Math.toIntExact(TimeUnit.DAYS.toSeconds(100));
    private static final String LOGIN_TEXTFIELD = "login-textfield";

    private static final String SP_LOGIN_USER = "sp-login-user";
    private static final String SP_LOGIN_TENANT = "sp-login-tenant";
    private static final Pattern FORBIDDEN_COOKIE_CONTENT = Pattern.compile("(\\s|.)*(<|>)(\\s|.)*");

    private final transient ApplicationContext context;

    private final transient VaadinSecurity vaadinSecurity;

    private final VaadinMessageSource i18n;

    private final UiProperties uiProperties;

    private final transient MultitenancyIndicator multiTenancyIndicator;

    private final boolean isDemo;

    private boolean useCookie = true;

    private TextField username;
    private TextField tenant;
    private PasswordField password;
    private Button signIn;

    private MultiValueMap<String, String> params;

    @Autowired
    protected AbstractHawkbitLoginUI(final ApplicationContext context, final VaadinSecurity vaadinSecurity,
            final VaadinMessageSource i18n, final UiProperties uiProperties,
            final MultitenancyIndicator multiTenancyIndicator) {
        this.context = context;
        this.vaadinSecurity = vaadinSecurity;
        this.i18n = i18n;
        this.uiProperties = uiProperties;
        this.multiTenancyIndicator = multiTenancyIndicator;
        this.isDemo = !uiProperties.getDemo().getDisclaimer().isEmpty();
    }

    @Override
    protected void init(final VaadinRequest request) {
        SpringContextHelper.setContext(context);

        params = UriComponentsBuilder.fromUri(Page.getCurrent().getLocation()).build().getQueryParams();

        setContent(buildContent());

        fillOutUsernameTenantFields();
        readCookie();
    }

    private VerticalLayout buildContent() {
        final VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSizeFull();
        rootLayout.setStyleName("main-content");

        rootLayout.addComponent(buildHeader());

        addLoginForm(rootLayout);

        addFooter(rootLayout);

        return rootLayout;
    }

    private void addLoginForm(final VerticalLayout rootLayout) {
        final Component loginForm = buildLoginForm();
        rootLayout.addComponent(loginForm);
        rootLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
    }

    private void addFooter(final VerticalLayout rootLayout) {
        final Resource resource = context
                .getResource("classpath:/VAADIN/themes/" + UI.getCurrent().getTheme() + "/layouts/footer.html");

        try (final InputStream resourceStream = resource.getInputStream()) {
            final CustomLayout customLayout = new CustomLayout(resourceStream);
            customLayout.setSizeUndefined();
            rootLayout.addComponent(customLayout);
            rootLayout.setComponentAlignment(customLayout, Alignment.BOTTOM_LEFT);
        } catch (final IOException ex) {
            LOG.error("Footer file cannot be loaded", ex);
        }
    }

    private static Component buildHeader() {
        final CssLayout cssLayout = new CssLayout();
        cssLayout.setStyleName("view-header");
        return cssLayout;
    }

    private void loginAuthenticationFailedNotification() {
        final Notification notification = new Notification(i18n.getMessage("notification.login.failed.title"));
        notification.setDescription(i18n.getMessage("notification.login.failed.description"));
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("error closable");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.setDelayMsec(1_000);
        notification.show(Page.getCurrent());
    }

    private void loginCredentialsExpiredNotification() {
        final Notification notification = new Notification(
                i18n.getMessage("notification.login.failed.credentialsexpired.title"));
        notification.setDescription(i18n.getMessage("notification.login.failed.credentialsexpired.description"));
        notification.setDelayMsec(10_000);
        notification.setHtmlContentAllowed(true);
        notification.setStyleName("error closeable");
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.show(Page.getCurrent());
    }

    private void fillOutUsernameTenantFields() {
        if (tenant != null && params.containsKey(TENANT_PARAMETER) && !params.get(TENANT_PARAMETER).isEmpty()) {
            tenant.setValue(params.get(TENANT_PARAMETER).get(0));
            tenant.setVisible(false);
            useCookie = false;
        }

        if (params.containsKey(USER_PARAMETER) && !params.get(USER_PARAMETER).isEmpty()) {
            username.setValue(params.get(USER_PARAMETER).get(0));
            useCookie = false;
        }
    }

    protected Component buildLoginForm() {

        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setSizeUndefined();
        loginPanel.setSpacing(true);
        loginPanel.addStyleName("login-panel");
        Responsive.makeResponsive(loginPanel);
        loginPanel.addComponent(buildFields());
        if (isDemo) {
            loginPanel.addComponent(buildDisclaimer());
        }
        loginPanel.addComponent(buildLinks());

        checkBrowserSupport(loginPanel);

        return loginPanel;
    }

    protected void checkBrowserSupport(final VerticalLayout loginPanel) {
        // Check if IE browser is not supported ( < IE11 )
        if (isUnsupportedBrowser()) {
            // Disable sign-in button and display a message
            signIn.setEnabled(Boolean.FALSE);
            loginPanel.addComponent(buildUnsupportedMessage());
        }
    }

    protected Component buildFields() {
        final HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("fields");
        buildTenantField();
        buildUserField();
        buildPasswordField();
        buildSignInButton();
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            fields.addComponents(tenant, username, password, signIn);
        } else {
            fields.addComponents(username, password, signIn);
        }
        fields.setComponentAlignment(signIn, Alignment.BOTTOM_LEFT);
        signIn.addClickListener(event -> handleLogin());

        return fields;
    }

    private Component buildDisclaimer() {
        final HorizontalLayout fields = new HorizontalLayout();
        fields.setSpacing(true);
        fields.addStyleName("disclaimer");

        final Label disclaimer = new Label(uiProperties.getDemo().getDisclaimer(), ContentMode.HTML);
        disclaimer.setCaption(i18n.getMessage("label.login.disclaimer"));
        disclaimer.setIcon(FontAwesome.EXCLAMATION_CIRCLE);
        disclaimer.setId("login-disclaimer");
        disclaimer.setWidth("525px");

        fields.addComponent(disclaimer);

        return fields;
    }

    private void handleLogin() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            final boolean textFieldsNotEmpty = hasTenantFieldText() && hasUserFieldText() && hashPasswordFieldText();
            if (textFieldsNotEmpty) {
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
        final String caption = isDemo
                ? i18n.getMessage("button.login.agreeandsignin")
                : i18n.getMessage("button.login.signin");

        signIn = new Button(caption);
        signIn.addStyleName(ValoTheme.BUTTON_PRIMARY + " " + ValoTheme.BUTTON_SMALL + " " + "login-button");
        signIn.setClickShortcut(KeyCode.ENTER);
        signIn.focus();
        signIn.setId("login-signIn");
    }

    private void buildPasswordField() {
        password = new PasswordField(i18n.getMessage("label.login.password"));
        password.setIcon(FontAwesome.LOCK);
        password.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        password.setId("login-password");
        if(isDemo && !uiProperties.getDemo().getPassword().isEmpty()) {
            password.setValue(uiProperties.getDemo().getPassword());
        }
    }

    private void buildUserField() {
        username = new TextField(i18n.getMessage("label.login.username"));
        username.setIcon(FontAwesome.USER);
        username.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        username.setId("login-username");
        if(isDemo && !uiProperties.getDemo().getUser().isEmpty()) {
            username.setValue(uiProperties.getDemo().getUser());
        }
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

    protected Component buildLinks() {

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

    private static boolean isUnsupportedBrowser() {
        final WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
        return webBrowser.isIE() && webBrowser.getBrowserMajorVersion() < 11;
    }

    private void readCookie() {
        if (!useCookie) {
            return;
        }

        final Cookie usernameCookie = getCookieByName(SP_LOGIN_USER);

        if (usernameCookie != null) {
            final String previousUser = usernameCookie.getValue();
            if (isAllowedCookieValue(previousUser)) {
                username.setValue(previousUser);
                password.focus();
            }
        } else {
            username.focus();
        }

        final Cookie tenantCookie = getCookieByName(SP_LOGIN_TENANT);

        if (tenantCookie != null && multiTenancyIndicator.isMultiTenancySupported()) {
            final String previousTenant = tenantCookie.getValue();
            if (isAllowedCookieValue(previousTenant)) {
                tenant.setValue(previousTenant.toUpperCase());
            }
        } else if (multiTenancyIndicator.isMultiTenancySupported()) {
            tenant.focus();
        } else {
            username.focus();
        }
    }

    protected static boolean isAllowedCookieValue(final String previousTenant) {
        return !FORBIDDEN_COOKIE_CONTENT.matcher(previousTenant).matches();
    }

    private void setCookies() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            final Cookie tenantCookie = new Cookie(SP_LOGIN_TENANT, tenant.getValue().toUpperCase());
            tenantCookie.setPath("/");
            // 100 days
            tenantCookie.setMaxAge(HUNDRED_DAYS_IN_SECONDS);
            tenantCookie.setHttpOnly(true);
            tenantCookie.setSecure(uiProperties.getLogin().getCookie().isSecure());
            VaadinService.getCurrentResponse().addCookie(tenantCookie);
        }

        final Cookie usernameCookie = new Cookie(SP_LOGIN_USER, username.getValue());
        usernameCookie.setPath("/");
        // 100 days
        usernameCookie.setMaxAge(HUNDRED_DAYS_IN_SECONDS);
        usernameCookie.setHttpOnly(true);
        usernameCookie.setSecure(uiProperties.getLogin().getCookie().isSecure());
        VaadinService.getCurrentResponse().addCookie(usernameCookie);
    }

    private static Cookie getCookieByName(final String name) {
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

    private void login(final String tenant, final String user, final String password, final boolean setCookies) {
        try {
            if (multiTenancyIndicator.isMultiTenancySupported()) {
                vaadinSecurity.login(new TenantUserPasswordAuthenticationToken(tenant, user, password));
            } else {
                vaadinSecurity.login(new UsernamePasswordAuthenticationToken(user, password));
            }
            /* set success login cookies */
            if (setCookies && useCookie) {
                setCookies();
            }

        } catch (final CredentialsExpiredException e) {
            LOG.debug("Credential expired", e);
            loginCredentialsExpiredNotification();
        } catch (final AuthenticationException e) {
            LOG.debug("Authentication failed", e);
            /* if not successful */
            loginAuthenticationFailedNotification();
        } catch (final Exception e) {
            LOG.debug("Login failed", e);
            loginAuthenticationFailedNotification();
        }
    }

    protected MultiValueMap<String, String> getParams() {
        return params;
    }

    protected TextField getUsername() {
        return username;
    }

    protected TextField getTenant() {
        return tenant;
    }

    protected PasswordField getPassword() {
        return password;
    }

    protected VaadinMessageSource getI18n() {
        return i18n;
    }

}
