/*
 * This file is part of the Goobi viewer - a content presentation and management
 * application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.omnifaces.cdi.Push;
import org.omnifaces.cdi.PushContext;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.filters.LoginFilter;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.annotation.comments.CommentManager;
import io.goobi.viewer.model.crowdsourcing.CrowdsourcingTools;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.security.authentication.HttpAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LoginResult;
import io.goobi.viewer.model.security.authentication.OpenIdProvider;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.transkribus.TranskribusUtils;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.servlets.utils.ServletUtils;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

/**
 * JSF session-scoped backing bean responsible for user authentication, registration, and account
 * management. Holds the currently logged-in {@link io.goobi.viewer.model.security.user.User}
 * and mediates login/logout across local and OpenID Connect providers.
 *
 * <p><b>Lifecycle:</b> Created once per HTTP session; destroyed when the session expires or the
 * user explicitly logs out. Sensitive credential fields ({@code password}, {@code passwordOne},
 * {@code passwordTwo}) are declared {@code transient} and are therefore not included in session
 * serialisation.
 *
 * <p><b>Thread safety:</b> Mostly confined to the JSF request thread. The
 * {@code getAuthenticationProviders()} method is {@code synchronized} to prevent concurrent
 * lazy initialisation of the provider list.
 */
@Named
@SessionScoped
public class UserBean implements Serializable {

    private static final long serialVersionUID = 5917173704087714181L;

    private static final Logger logger = LogManager.getLogger(UserBean.class);

    @Inject
    private CaptchaBean captchaBean;
    @Inject
    private NavigationHelper navigationHelper;

    @Inject
    @Push
    private PushContext sessionTimeoutCounter;

    /**
     * The logged in user.
     */
    private User user;
    private String nickName;
    private String email;
    private transient String password;
    private String activationKey;
    /** Selected OpenID Connect provider. */
    private IAuthenticationProvider authenticationProvider;
    private IAuthenticationProvider loggedInProvider;

    // Passwords for creating an new local user account
    private transient String passwordOne = "";
    private transient String passwordTwo = "";

    /** Honey pot field invisible to human users. */
    private String lastName;
    /** Redirect URL after successful login. */
    private String redirectUrl = null;
    /** External origin, e.g. IIIF Auth client. */
    private String origin = null;
    private String transkribusUserName;
    private String transkribusPassword;
    private Boolean hasAdminBackendAccess;
    private HttpSession session;

    /**
     * Empty constructor.
     */
    public UserBean() {
        logger.trace("User bean instatiated: {}", this);
        this.authenticationProvider = getLocalAuthenticationProvider();
        this.session = BeanUtils.getSession();
    }

    /**
     * updateSessionTimeoutCounter.
     */
    public void updateSessionTimeoutCounter() {
        logger.trace("updateSessionTimeoutCounter");
        sessionTimeoutCounter.send("update");
    }

    /**
     * getSessionTimeout.
     *
     * @return the remaining session timeout formatted as an ISO time string
     */
    public String getSessionTimeout() {
        long lastActityTimestamp = BeanUtils.getSession().getLastAccessedTime();
        logger.trace("lastActityTimestamp: {}", lastActityTimestamp);
        long inactiveMillis = System.currentTimeMillis() - lastActityTimestamp;
        logger.trace("inactiveMillis: {}", inactiveMillis);
        int maxInactiveSeconds = BeanUtils.getSession().getMaxInactiveInterval();
        // logger.trace("maxInactiveSeconds: {}", maxInactiveSeconds); //NOSONAR Debug
        long timeoutMillis = maxInactiveSeconds * 1000 - inactiveMillis;
        logger.trace("timeoutMillis: {}", timeoutMillis);
        LocalTime timeout = LocalTime.ofSecondOfDay(timeoutMillis / 1000);
        return DateTimeFormatter.ISO_TIME.format(timeout);
    }

