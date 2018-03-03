package org.jahia.modules.vault.listeners;

import java.util.Locale;
import java.util.Map;
import javax.jcr.RepositoryException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.jahia.api.Constants;
import org.jahia.bin.Logout;
import org.jahia.modules.external.ExternalContentStoreProvider;
import org.jahia.modules.vault.provider.VaultDataSource;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.BaseLoginEvent;
import org.jahia.services.content.JCRNodeWrapper;
import org.jahia.services.content.JCRStoreProvider;
import org.jahia.services.content.JCRStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class VaultApplicationListener implements ApplicationListener<ApplicationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VaultApplicationListener.class);
    private transient JCRStoreService jcrStoreService;
    public static ThreadLocal myThreadLocal = new ThreadLocal();

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof BaseLoginEvent) {
            handleLoginEvent((BaseLoginEvent) applicationEvent);
        } else if (applicationEvent instanceof Logout.LogoutEvent) {
            handleLogoutEvent((Logout.LogoutEvent) applicationEvent);
        }
    }

    public void setJcrStoreService(JCRStoreService jcrStoreService) {
        this.jcrStoreService = jcrStoreService;
    }

    private void handleLoginEvent(BaseLoginEvent loginEvent) {

        final AuthValveContext authContext = loginEvent.getAuthValveContext();
        final HttpServletRequest httpServletRequest = authContext.getRequest();

        final HttpSession session = httpServletRequest.getSession();
        if (session != null) {
            // TODO: get vault edp to mount from the site key of the login event
            final String vaultProviderKey = "test" + "-mount";
            for (Map.Entry<String, JCRStoreProvider> entry : jcrStoreService.getSessionFactory().getMountPoints().entrySet()) {
                if (entry.getValue() instanceof ExternalContentStoreProvider && entry.getKey().startsWith("/mounts")) {
                    final ExternalContentStoreProvider jcrStoreProvider = (ExternalContentStoreProvider) entry.getValue();

                    try {
                        final JCRNodeWrapper node = authContext.getSessionFactory().getCurrentSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH, true).getNodeByUUID(jcrStoreProvider.getKey());
                        if (vaultProviderKey.equals(node.getName())) {
                            final VaultDataSource dataSource = (VaultDataSource) jcrStoreProvider.getDataSource();
                            final String username = httpServletRequest.getParameter("username");
                            final String password = httpServletRequest.getParameter("password");
                            final String userToken = dataSource.authenticate(username, password);
                            if (userToken != null) {
                                final Cookie vaultUserTokenCookie = new Cookie(VaultDataSource.ATTR_VAULT_TOKEN, userToken);
                                vaultUserTokenCookie.setPath("/");
                                vaultUserTokenCookie.setMaxAge(-1);
                                authContext.getResponse().addCookie(vaultUserTokenCookie);
                            }
                        }
                    } catch (RepositoryException ex) {
                        LOGGER.error("Impossible to get mount point node", ex);
                    }

                }
            }
        }
    }

    private void handleLogoutEvent(Logout.LogoutEvent logoutEvent) {
        final Cookie vaultUserTokenCookie = new Cookie(VaultDataSource.ATTR_VAULT_TOKEN, "");
        vaultUserTokenCookie.setPath("/");
        vaultUserTokenCookie.setMaxAge(0);
        logoutEvent.getResponse().addCookie(vaultUserTokenCookie);
    }
}
