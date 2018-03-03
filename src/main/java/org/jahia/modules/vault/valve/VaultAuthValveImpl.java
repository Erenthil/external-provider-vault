//package org.jahia.modules.vault.valve;
//
//import java.util.Locale;
//import java.util.Map;
//import javax.jcr.RepositoryException;
//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpSession;
//import org.jahia.api.Constants;
//import org.jahia.bin.Login;
//import org.jahia.modules.external.ExternalContentStoreProvider;
//import org.jahia.modules.vault.provider.VaultDataSource;
//import org.jahia.params.valves.AuthValveContext;
//import org.jahia.params.valves.AutoRegisteredBaseAuthValve;
//import static org.jahia.params.valves.LoginEngineAuthValveImpl.LOGIN_TAG_PARAMETER;
//import org.jahia.pipelines.PipelineException;
//import org.jahia.pipelines.valves.ValveContext;
//import org.jahia.services.content.JCRNodeWrapper;
//import org.jahia.services.content.JCRStoreProvider;
//import org.jahia.services.content.JCRStoreService;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//public class VaultAuthValveImpl extends AutoRegisteredBaseAuthValve {
//
//    private static final Logger LOGGER = LoggerFactory.getLogger(VaultAuthValveImpl.class);
//    private transient JCRStoreService jcrStoreService;
//
//    @Override
//    public void invoke(Object context, ValveContext valveContext) throws PipelineException {
//        final AuthValveContext authContext = (AuthValveContext) context;
//        final HttpServletRequest httpServletRequest = authContext.getRequest();
//
//        if (isLoginRequested(httpServletRequest)) {
//            final HttpSession session = authContext.getRequest().getSession(false);
//            if (session != null) {
//                final String vaultProviderKey = "test" + "-mount";
//                for (Map.Entry<String, JCRStoreProvider> entry : jcrStoreService.getSessionFactory().getMountPoints().entrySet()) {
//                    if (entry.getValue() instanceof ExternalContentStoreProvider) {
//                        final ExternalContentStoreProvider jcrStoreProvider = (ExternalContentStoreProvider) entry.getValue();
//                        try {
//                            final JCRNodeWrapper node = authContext.getSessionFactory().getCurrentSession(Constants.EDIT_WORKSPACE, Locale.ENGLISH, Locale.ENGLISH, true).getNodeByUUID(jcrStoreProvider.getKey());
//                            if (vaultProviderKey.equals(node.getName())) {
//                                final VaultDataSource dataSource = (VaultDataSource) jcrStoreProvider.getDataSource();
//                                final String username = httpServletRequest.getParameter("fbourasse");
//                                final String password = httpServletRequest.getParameter("password");
//                                final String userToken = dataSource.authenticate(username, password);
//                                if (userToken != null) {
//                                    session.setAttribute(VaultDataSource.ATTR_VAULT_TOKEN, userToken);
//                                }
//                            }
//                        } catch (RepositoryException ex) {
//                            LOGGER.error("Impossible to get mount point node", ex);
//                        }
//
//                    }
//                }
//            }
//        }
//        valveContext.invokeNext(context);
//    }
//
//    public void setJcrStoreService(JCRStoreService jcrStoreService) {
//        this.jcrStoreService = jcrStoreService;
//    }
//
//    protected boolean isLoginRequested(HttpServletRequest request) {
//        String doLogin = request.getParameter(LOGIN_TAG_PARAMETER);
//        if (doLogin != null) {
//            return Boolean.valueOf(doLogin) || "1".equals(doLogin);
//        } else if ("/cms".equals(request.getServletPath())) {
//            return Login.getMapping().equals(request.getPathInfo());
//        }
//
//        return false;
//    }
//}
