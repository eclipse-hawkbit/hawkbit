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

import org.eclipse.hawkbit.im.authentication.MultitenancyIndicator;
import org.eclipse.hawkbit.im.authentication.TenantUserPasswordAuthenticationToken;
import org.eclipse.hawkbit.ui.AbstractHawkbitUI;
import org.eclipse.hawkbit.ui.UiProperties;
import org.eclipse.hawkbit.ui.common.data.proxies.ProxyLoginCredentials;
import org.eclipse.hawkbit.ui.common.notification.ParallelNotification;
import org.eclipse.hawkbit.ui.components.SPUIComponentProvider;
import org.eclipse.hawkbit.ui.themes.HawkbitTheme;
import org.eclipse.hawkbit.ui.utils.HawkbitCommonUtil;
import org.eclipse.hawkbit.ui.utils.SPUIStyleDefinitions;
import org.eclipse.hawkbit.ui.utils.SpringContextHelper;
import org.eclipse.hawkbit.ui.utils.UIComponentIdProvider;
import org.eclipse.hawkbit.ui.utils.UINotification;
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
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import org.vaadin.spring.security.VaadinSecurity;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.Title;
import com.vaadin.annotations.Widgetset;
import com.vaadin.data.Binder;
import com.vaadin.event.ShortcutAction.KeyCode;
import com.vaadin.icons.VaadinIcons;
import com.vaadin.server.Page;
import com.vaadin.server.Responsive;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.WebBrowser;
import com.vaadin.shared.Position;
import com.vaadin.shared.ui.ContentMode;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.Component;
import com.vaadin.ui.CssLayout;
import com.vaadin.ui.CustomLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Link;
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
    private static final String LOGIN_TEXTFIELD = "login-textfield";

    private final transient ApplicationContext context;

    private final transient VaadinSecurity vaadinSecurity;

    private final VaadinMessageSource i18n;

    private final UiProperties uiProperties;

    private final transient MultitenancyIndicator multiTenancyIndicator;

    private final boolean isDemo;

    private TextField username;
    private TextField tenant;
    private PasswordField password;
    private Button signIn;

    private MultiValueMap<String, String> params;

    private final Binder<ProxyLoginCredentials> binder;

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
        this.binder = new Binder<>();
    }

    @Override
    protected void init(final VaadinRequest request) {
        HawkbitCommonUtil.initLocalization(this, uiProperties.getLocalization(), i18n);
        SpringContextHelper.setContext(context);

        params = UriComponentsBuilder.fromUri(Page.getCurrent().getLocation()).build().getQueryParams();

        setContent(buildContent());

        final ProxyLoginCredentials credentialsBean = new ProxyLoginCredentials();
        populateCredentials(credentialsBean);

        binder.addStatusChangeListener(event -> signIn.setEnabled(event.getBinder().isValid()));
        binder.setBean(credentialsBean);

    }

    private VerticalLayout buildContent() {
        final VerticalLayout rootLayout = new VerticalLayout();
        rootLayout.setSpacing(false);
        rootLayout.setMargin(false);
        rootLayout.setSizeFull();
        rootLayout.setStyleName("main-content");

        rootLayout.addComponent(buildHeader());

        addLoginForm(rootLayout);

        addFooter(rootLayout);

        return rootLayout;
    }

    private static Component buildHeader() {
        final CssLayout cssLayout = new CssLayout();
        cssLayout.setStyleName("view-header");
        return cssLayout;
    }

    private void addLoginForm(final VerticalLayout rootLayout) {
        final Component loginForm = buildLoginForm();
        rootLayout.addComponent(loginForm);
        rootLayout.setComponentAlignment(loginForm, Alignment.MIDDLE_CENTER);
    }

    protected Component buildLoginForm() {
        final VerticalLayout loginPanel = new VerticalLayout();
        loginPanel.setMargin(false);
        loginPanel.setSpacing(true);
        loginPanel.setSizeUndefined();
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

    protected Component buildFields() {
        final HorizontalLayout fields = new HorizontalLayout();
        fields.setMargin(false);
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

        return fields;
    }

    private void buildTenantField() {
        if (multiTenancyIndicator.isMultiTenancySupported()) {
            tenant = new TextField(i18n.getMessage("label.login.tenant"));
            tenant.setIcon(VaadinIcons.DATABASE);
            tenant.addStyleName(
                    ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
            tenant.addStyleName("uppercase");
            tenant.setId("login-tenant");

            binder.forField(tenant).asRequired().bind(ProxyLoginCredentials::getTenant,
                    ProxyLoginCredentials::setTenant);
        }
    }

    private void buildUserField() {
        username = new TextField(i18n.getMessage("label.login.username"));
        username.setIcon(VaadinIcons.USER);
        username.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        username.setId("login-username");

        binder.forField(username).asRequired().bind(ProxyLoginCredentials::getUsername,
                ProxyLoginCredentials::setUsername);
    }

    private void buildPasswordField() {
        password = new PasswordField(i18n.getMessage("label.login.password"));
        password.setIcon(VaadinIcons.LOCK);
        password.addStyleName(
                ValoTheme.TEXTFIELD_INLINE_ICON + " " + ValoTheme.TEXTFIELD_SMALL + " " + LOGIN_TEXTFIELD);
        password.setId("login-password");

        binder.forField(password).asRequired().bind(ProxyLoginCredentials::getPassword,
                ProxyLoginCredentials::setPassword);
    }

    private void buildSignInButton() {
        final String caption = isDemo ? i18n.getMessage("button.login.agreeandsignin")
                : i18n.getMessage("button.login.signin");

        signIn = new Button(caption);
        signIn.addStyleName(ValoTheme.BUTTON_PRIMARY + " " + ValoTheme.BUTTON_SMALL + " " + "login-button");
        signIn.setClickShortcut(KeyCode.ENTER);
        signIn.focus();
        signIn.setId("login-signin");

        signIn.addClickListener(event -> handleLogin());
    }

    private void handleLogin() {
        final ProxyLoginCredentials credentialsBean = binder.getBean();
        final String providedTenant = credentialsBean.getTenant();
        final String providedUsername = credentialsBean.getUsername();
        final String providedPassword = credentialsBean.getPassword();

        if (multiTenancyIndicator.isMultiTenancySupported()) {
            if (!StringUtils.isEmpty(providedTenant) && !StringUtils.isEmpty(providedUsername)
                    && !StringUtils.isEmpty(providedPassword)) {
                login(providedTenant, providedUsername, providedPassword);
            }
        } else if (!multiTenancyIndicator.isMultiTenancySupported() && !StringUtils.isEmpty(providedUsername)
                && !StringUtils.isEmpty(providedPassword)) {
            login(null, providedUsername, providedPassword);
        }
    }

    private void login(final String tenant, final String user, final String password) {
        try {
            if (multiTenancyIndicator.isMultiTenancySupported()) {
                vaadinSecurity.login(new TenantUserPasswordAuthenticationToken(tenant, user, password));
            } else {
                vaadinSecurity.login(new UsernamePasswordAuthenticationToken(user, password));
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

    private void loginCredentialsExpiredNotification() {
        showErrorNotification(i18n.getMessage("notification.login.failed.credentialsexpired.title"),
                i18n.getMessage("notification.login.failed.credentialsexpired.description"));
    }

    private void showErrorNotification(final String caption, final String description) {
        final ParallelNotification notification = UINotification.buildNotification(
                SPUIStyleDefinitions.SP_NOTIFICATION_ERROR_MESSAGE_STYLE, caption, description, null, true);
        notification.setPosition(Position.BOTTOM_CENTER);
        notification.show(Page.getCurrent());
    }

    private void loginAuthenticationFailedNotification() {
        showErrorNotification(i18n.getMessage("notification.login.failed.title"),
                i18n.getMessage("notification.login.failed.description"));
    }

    private Component buildDisclaimer() {
        final HorizontalLayout fields = new HorizontalLayout();
        fields.setMargin(false);
        fields.setSpacing(true);
        fields.addStyleName("disclaimer");

        final Label disclaimer = new Label(uiProperties.getDemo().getDisclaimer(), ContentMode.HTML);
        disclaimer.setCaption(i18n.getMessage("label.login.disclaimer"));
        disclaimer.setIcon(VaadinIcons.EXCLAMATION_CIRCLE);
        disclaimer.setId("login-disclaimer");
        disclaimer.setWidth("525px");

        fields.addComponent(disclaimer);

        return fields;
    }

    protected Component buildLinks() {
        final HorizontalLayout links = new HorizontalLayout();
        links.setMargin(false);
        links.setSpacing(true);
        links.addStyleName("links");
        final String linkStyle = "v-link";

        if (!uiProperties.getLinks().getDocumentation().getRoot().isEmpty()) {
            final Link docuLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_DOCUMENTATION,
                    i18n.getMessage("link.documentation.name"), uiProperties.getLinks().getDocumentation().getRoot(),
                    VaadinIcons.QUESTION_CIRCLE, "_blank", linkStyle);
            links.addComponent(docuLink);
            docuLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        if (!uiProperties.getLinks().getRequestAccount().isEmpty()) {
            final Link requestAccountLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_REQUESTACCOUNT,
                    i18n.getMessage("link.requestaccount.name"), uiProperties.getLinks().getRequestAccount(),
                    VaadinIcons.CART, "", linkStyle);
            links.addComponent(requestAccountLink);
            requestAccountLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        if (!uiProperties.getLinks().getUserManagement().isEmpty()) {
            final Link userManagementLink = SPUIComponentProvider.getLink(UIComponentIdProvider.LINK_USERMANAGEMENT,
                    i18n.getMessage("link.usermanagement.name"), uiProperties.getLinks().getUserManagement(),
                    VaadinIcons.USERS, "_blank", linkStyle);
            links.addComponent(userManagementLink);
            userManagementLink.addStyleName(ValoTheme.LINK_SMALL);
        }

        return links;
    }

    protected void checkBrowserSupport(final VerticalLayout loginPanel) {
        // Check if IE browser is not supported ( < IE11 )
        if (isUnsupportedBrowser()) {
            // Disable sign-in button and display a message
            signIn.setEnabled(Boolean.FALSE);
            loginPanel.addComponent(buildUnsupportedMessage());
        }
    }

    private static boolean isUnsupportedBrowser() {
        final WebBrowser webBrowser = Page.getCurrent().getWebBrowser();
        return webBrowser.isIE() && webBrowser.getBrowserMajorVersion() < 11;
    }

    private Component buildUnsupportedMessage() {
        final Label label = new Label(i18n.getMessage("label.unsupported.browser.ie"));
        label.addStyleName(ValoTheme.LABEL_FAILURE);
        return label;
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

    private void populateCredentials(final ProxyLoginCredentials credentialsBean) {
        if (tenant != null && params.containsKey(TENANT_PARAMETER) && !params.get(TENANT_PARAMETER).isEmpty()) {
            credentialsBean.setTenant(params.get(TENANT_PARAMETER).get(0));
            tenant.setVisible(false);
        }

        if (params.containsKey(USER_PARAMETER) && !params.get(USER_PARAMETER).isEmpty()) {
            credentialsBean.setUsername(params.get(USER_PARAMETER).get(0));
        } else if (isDemo && !uiProperties.getDemo().getUser().isEmpty()) {
            credentialsBean.setUsername(uiProperties.getDemo().getUser());
        }

        if (isDemo && !uiProperties.getDemo().getPassword().isEmpty()) {
            credentialsBean.setPassword(uiProperties.getDemo().getPassword());
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
