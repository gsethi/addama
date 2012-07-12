package org.systemsbiology.addama.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.CredentialStore;
import com.google.api.client.auth.oauth2.MemoryCredentialStore;
import com.google.api.client.googleapis.auth.oauth2.*;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.oauth2.Oauth2;
import com.google.api.services.oauth2.model.Userinfo;
import org.springframework.core.io.ClassPathResource;
import org.systemsbiology.addama.commons.web.exceptions.ForbiddenAccessException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

import static com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import static com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.load;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.commons.lang.StringUtils.substringAfter;

/**
 * @author hrovira
 */
public class CredentialMediator {
    private static final List<String> SCOPES = asList(
            // Required to access and manipulate files.
            "https://www.googleapis.com/auth/drive.file",
            // Required to identify the user in our data store.
            "https://www.googleapis.com/auth/userinfo.email",
            "https://www.googleapis.com/auth/userinfo.profile");

    private final HttpServletRequest request;
    private final GoogleClientSecrets secrets;
    private static final CredentialStore credentialStore = new MemoryCredentialStore();

    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final HttpTransport TRANSPORT = new NetHttpTransport();
    private static final String USER_ID_KEY = "userId";
    private static final String EMAIL_KEY = "emailAddress";

    public CredentialMediator(HttpServletRequest request) throws IOException {
        this.request = request;
        String configPath = getConfigPath(request);
        this.secrets = load(JSON_FACTORY, new ClassPathResource(configPath).getInputStream());
    }

    public Drive getDriveService() throws NoRefreshTokenException, ForbiddenAccessException, IOException {
        Credential credentials = getActiveCredential();
        return new Drive.Builder(TRANSPORT, JSON_FACTORY, credentials).build();
    }

    public void deleteCredentials() throws IOException {
        String userId = getUserId();
        if (!isEmpty(userId)) {
            Credential credential = getStoredCredential(userId);
            credentialStore.delete(userId, credential);
        }
    }

    /**
     * Retrieve credentials using the provided authorization code.
     * <p/>
     * This function exchanges the authorization code for an access token and
     * queries the UserInfo API to retrieve the user's e-mail address. If a
     * refresh token has been retrieved along with an access token, it is stored
     * in the application database using the user's e-mail address as key. If no
     * refresh token has been retrieved, the function checks in the application
     * database for one and returns it if found or throws a
     * NoRefreshTokenException with the authorization URL to redirect the user
     * to.
     *
     * @return Credential containing an access and refresh token.
     * @throws NoRefreshTokenException No refresh token could be retrieved from the available sources.
     */
    public Credential getActiveCredential() throws NoRefreshTokenException, IOException, ForbiddenAccessException {
        String userId = getUserId();
        Credential credential = getStoredCredential(userId);

        if (credential == null || isEmpty(credential.getRefreshToken())) {
            // No refresh token has been retrieved. Start a "fresh" OAuth 2.0 flow so that we can get a refresh token.
            String email = (String) request.getSession().getAttribute(EMAIL_KEY);
            String authorizationUrl = getAuthorizationUrl(email);
            throw new NoRefreshTokenException(authorizationUrl);
        }

        return credential;
    }

    public Userinfo getActiveUserinfo() throws NoRefreshTokenException, ForbiddenAccessException, IOException {
        return getUserInfo(getActiveCredential());
    }

    public void storeCallbackCode(String code) throws IOException, ForbiddenAccessException {
        Credential credential = exchangeCode(code);
        if (credential != null) {
            Userinfo userInfo = getUserInfo(credential);
            String userId = userInfo.getId();
            request.getSession().setAttribute(USER_ID_KEY, userId);
            request.getSession().setAttribute(EMAIL_KEY, userInfo.getEmail());
            if (!isEmpty(credential.getRefreshToken())) {
                credentialStore.store(userId, credential);
            }
        }
    }

    public String getUserId() {
        return (String) request.getSession().getAttribute(USER_ID_KEY);
    }

    public String getClientId() {
        return secrets.getWeb().getClientId();
    }

    /**
     * Retrieve the authorization URL to authorize the user with the given email address.
     *
     * @param emailAddress User's e-mail address.
     * @return Authorization URL to redirect the user to.
     */
    private String getAuthorizationUrl(String emailAddress) {
        Details details = secrets.getWeb();
        GoogleAuthorizationCodeRequestUrl urlBuilder = new GoogleAuthorizationCodeRequestUrl(details.getClientId(),
                details.getRedirectUris().get(0), SCOPES).setAccessType("offline").setApprovalPrompt("force");
        if (!isEmpty(emailAddress)) urlBuilder.set("user_id", emailAddress);
        return urlBuilder.build();
    }

    /**
     * Retrieves stored credentials for the provided email address.
     *
     * @param userId User's Google ID.
     * @return Stored GoogleCredential if found, {@code null} otherwise.
     */
    private Credential getStoredCredential(String userId) throws IOException {
        if (!isEmpty(userId)) {
            Credential credential = buildEmptyCredential();
            if (credentialStore.load(userId, credential)) {
                return credential;
            }
        }
        return null;
    }

    /**
     * Send a request to the UserInfo API to retrieve user e-mail address
     * associated with the given credential.
     *
     * @param credential Credential to authorize the request.
     * @return User's e-mail address.
     * @throws ForbiddenAccessException An error occurred, and the retrieved email address was null.
     * @throws IOException
     */
    private Userinfo getUserInfo(Credential credential) throws ForbiddenAccessException, IOException {
        Oauth2 userInfoService = new Oauth2.Builder(TRANSPORT, JSON_FACTORY, credential).build();
        Userinfo userInfo = userInfoService.userinfo().get().execute();
        if (userInfo == null) throw new ForbiddenAccessException();
        return userInfo;
    }

    /**
     * Exchange an authorization code for a credential.
     *
     * @param authorizationCode Authorization code to exchange for OAuth 2.0
     *                          credentials.
     * @return Credential representing the upgraded authorizationCode.
     * @throws IOException
     */
    private Credential exchangeCode(String authorizationCode) throws IOException {
        // Talk to Google and upgrade the given authorization code to an access token and hopefully a refresh token.
        Details details = secrets.getWeb();
        GoogleTokenResponse response = new GoogleAuthorizationCodeTokenRequest(TRANSPORT, JSON_FACTORY,
                details.getClientId(), details.getClientSecret(), authorizationCode,
                details.getRedirectUris().get(0)).execute();
        return buildEmptyCredential().setFromTokenResponse(response);
    }

    /**
     * Builds an empty GoogleCredential, configured with appropriate
     * HttpTransport, JsonFactory, and client information.
     */
    private Credential buildEmptyCredential() {
        return new GoogleCredential.Builder().setClientSecrets(this.secrets)
                .setTransport(TRANSPORT).setJsonFactory(JSON_FACTORY).build();
    }

    private String getConfigPath(HttpServletRequest request) {
        String contextPath = request.getSession().getServletContext().getContextPath();
        if (contextPath.startsWith("/")) {
            contextPath = substringAfter(contextPath, "/");
        }
        return "services/" + contextPath + ".config";
    }

    public static class NoRefreshTokenException extends Exception {
        private final String authorizationUrl;

        public NoRefreshTokenException(String authorizationUrl) {
            this.authorizationUrl = authorizationUrl;
        }

        public String getAuthorizationUrl() {
            return authorizationUrl;
        }
    }
}