    /**
     * Creates and persists a new local User.
     *
     * @return an empty string after attempting to register the new user account
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String createNewUserAccount() throws DAOException {
        // Check whether user account registration is enabled
        if (!DataManager.getInstance().getConfiguration().isUserRegistrationEnabled()) {
            logFailedUserRegistration();
            logger.debug("User registration is disabled.");
            return "";
        }
        // Check whether the security question has been answered correct, if configured
        if (captchaBean != null && !captchaBean.checkAnswer()) {
            Messages.error("user__security_question_wrong");
            logFailedUserRegistration();
            logger.debug("Wrong security question answer.");
            return "";

        }

        // Check whether the invisible field lastName has been filled (real users cannot do that)
        if (StringUtils.isNotEmpty(lastName)) {
            logFailedUserRegistration();
            logger.debug("Honeypot field entry: {}", lastName);
            return "";
        }
        // Check for existing nicknames
        if (nickName != null && DataManager.getInstance().getDao().getUserByNickname(nickName) != null) {
            // Do not allow the same nickname being used for multiple users
            Messages.error(ViewerResourceBundle.getTranslation("user_nicknameTaken", null).replace("{0}", nickName.trim()));
            logFailedUserRegistration();
            logger.debug("User account already exists for nickname '{}'.", nickName);
            return "";
        }
        // Check for existing e-mail addresses
        if (DataManager.getInstance().getDao().getUserByEmail(email) != null) {
            // Do not allow the same email address being used for multiple users
            Messages.error("newUserExist");
            logFailedUserRegistration();
            if (logger.isDebugEnabled()) {
                logger.debug("User account already exists for e-mail address '{}'.", NetTools.scrambleEmailAddress(email));
            }
            return "";
        }

        if (StringUtils.isNotBlank(passwordOne) && passwordOne.equals(passwordTwo)) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNickName(nickName);
            newUser.setNewPassword(passwordOne);
            resetPasswordFields();
            if (sendActivationEmail(newUser)) {
                // Only attempt to persist the new user if the activation email could be sent
                if (DataManager.getInstance().getDao().addUser(newUser)) {
                    String msg = ViewerResourceBundle.getTranslation("user_accountCreated", null);
                    Messages.info(msg.replace("{0}", email));
                    logger.debug("User account created for '{}'.", email);
                } else {
                    Messages.error("errSave");
                }
                return "user?faces-redirect=true";
            }
            logFailedUserRegistration();
            logger.debug("E-mail could not be sent");
            Messages.error(ViewerResourceBundle.getTranslation("errSendEmail", null)
                    .replace("{0}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress()));
        } else {
            Messages.error("user_passwordMismatch");
        }

        return "";
    }

    /**
     * Logs failed user registration attempts with scrambled e-mail and IP addresses.
     */
    private void logFailedUserRegistration() {
        String ipAddress = "UNKNOWN";
        HttpServletRequest request = BeanUtils.getRequest();
        if (request != null) {
            ipAddress = NetTools.getIpAddress(request);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Failed user registration attempt from {}, e-mail '{}'", NetTools.scrambleIpAddress(ipAddress),
                    NetTools.scrambleEmailAddress(email));
        }
    }

    /**
     * activateUserAccountAction.
     *
     * @return the navigation outcome after attempting to activate the user account via email and key
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String activateUserAccountAction() throws DAOException {
        if (StringUtils.isNotEmpty(email) && StringUtils.isNotEmpty(activationKey)) {
            User u = DataManager.getInstance().getDao().getUserByEmail(email);
            if (u != null && !u.isActive()) {
                if (activationKey.equals(u.getActivationKey())) {
                    // Activate user
                    u.setActivationKey(null);
                    u.setActive(true);
                    if (DataManager.getInstance().getDao().updateUser(u)) {
                        Messages.info(ViewerResourceBundle.getTranslation("user_accountActivationSuccess", null));
                        logger.debug("User account successfully activated: {}", u.getEmail());
                    } else {
                        Messages.error(ViewerResourceBundle.getTranslation("errSave", null));
                    }
                } else {
                    logger.debug("Activation key mismatch (expected: '{}' (received: '{}').", u.getActivationKey(), activationKey);
                    Messages.error(ViewerResourceBundle.getTranslation("user_accountActivationWrongData", null));
                }
            } else {
                logger.debug("User not found or account already activated: {}", email);
            }
            activationKey = null;
        }

        return "user?faces-redirect=true";
    }

    /**
     * Login action method for local accounts.
     *
     * @return the url mapping to navigate to
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     * @throws java.lang.IllegalStateException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.util.concurrent.ExecutionException if any.
     * @should return null for invalid input
     */
    public String login() throws AuthenticationProviderException, IllegalStateException, InterruptedException, ExecutionException {
        return login(getAuthenticationProvider());
    }

    /**
     * login.
     *
     * @param provider authentication provider to use for the login attempt
     * @return the navigation outcome or redirect URL after the login attempt
     * @throws java.lang.IllegalStateException if any.
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.util.concurrent.ExecutionException if any.
     */
    public String login(IAuthenticationProvider provider)
            throws AuthenticationProviderException, IllegalStateException, InterruptedException, ExecutionException {
        if (provider instanceof OpenIdProvider) {
            // Make sure the current URL is added to the provider so the redirection endpoint can redirect there
            this.redirectUrl = "#";
        }
        if ("#".equals(this.redirectUrl)) {
            this.redirectUrl = buildRedirectUrl();
        }
        logger.trace("login: {}", this);
        if (provider != null) {
            try {
                // Set provider so it can be accessed from outsde
                setAuthenticationProvider(provider);
                if (redirectUrl == null && provider instanceof HttpHeaderProvider) {
                    this.redirectUrl = buildRedirectUrl();
                }
                provider.setRedirectUrl(this.redirectUrl);
                provider.login(email, password).thenAccept(result -> completeLogin(provider, result));
            } finally {
                // Reset provider to local so that all fields are displayed after a faild non-local login
                setAuthenticationProvider(getLocalAuthenticationProvider());
            }
        }

        return null;
    }

    static String buildRedirectUrl() {
        HttpServletRequest request = BeanUtils.getRequest();
        return ViewHistory.getCurrentView(request)
                .map(path -> ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + path.getCombinedPrettyfiedUrl())
                .orElse("");
    }

    /**
     *
     * @param provider Authentication provider that performed the login
     * @param result Result object containing user and request/response data
     * @throws IllegalStateException
     */
    private void completeLogin(IAuthenticationProvider provider, LoginResult result) {
        logger.debug("completeLogin: {}", this);
        // Results from a redirection endpoint will contain the wrong request/response objects
        HttpServletResponse response = provider instanceof HttpAuthenticationProvider ? BeanUtils.getResponse() : result.getResponse();
        HttpServletRequest request = provider instanceof HttpAuthenticationProvider ? BeanUtils.getRequest() : result.getRequest();
        // HttpSession session = request != null ? request.getSession(false) : null;
        try {
            Optional<User> oUser = result.getUser().filter(u -> u.isActive() && !u.isSuspended());
            if (result.isRefused()) {
                if (result.getDelay() > 0) {
                    String msg =
                            ViewerResourceBundle.getTranslation("errLoginDelay", navigationHelper != null ? navigationHelper.getLocale() : null)
                                    .replace("{0}", String.valueOf((int) Math.ceil(result.getDelay() / 1000.0)));
                    Messages.error(msg);
                } else {
                    Messages.error("errLoginWrong");
                }
            } else if (result.getUser().map(u -> !u.isActive()).orElse(false)) {
                Messages.error("errLoginWrong");
            } else if (result.getUser().map(User::isSuspended).orElse(false)) {
                Messages.error("errLoginWrong");
            } else if (oUser.isPresent()) { //login successful
                try {
                    User u = oUser.get();
                    if (this.user != null) {
                        if (this.user.equals(u)) {
                            logger.debug("User already logged in");
                            if (isCloseTabAfterLogin()) {
                                logger.debug("Closing login tab...");
                                doRedirect(response, DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "logincomplete/");
                            }
                            return;
                        }
                        // Exception if different user logged in
                        throw new AuthenticationProviderException("errLoginError");
                    }

                    BeanUtils.wipeSessionAttributes(session);
                    DataManager.getInstance().getBookmarkManager().addSessionBookmarkListToUser(u, request);
                    // Update last login
                    u.setLastLogin(LocalDateTime.now());
                    if (!DataManager.getInstance().getDao().updateUser(u)) {
                        logger.error("Could not update user in DB.");
                    }
                    setUser(u);
                    if (session != null) {
                        session.setAttribute("user", u);
                        logger.trace("Added user to HTTP session ID {}: {}", session.getId(), u.getId());
                    }

                    if (response != null) {
                        if (isCloseTabAfterLogin()) {
                            logger.debug("Closing login tab...");
                            doRedirect(response, DataManager.getInstance().getConfiguration().getViewerBaseUrl() + "logincomplete/");
                        } else if (StringUtils.isNotEmpty(redirectUrl)) {
                            logger.debug("Redirecting to {}", redirectUrl);
                            String url = this.redirectUrl;
                            this.redirectUrl = "";
                            doRedirect(response, url);
                        } else {
                            Optional<ViewerPath> currentPath = ViewHistory.getCurrentView(request);
                            if (currentPath.isPresent()) {
                                logger.trace("Redirecting to current URL: {}", currentPath.get().getCombinedPrettyfiedUrl());
                                doRedirect(response, ServletUtils.getServletPathWithHostAsUrlFromRequest(request)
                                        + currentPath.get().getCombinedPrettyfiedUrl());
                            } else {
                                logger.trace("Redirecting to start page");
                                doRedirect(response, ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
                            }
                        }
                    }

                    // Update personal filter query suffix
                    SearchHelper.updateFilterQuerySuffix(request, IPrivilegeHolder.PRIV_LIST);

                    // Add this user to configured groups
                    if (provider.getAddUserToGroups() != null && !provider.getAddUserToGroups().isEmpty()) {
                        Role role = DataManager.getInstance().getDao().getRole("member");
                        if (role != null) {
                            for (String groupName : provider.getAddUserToGroups()) {
                                UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(groupName);
                                if (userGroup != null && !userGroup.getMembers().contains(u)) {
                                    userGroup.addMember(u, role);
                                    logger.debug("Added user {} to user group '{}'", u.getId(), userGroup.getName());
                                }
                            }
                        }
                    }
                    this.loggedInProvider = provider;
                } catch (DAOException | IOException | IndexUnreachableException | PresentationException | IllegalStateException e) {
                    //user may login, but setting up viewer account failed
                    provider.logout();
                    throw new AuthenticationProviderException(e);
                }
            } else {
                Messages.error("errLoginInactive");
            }
        } catch (AuthenticationProviderException e) {
            logger.error("Error logging in ", e);
            Messages.error("errLoginError");
        } finally {
            logger.trace("releasing result");
            result.setRedirected();
            // Reset to local provider so that the email field is displayed
            setAuthenticationProvider(getLocalAuthenticationProvider());
        }
    }

    /**
     * Redirects to the given URL. The type of response
     * 
     * @param response {@link HttpServletResponse} from {@link LoginResult}
     * @param url Redirect URL
     * @throws IOException
     */
    private static void doRedirect(HttpServletResponse response, String url) throws IOException {
        logger.trace("doRedirect: {}", url);
        if (response.equals(BeanUtils.getResponse())) {
            // Local authentication: use Faces external context for redirection to avoid an IllegalStateException 
            FacesContext.getCurrentInstance()
                    .getExternalContext()
                    .redirect(url);
        } else {
            // OpenID, etc.: Use response from LoginResult
            response.sendRedirect(url);
        }
    }

    /**
     * Logout action method.
     *
     * @return an empty string after logging out the current user and invalidating the session
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     * @throws IOException
     */
    public String logout() throws AuthenticationProviderException, IOException {
        logger.trace("logout");

        HttpServletRequest request = BeanUtils.getRequest();
        String url = getRedirectUrl(request); // do this before resetting session

        if (user != null) {
            user.setTranskribusSession(null);
            setUser(null);
        }
        password = null;
        hasAdminBackendAccess = null;
        if (loggedInProvider != null) {
            loggedInProvider.logout();
            loggedInProvider = null;
        }
        try {
            BeanUtils.wipeSessionAttributes(request.getSession());
            SearchHelper.updateFilterQuerySuffix(request, IPrivilegeHolder.PRIV_LIST);
            DataManager.getInstance().getBearerTokenManager().purgeExpiredTokens();
        } catch (IndexUnreachableException | PresentationException | DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        try {
            request.logout();
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
        }
        session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }

        // Actively redirect to outcome URL, because the loaded record is unloaded at this point
        if (StringUtils.isNotEmpty(url)) {
            doRedirect(BeanUtils.getResponse(), url);
        }

        return url;
    }

    /**
     * @param request Current HTTP servlet request
     * @return Redirect outcome
     */
    private String getRedirectUrl(HttpServletRequest request) {
        Optional<ViewerPath> oCurrentPath = ViewHistory.getCurrentView(request);
        if (StringUtils.isNotEmpty(redirectUrl)) {
            if ("#".equals(redirectUrl)) {
                logger.trace("Stay on current page");
            }
            logger.trace("Redirecting to {}", redirectUrl);
            String url = this.redirectUrl;
            this.redirectUrl = "";

            // Do not redirect to user backend pages because LoginFilter won't work here for some reason
            String servletPath = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            if (url.length() < servletPath.length() || !LoginFilter.isRestrictedUri(url.substring(servletPath.length()))) {
                return url;
            }
        } else if (oCurrentPath.isPresent()) {
            ViewerPath currentPath = oCurrentPath.get();
            if (LoginFilter.isRestrictedUri(currentPath.getCombinedUrl())) {
                logger.trace("Redirecting to start page");
                return ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + "/";
            }
            logger.trace("Redirecting to current url {}", currentPath.getCombinedPrettyfiedUrl());
            return ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + currentPath.getCombinedPrettyfiedUrl();
        } else {
            // IF ViewerPath is unavailable, extract URI via PrettyContext
            PrettyContext prettyContext = PrettyContext.getCurrentInstance(request);
            if (prettyContext != null && LoginFilter.isRestrictedUri(prettyContext.getRequestURL().toURL())) {
                logger.trace("Redirecting to start page");
                return ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + "/";
            }
        }

        return "";
    }

    /**
     * Returns a list of all existing users (minus the superusers and the current user).
     *
     * @return a list of all non-superuser users excluding the currently logged-in user
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers() throws DAOException {
        List<User> ret = new ArrayList<>();

        for (User u : DataManager.getInstance().getDao().getAllUsers(true)) {
            if (!u.isSuperuser() && !u.equals(getUser())) {
                ret.add(u);
            }
        }

        return ret;
    }

    /**
     *
     * @param user User for whom to send the activation email
     * @return true if activation email sent successfully; false otherwise
     */
    private boolean sendActivationEmail(User user) {
        if (StringUtils.isNotEmpty(user.getEmail())) {
            // Generate and save the activation key, if not yet set
            if (user.getActivationKey() == null) {
                user.setActivationKey(StringTools.generateHash(UUID.randomUUID() + String.valueOf(System.currentTimeMillis())));
            }

            // Generate e-mail text
            StringBuilder sb = new StringBuilder();
            if (navigationHelper == null) {
                logger.debug("NavigationHelper not found");
                return false;
            }
            String baseUrl = navigationHelper.getApplicationUrl();
            String activationUrl = new StringBuilder(baseUrl).append("user/activate/")
                    .append(user.getEmail())
                    .append('/')
                    .append(user.getActivationKey())
                    .append("/")
                    .toString();
            sb.append(ViewerResourceBundle.getTranslation("user_activationEmailBody", null)
                    .replace("{0}", baseUrl)
                    .replace("{1}", activationUrl)
                    .replace("{2}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress()));

            // Send
            try {
                if (NetTools.postMail(Collections.singletonList(user.getEmail()), null, null,
                        ViewerResourceBundle.getTranslation("user_activationEmailSubject", null),
                        sb.toString())) {
                    logger.debug("Activation e-mail sent for: {}", user.getEmail());
                    return true;
                }
                logger.error("Could not send activation e-mail to: {}", user.getEmail());
            } catch (MessagingException e) {
                logger.error(e.getMessage(), e);
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
        }

        return false;
    }

    /**
     * Sends a password reset link to the current e-mail address.
     *
     * @return the navigation outcome after attempting to send the password reset email
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String sendPasswordResetLinkAction() throws DAOException {
        User u = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (u != null && !u.isOpenIdUser()) {
            if (u.isActive()) {
                u.setActivationKey(StringTools.generateHash(String.valueOf(System.currentTimeMillis())));
                String requesterIp = "???";
                if (FacesContext.getCurrentInstance().getExternalContext() != null
                        && FacesContext.getCurrentInstance().getExternalContext().getRequest() != null) {
                    requesterIp = NetTools.getIpAddress((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
                }
                String resetUrl = navigationHelper.getApplicationUrl() + "user/resetpw/" + u.getEmail() + "/" + u.getActivationKey() + "/";

                if (DataManager.getInstance().getDao().updateUser(u)) {
                    try {
                        if (NetTools.postMail(Collections.singletonList(email), null, null,
                                ViewerResourceBundle.getTranslation("user_retrieveAccountConfirmationEmailSubject", null),
                                ViewerResourceBundle.getTranslation("user_retrieveAccountConfirmationEmailBody", null)
                                        .replace("{0}", requesterIp)
                                        .replace("{1}", resetUrl))) {
                            email = null;
                            Messages.info("user_retrieveAccountConfirmationEmailMessage");
                            return "user?faces-redirect=true";
                        }
                        logger.error("Could not send passwort reset link e-mail to: {}", u.getEmail());
                    } catch (UnsupportedEncodingException | MessagingException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                Messages.error(ViewerResourceBundle.getTranslation("user_retrieveAccountError", null)
                        .replace("{0}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress()));
                return "userRetrieveAccount";
            }

            // Send new activation mail if not yet activated
            if (sendActivationEmail(u)) {
                Messages.info(ViewerResourceBundle.getTranslation("user_activationEmailReSent", null));
            } else {
                Messages.error(ViewerResourceBundle.getTranslation("errSendEmail", null));
            }
            return "pretty:user";
        }

        Messages.error("user_retrieveAccountUserNotFound");
        return "userRetrieveAccount";
    }

    /**
     * Generates a new user password if the key is correct.
     *
     * @return the navigation outcome after attempting to reset the password via the activation key
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String resetPasswordAction() throws DAOException {
        User u = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (u != null && u.isActive() && !u.isOpenIdUser()) {
            if (StringUtils.isNotEmpty(activationKey) && activationKey.equals(u.getActivationKey())) {
                String newPassword = StringTools.generateHash(String.valueOf(System.currentTimeMillis())).substring(0, 8);
                u.setNewPassword(newPassword);
                u.setActivationKey(null);
                try {
                    if (NetTools.postMail(Collections.singletonList(email), null, null,
                            ViewerResourceBundle.getTranslation("user_retrieveAccountNewPasswordEmailSubject", null),
                            ViewerResourceBundle.getTranslation("user_retrieveAccountNewPasswordEmailBody", null).replace("{0}", newPassword))
                            && DataManager.getInstance().getDao().updateUser(u)) {
                        email = null;
                        Messages.info("user_retrieveAccountPasswordResetMessage");
                        return "user?faces-redirect=true";
                    }
                    logger.error("Could not send new password e-mail to: {}", u.getEmail());
                } catch (UnsupportedEncodingException | MessagingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            Messages.error(ViewerResourceBundle.getTranslation("user_retrieveAccountError", null)
                    .replace("{0}", DataManager.getInstance().getConfiguration().getDefaultFeedbackEmailAddress()));
            return "user?faces-redirect=true";
        }

        Messages.error("user_retrieveAccountUserNotFound");
        return "user?faces-redirect=true";
    }

    /**
     * transkribusLoginAction.
     *
     * @return an empty string after attempting to authenticate with Transkribus
     */
    public String transkribusLoginAction() {
        if (transkribusUserName == null || transkribusPassword == null) {
            Messages.error("transkribus_loginError");
            return "";
        }

        try {
            user.setTranskribusSession(TranskribusUtils.login(DataManager.getInstance().getConfiguration().getTranskribusRestApiUrl(),
                    transkribusUserName, transkribusPassword));
        } catch (IOException | JDOMException e) {
            Messages.error("transkribus_loginError");
            return "";
        }
        if (user.getTranskribusSession() == null) {
            Messages.error("transkribus_loginError");
            return "";
        }

        return "";
    }

    /**
     * Getter for the field <code>user</code>.
     *
     * @return the currently authenticated user, or null if no user is logged in
     */
    public User getUser() {
        return user;
    }

    /**
     * Setter for the field <code>user</code>.
     *
     * @param user the currently authenticated user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * Getter for the field <code>nickName</code>.
     *
     * @return the nickname entered during registration or profile editing
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Setter for the field <code>nickName</code>.
     *
     * @param nickName the desired nickname for the user account
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * Getter for the field <code>email</code>.
     *
     * @return the email address entered for login or registration
     */
    public String getEmail() {
        return email;
    }

    /**
     * Setter for the field <code>email</code>.
     *
     * @param email the email address for login or registration
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Getter for the field <code>password</code>.
     *
     * @return the plain-text password entered by the user for login or registration
     */
    public String getPassword() {
        return password;
    }

    /**
     * Setter for the field <code>password</code>.
     *
     * @param password the plain-text password entered by the user for login or registration
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * isLoggedIn.
     *
     * @return true if a user is currently logged in and their account is active and not suspended, false otherwise
     */
    public boolean isLoggedIn() {
        return user != null && user.isActive() && !user.isSuspended();
    }

    /**
     * isAdmin.
     *
     * @return true if the currently logged-in user is a superuser (administrator), false otherwise
     */
    public boolean isAdmin() {
        return user != null && user.isSuperuser();
    }

    /**
     * isUserRegistrationEnabled.
     *
     * @return true if self-registration for new users is enabled in the configuration, false otherwise
     */
    public boolean isUserRegistrationEnabled() {
        return DataManager.getInstance().getConfiguration().isUserRegistrationEnabled();
    }

    /**
     * isShowOpenId.
     *
     * @return true if OpenID Connect authentication options should be shown in the UI, false otherwise
     */
    public boolean isShowOpenId() {
        return DataManager.getInstance().getConfiguration().isShowOpenIdConnect();
    }

    public boolean isCloseTabAfterLogin() {
        return StringUtils.isNotEmpty(origin);
    }

    /**
     * Getter for the field <code>authenticationProviders</code>.
     *
     * @return a list of all configured authentication providers
     */
    public synchronized List<IAuthenticationProvider> getAuthenticationProviders() {
        return DataManager.getInstance().getConfiguration().getAuthenticationProviders();
    }

    /**
     * getLocalAuthenticationProvider.
     *
     * @return the first configured local authentication provider, or null if none is configured
     */
    public IAuthenticationProvider getLocalAuthenticationProvider() {
        return getProvidersOfType("local").stream().findFirst().orElse(null);
    }

    /**
     * getXserviceAuthenticationProvider.
     *
     * @return the first configured userPassword-type authentication provider, or null if none is configured
     */
    public IAuthenticationProvider getXserviceAuthenticationProvider() {
        return getProvidersOfType("userPassword").stream().findFirst().orElse(null);
    }

    /**
     * showAuthenticationProviderSelection.
     *
     * @return true if more than one local or username/password authentication provider is configured, false otherwise
     */
    public boolean showAuthenticationProviderSelection() {
        return getAuthenticationProviders().stream()
                .filter(p -> "local".equalsIgnoreCase(p.getType()) || "userPassword".equalsIgnoreCase(p.getType()))
                .count() > 1;
    }

    /**
     * Setter for the field <code>authenticationProvider</code>.
     *
     * @param provider authentication provider to set as the active one
     */
    public void setAuthenticationProvider(IAuthenticationProvider provider) {
        logger.trace("setAuthenticationProvider: {}", provider.getName());
        this.authenticationProvider = provider;
    }

    /**
     * Getter for the field <code>authenticationProvider</code>.
     *
     * @return the currently active authentication provider
     */
    public IAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    /**
     * setAuthenticationProviderName.
     *
     * @param name name used to look up the matching authentication provider
     */
    public void setAuthenticationProviderName(String name) {
        this.authenticationProvider = getAuthenticationProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(getLocalAuthenticationProvider());
    }

    /**
     * getAuthenticationProviderName.
     *
     * @return the name of the currently selected authentication provider, or an empty string if none is selected
     */
    public String getAuthenticationProviderName() {
        if (this.authenticationProvider != null) {
            return this.authenticationProvider.getName();
        }

        return "";
    }

    /**
     * Getter for the field <code>passwordOne</code>.
     *
     * @return the first password entry for new account creation or password change
     */
    public String getPasswordOne() {
        return this.passwordOne;
    }

    /**
     * Setter for the field <code>passwordOne</code>.
     *
     * @param passwordOne first password entry for new account creation
     */
    public void setPasswordOne(String passwordOne) {
        this.passwordOne = passwordOne;
    }

    /**
     * Getter for the field <code>passwordTwo</code>.
     *
     * @return the confirmation password entry for new account creation or password change
     */
    public String getPasswordTwo() {
        return this.passwordTwo;
    }

    /**
     * Setter for the field <code>passwordTwo</code>.
     *
     * @param passwordTwo confirmation password entry for new account creation
     */
    public void setPasswordTwo(String passwordTwo) {
        this.passwordTwo = passwordTwo;
    }

    /**
     * resetPasswordFields.
     */
    public void resetPasswordFields() {
        passwordOne = "";
        passwordTwo = "";
    }

    /**
     * Getter for the field <code>lastName</code>.
     *
     * @return the last name of the user
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Setter for the field <code>lastName</code>.
     *
     * @param lastName the last name of the user
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Getter for the field <code>redirectUrl</code>.
     *
     * @return the URL to redirect to after a successful login, or null if the default redirect applies
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * Setter for the field <code>redirectUrl</code>.
     *
     * @param redirectUrl the URL to redirect to after a successful login, or null to use the default
     */
    public void setRedirectUrl(String redirectUrl) {
        if (!"RES_NOT_FOUND".equals(redirectUrl)) {
            if (StringUtils.isNotEmpty(redirectUrl) && !redirectUrl.equals("#")
                    && !NetTools.isRedirectUrlAllowed(redirectUrl, navigationHelper != null ? navigationHelper.getApplicationUrl() : null)) {
                logger.warn("Rejected redirect URL not on whitelist: {}", redirectUrl);
                return;
            }
            this.redirectUrl = redirectUrl;
            logger.trace("Redirect URL: {}", redirectUrl);
        }
    }

    
    public String getOrigin() {
        return origin;
    }

    
    public void setOrigin(String origin) {
        logger.debug("setOrigin: {}", origin);
        this.origin = origin;
    }

    /**
     * Getter for the field <code>activationKey</code>.
     *
     * @return the account activation key sent to the user by email
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * Setter for the field <code>activationKey</code>.
     *
     * @param activationKey the account activation key sent to the user by email
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * Getter for the field <code>transkribusUserName</code>.
     *
     * @return the Transkribus account username for the linked integration
     */
    public String getTranskribusUserName() {
        return transkribusUserName;
    }

    /**
     * Setter for the field <code>transkribusUserName</code>.
     *
     * @param transkribusUserName the Transkribus account username for the linked integration
     */
    public void setTranskribusUserName(String transkribusUserName) {
        this.transkribusUserName = transkribusUserName;
    }

    /**
     * Getter for the field <code>transkribusPassword</code>.
     *
     * @return the Transkribus account password for the linked integration
     */
    public String getTranskribusPassword() {
        return transkribusPassword;
    }

    /**
     * Setter for the field <code>transkribusPassword</code>.
     *
     * @param transkribusPassword the Transkribus account password for the linked integration
     */
    public void setTranskribusPassword(String transkribusPassword) {
        this.transkribusPassword = transkribusPassword;
    }

    /**
     * Checks whether the logged in user has access to the admin backend via being an admin or having CMS/campaign/comments access. Result is
     * persisted for the duration of the session.
     *
     * @return true if the current user has access to the admin backend, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     */
    public Boolean getHasAdminBackendAccess() throws PresentationException, IndexUnreachableException, DAOException {
        if (hasAdminBackendAccess == null) {
            if (user == null) {
                hasAdminBackendAccess = false;
            } else {
                hasAdminBackendAccess = user.isSuperuser() || user.isHasCmsPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES)
                        || CrowdsourcingTools.isUserOwnsAnyCampaigns(user) || CommentManager.isUserHasAccessToCommentGroups(user);
            }
        }

        return hasAdminBackendAccess;
    }

    /**
     * Setter for the field <code>hasAdminBackendAccess</code>.
     *
     * @param hasAdminBackendAccess true if the current user has access to the admin backend; false otherwise
     */
    public void setHasAdminBackendAccess(Boolean hasAdminBackendAccess) {
        this.hasAdminBackendAccess = hasAdminBackendAccess;
    }

    /**
     * userEquals.
     *
     * @param id database ID to compare against the current user
     * @return true if the given database ID matches the ID of the currently logged-in user, false otherwise
     */
    public boolean userEquals(long id) {
        return getUser().getId().equals(id);
    }

    /**
     * hasProvidersOfType.
     *
     * @param type provider type string to match (e.g. "local", "openId")
     * @return true if at least one configured authentication provider matches the given type, false otherwise
     */
    public boolean hasProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().anyMatch(provider -> type.equalsIgnoreCase(provider.getType()));
        }
        return false;
    }

    /**
     * getProvidersOfType.
     *
     * @param type provider type string to filter by (e.g. "local", "openId")
     * @return a list of authentication providers of the given type
     */
    public List<IAuthenticationProvider> getProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().filter(provider -> type.equalsIgnoreCase(provider.getType())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * getNumberOfProviderTypes.
     *
     * @return a int.
     */
    public int getNumberOfProviderTypes() {
        return getAuthenticationProviders().stream().collect(Collectors.groupingBy(IAuthenticationProvider::getType, Collectors.counting())).size();
    }

    /**
     * isAllowPasswordChange.
     *
     * @return true if the currently used authentication provider allows the user to change their password, false otherwise
     */
    public boolean isAllowPasswordChange() {
        return loggedInProvider != null && loggedInProvider.allowsPasswordChange();
    }

    /**
     * isAllowNickNameChange.
     *
     * @return true if the currently used authentication provider allows the user to change their nickname, false otherwise
     */
    public boolean isAllowNickNameChange() {
        return loggedInProvider != null && loggedInProvider.allowsNicknameChange();

    }

    /**
     * isAllowEmailChange.
     *
     * @return true if the currently used authentication provider allows the user to change their email address, false otherwise
     */
    public boolean isAllowEmailChange() {
        return loggedInProvider != null && loggedInProvider.allowsEmailChange();

    }

    /**
     * isRequireLoginCaptcha.
     *
     * @return true if a CAPTCHA is required for the login form, false otherwise
     */
    public boolean isRequireLoginCaptcha() {
        // TODO
        return false;
    }

    /**
     * Checks if the current user is required to agree to the terms of use.
     *
     * @return true if a user is logged in and {@link io.goobi.viewer.model.security.user.User#isAgreedToTermsOfUse()} returns false for this user
     */
    public boolean mustAgreeToTermsOfUse() {
        return this.user != null && !this.user.isAgreedToTermsOfUse();
    }

    /**
     * agreeToTermsOfUse.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void agreeToTermsOfUse() throws DAOException {
        if (this.user != null) {
            this.user.setAgreedToTermsOfUse(true);
            DataManager.getInstance().getDao().updateUser(this.user);
        }
    }

    /**
     * rejectTermsOfUse.
     *
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void rejectTermsOfUse() throws DAOException {
        if (this.user != null) {
            this.user.setAgreedToTermsOfUse(false);
            DataManager.getInstance().getDao().updateUser(this.user);
        }
    }

    /**
     * logoutWithMessage.
     *
     * @param messageKey i18n key for the info message shown after logout
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     * @throws IOException
     */
    public void logoutWithMessage(String messageKey) throws AuthenticationProviderException, IOException {
        this.logout();
        Messages.info(messageKey);

    }

    /**
     * createBackupOfCurrentUser.
     */
    public void createBackupOfCurrentUser() {
        if (getUser() != null) {
            getUser().backupFields();
        }
    }

    /**
     * Represents a {@link TimerTask} that periodically updates the session timeout counter to keep the user's session information current.
     */
    public class SessionTimeoutMonitorTask extends TimerTask {
        @Override
        public void run() {
            logger.trace("timeout monitor update task running");
            updateSessionTimeoutCounter();
        }
    }
}
