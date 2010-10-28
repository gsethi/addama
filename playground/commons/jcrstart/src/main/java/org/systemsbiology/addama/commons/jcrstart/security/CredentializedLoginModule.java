package org.systemsbiology.addama.commons.jcrstart.security;

import org.apache.jackrabbit.core.security.CredentialsCallback;
import org.apache.jackrabbit.core.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;

import javax.jcr.Credentials;
import javax.jcr.SimpleCredentials;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

/**
 * @author hrovira
 */
public class CredentializedLoginModule implements LoginModule {
    private static final Logger log = LoggerFactory.getLogger(CredentializedLoginModule.class);

    private static AuthenticationInfo authenticationInfo;

    // used to load configured username and password from property file
    private String configPropertyFile;
    private String configKeyUsername;
    private String configKeyPassword;

    // used to process authentication requests
    private CallbackHandler callbackHandler;
    private Subject subject;
    private UserPrincipal userPrincipal;


    /*
     * Public Getter/Setters
     */
    public String getConfigPropertyFile() {
        return configPropertyFile;
    }

    public void setConfigPropertyFile(String configPropertyFile) {
        this.configPropertyFile = configPropertyFile;
    }

    public String getConfigKeyUsername() {
        return configKeyUsername;
    }

    public void setConfigKeyUsername(String configKeyUsername) {
        this.configKeyUsername = configKeyUsername;
    }

    public String getConfigKeyPassword() {
        return configKeyPassword;
    }

    public void setConfigKeyPassword(String configKeyPassword) {
        this.configKeyPassword = configKeyPassword;
    }

    /*
     * LoginModule Impls
     */
    public void initialize(Subject subject, CallbackHandler callbackHandler, Map sharedState, Map options) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;

        loadAuthorizedCredentials();
    }

    public boolean login() throws LoginException {
        if (callbackHandler == null) {
            throw new LoginException("no callbackHandler available");
        }

        userPrincipal = null;

        Credentials creds = getCredentials();
        if (creds != null && creds instanceof SimpleCredentials) {
            SimpleCredentials sc = (SimpleCredentials) creds;

            if (authenticationInfo.matches(sc)) {
                if (log.isDebugEnabled()) {
                    log.debug("authorized passed");
                }

                userPrincipal = new UserPrincipal(sc.getUserID());
                return true;
            }
        }

        log.warn("authentication failed");

        userPrincipal = null;
        throw new FailedLoginException();
    }

    public boolean commit() throws LoginException {
        if (userPrincipal != null) {
            subject.getPrincipals().add(userPrincipal);
            return true;
        }
        return false;
    }

    public boolean abort() throws LoginException {
        return logout();
    }

    public boolean logout() throws LoginException {
        subject.getPrincipals().remove(userPrincipal);
        userPrincipal = null;
        return true;
    }

    /*
     * Private Methods
     */
    private void loadAuthorizedCredentials() {
        try {
            if (authenticationInfo == null) {
                FileSystemResource resource = new FileSystemResource(configPropertyFile);
                log.info("loadAuthorizedCredentials(): resource=" + resource);

                Properties properties = new Properties();
                properties.load(resource.getInputStream());
                String user = properties.getProperty(configKeyUsername);
                String pswd = properties.getProperty(configKeyPassword);

                authenticationInfo = new AuthenticationInfo(user, pswd);
                if (log.isDebugEnabled()) {
                    log.debug("loadAuthorizedCredentials(): authenticationInfo=" + authenticationInfo);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private Credentials getCredentials() throws LoginException {
        try {
            CredentialsCallback ccb = new CredentialsCallback();
            callbackHandler.handle(new Callback[]{ccb});
            return ccb.getCredentials();
        } catch (IOException ioe) {
            throw new LoginException(ioe.toString());
        } catch (UnsupportedCallbackException uce) {
            throw new LoginException(uce.getCallback().toString() + " not available");
        }
    }

    /*
     * Private Class
     */
    private class AuthenticationInfo {
        private final String user;
        private final String password;

        private AuthenticationInfo(String user, String password) {
            this.user = user;
            this.password = password;
        }

        public boolean matches(SimpleCredentials sc) {
            String scUser = sc.getUserID();
            String scPswd = new String(sc.getPassword());

            return user.equalsIgnoreCase(scUser) && password.equals(scPswd);
        }
    }
}
