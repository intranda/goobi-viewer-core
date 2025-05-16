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
import java.util.Timer;
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
import io.goobi.viewer.model.security.authentication.HttpHeaderProvider;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LoginResult;
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
 * Primarily for user authentication.
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
    private SessionBean sessionBean;

    @Inject
    @Push
    private PushContext sessionTimeoutCounter;

    private Timer sessionTimeoutMonitorTimer;

    /**
     * The logged in user
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
    private String transkribusUserName;
    private String transkribusPassword;
    private Boolean hasAdminBackendAccess;

    /**
     * Empty constructor.
     */
    public UserBean() {
        // the emptiness inside
        this.authenticationProvider = getLocalAuthenticationProvider();
    }

    /**
     * Setter for unit tests.
     * 
     * @param sessionBean
     */
    void setSessionBean(SessionBean sessionBean) {
        this.sessionBean = sessionBean;
    }

    /**
     * <p>
     * updateSessionTimeoutCounter.
     * </p>
     */
    public void updateSessionTimeoutCounter() {
        logger.trace("updateSessionTimeoutCounter");
        sessionTimeoutCounter.send("update");
    }

    /**
     * <p>
     * getSessionTimeout.
     * </p>
     *
     * @return a {@link java.lang.String} object
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
     * @return a {@link java.lang.String} object.
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
     * <p>
     * activateUserAccountAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     */
    public String login() throws AuthenticationProviderException, IllegalStateException, InterruptedException, ExecutionException {
        return login(getAuthenticationProvider());
    }

    /**
     * <p>
     * login.
     * </p>
     *
     * @param provider a {@link io.goobi.viewer.model.security.authentication.IAuthenticationProvider} object.
     * @return a {@link java.lang.String} object.
     * @throws java.lang.IllegalStateException if any.
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     * @throws java.lang.InterruptedException if any.
     * @throws java.util.concurrent.ExecutionException if any.
     */
    public String login(IAuthenticationProvider provider)
            throws AuthenticationProviderException, IllegalStateException, InterruptedException, ExecutionException {
        if ("#".equals(this.redirectUrl)) {
            this.redirectUrl = buildRedirectUrl();
        }
        logger.trace("login");
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
     * @param provider
     * @param result
     * @throws IllegalStateException
     */
    private void completeLogin(IAuthenticationProvider provider, LoginResult result) {
        logger.debug("completeLogin");
        HttpServletResponse response = result.getResponse();
        HttpServletRequest request = result.getRequest();
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
                            return;
                        }
                        // Exception if different user logged in
                        throw new AuthenticationProviderException("errLoginError");
                    }
                    if (sessionBean != null) {
                        sessionBean.wipeSessionAttributes();
                    }
                    DataManager.getInstance().getBookmarkManager().addSessionBookmarkListToUser(u, request);
                    // Update last login
                    u.setLastLogin(LocalDateTime.now());
                    if (!DataManager.getInstance().getDao().updateUser(u)) {
                        logger.error("Could not update user in DB.");
                    }
                    setUser(u);
                    if (request != null && request.getSession(false) != null) {
                        request.getSession(false).setAttribute("user", u);
                        logger.trace("Added user to HTTP session ID {}: {}", request.getSession(false).getId(), u.getId());
                    }

                    // Start timer
                    //                    sessionTimeoutMonitorTimer = new Timer();
                    //                    sessionTimeoutMonitorTimer.scheduleAtFixedRate(new SessionTimeoutMonitorTask(), 0, 10000);

                    if (response != null && StringUtils.isNotEmpty(redirectUrl)) {
                        logger.trace("Redirecting to {}", redirectUrl);
                        String url = this.redirectUrl;
                        this.redirectUrl = "";
                        doRedirect(response, url);
                    } else if (response != null) {
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
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public String logout() throws AuthenticationProviderException {
        logger.trace("logout");

        HttpServletRequest request = BeanUtils.getRequest();
        String url = redirect(request);

        user.setTranskribusSession(null);
        setUser(null);
        password = null;
        hasAdminBackendAccess = null;
        if (loggedInProvider != null) {
            loggedInProvider.logout();
            loggedInProvider = null;
        }
        try {
            // Kill session timeout update timer
            if (sessionTimeoutMonitorTimer != null) {
                sessionTimeoutMonitorTimer.cancel();
            }
            if (sessionBean != null) {
                sessionBean.wipeSessionAttributes();
            }
            SearchHelper.updateFilterQuerySuffix(request, IPrivilegeHolder.PRIV_LIST);
        } catch (IndexUnreachableException | PresentationException | DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        try {
            request.logout();
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
        }
        HttpSession session = request.getSession(false);
        session.invalidate();
        return url;
    }

    /**
     * @param request
     * @return Redirect outcome
     */
    private String redirect(HttpServletRequest request) {
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
                return "pretty:index";
            }
            logger.trace("Redirecting to current url {}", currentPath.getCombinedPrettyfiedUrl());
            return currentPath.getCombinedPrettyfiedUrl();
        } else {
            // IF ViewerPath is unavailable, extract URI via PrettyContext
            PrettyContext prettyContext = PrettyContext.getCurrentInstance(request);
            if (prettyContext != null && LoginFilter.isRestrictedUri(prettyContext.getRequestURL().toURL())) {
                logger.trace("Redirecting to start page");
                return "pretty:index";
            }
        }

        return "";
    }

    /**
     * Returns a list of all existing users (minus the superusers and the current user).
     *
     * @return a {@link java.util.List} object.
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
     * @param user
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
     * @return a {@link java.lang.String} object.
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
     * @return a {@link java.lang.String} object.
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
     * <p>
     * transkribusLoginAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
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
     * <p>
     * Getter for the field <code>user</code>.
     * </p>
     *
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * <p>
     * Setter for the field <code>user</code>.
     * </p>
     *
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * <p>
     * Getter for the field <code>nickName</code>.
     * </p>
     *
     * @return the nickName
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * <p>
     * Setter for the field <code>nickName</code>.
     * </p>
     *
     * @param nickName the nickName to set
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * <p>
     * Getter for the field <code>email</code>.
     * </p>
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * <p>
     * Setter for the field <code>email</code>.
     * </p>
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * <p>
     * Getter for the field <code>password</code>.
     * </p>
     *
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * <p>
     * Setter for the field <code>password</code>.
     * </p>
     *
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * <p>
     * isLoggedIn.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isLoggedIn() {
        return user != null && user.isActive() && !user.isSuspended();
    }

    /**
     * <p>
     * isAdmin.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAdmin() {
        return user != null && user.isSuperuser();
    }

    /**
     * <p>
     * isUserRegistrationEnabled.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isUserRegistrationEnabled() {
        return DataManager.getInstance().getConfiguration().isUserRegistrationEnabled();
    }

    /**
     * <p>
     * isShowOpenId.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isShowOpenId() {
        return DataManager.getInstance().getConfiguration().isShowOpenIdConnect();
    }

    /**
     * <p>
     * Getter for the field <code>authenticationProviders</code>.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public synchronized List<IAuthenticationProvider> getAuthenticationProviders() {
        return DataManager.getInstance().getConfiguration().getAuthenticationProviders();
    }

    /**
     * <p>
     * getLocalAuthenticationProvider.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.IAuthenticationProvider} object.
     */
    public IAuthenticationProvider getLocalAuthenticationProvider() {
        return getProvidersOfType("local").stream().findFirst().orElse(null);
    }

    /**
     * <p>
     * getXserviceAuthenticationProvider.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.IAuthenticationProvider} object.
     */
    public IAuthenticationProvider getXserviceAuthenticationProvider() {
        return getProvidersOfType("userPassword").stream().findFirst().orElse(null);
    }

    /**
     * <p>
     * showAuthenticationProviderSelection.
     * </p>
     *
     * @return a boolean
     */
    public boolean showAuthenticationProviderSelection() {
        return getAuthenticationProviders().stream()
                .filter(p -> "local".equalsIgnoreCase(p.getType()) || "userPassword".equalsIgnoreCase(p.getType()))
                .count() > 1;
    }

    /**
     * <p>
     * Setter for the field <code>authenticationProvider</code>.
     * </p>
     *
     * @param provider a {@link io.goobi.viewer.model.security.authentication.IAuthenticationProvider} object.
     */
    public void setAuthenticationProvider(IAuthenticationProvider provider) {
        logger.trace("setAuthenticationProvider: {}", provider.getName());
        this.authenticationProvider = provider;
    }

    /**
     * <p>
     * Getter for the field <code>authenticationProvider</code>.
     * </p>
     *
     * @return a {@link io.goobi.viewer.model.security.authentication.IAuthenticationProvider} object.
     */
    public IAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    /**
     * <p>
     * setAuthenticationProviderName.
     * </p>
     *
     * @param name a {@link java.lang.String} object.
     */
    public void setAuthenticationProviderName(String name) {
        this.authenticationProvider = getAuthenticationProviders().stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(getLocalAuthenticationProvider());
    }

    /**
     * <p>
     * getAuthenticationProviderName.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAuthenticationProviderName() {
        if (this.authenticationProvider != null) {
            return this.authenticationProvider.getName();
        }

        return "";
    }

    /**
     * <p>
     * Getter for the field <code>passwordOne</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPasswordOne() {
        return this.passwordOne;
    }

    /**
     * <p>
     * Setter for the field <code>passwordOne</code>.
     * </p>
     *
     * @param passwordOne a {@link java.lang.String} object.
     */
    public void setPasswordOne(String passwordOne) {
        this.passwordOne = passwordOne;
    }

    /**
     * <p>
     * Getter for the field <code>passwordTwo</code>.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getPasswordTwo() {
        return this.passwordTwo;
    }

    /**
     * <p>
     * Setter for the field <code>passwordTwo</code>.
     * </p>
     *
     * @param passwordTwo a {@link java.lang.String} object.
     */
    public void setPasswordTwo(String passwordTwo) {
        this.passwordTwo = passwordTwo;
    }

    /**
     * <p>
     * resetPasswordFields.
     * </p>
     */
    public void resetPasswordFields() {
        passwordOne = "";
        passwordTwo = "";
    }

    /**
     * <p>
     * Getter for the field <code>lastName</code>.
     * </p>
     *
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * <p>
     * Setter for the field <code>lastName</code>.
     * </p>
     *
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * <p>
     * Getter for the field <code>redirectUrl</code>.
     * </p>
     *
     * @return the redirectUrl
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * <p>
     * Setter for the field <code>redirectUrl</code>.
     * </p>
     *
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl) {
        if (!"RES_NOT_FOUND".equals(redirectUrl)) {
            this.redirectUrl = redirectUrl;
            logger.trace("Redirect URL: {}", redirectUrl);
        }
    }

    /**
     * <p>
     * Getter for the field <code>activationKey</code>.
     * </p>
     *
     * @return the activationKey
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * <p>
     * Setter for the field <code>activationKey</code>.
     * </p>
     *
     * @param activationKey the activationKey to set
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * <p>
     * Getter for the field <code>transkribusUserName</code>.
     * </p>
     *
     * @return the transkribusUserName
     */
    public String getTranskribusUserName() {
        return transkribusUserName;
    }

    /**
     * <p>
     * Setter for the field <code>transkribusUserName</code>.
     * </p>
     *
     * @param transkribusUserName the transkribusUserName to set
     */
    public void setTranskribusUserName(String transkribusUserName) {
        this.transkribusUserName = transkribusUserName;
    }

    /**
     * <p>
     * Getter for the field <code>transkribusPassword</code>.
     * </p>
     *
     * @return the transkribusPassword
     */
    public String getTranskribusPassword() {
        return transkribusPassword;
    }

    /**
     * <p>
     * Setter for the field <code>transkribusPassword</code>.
     * </p>
     *
     * @param transkribusPassword the transkribusPassword to set
     */
    public void setTranskribusPassword(String transkribusPassword) {
        this.transkribusPassword = transkribusPassword;
    }

    /**
     * Checks whether the logged in user has access to the admin backend via being an admin or having CMS/campaign/comments access. Result is
     * persisted for the duration of the session.
     *
     * @return the hasAdminBackendAccess
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
     * <p>
     * Setter for the field <code>hasAdminBackendAccess</code>.
     * </p>
     *
     * @param hasAdminBackendAccess the hasAdminBackendAccess to set
     */
    public void setHasAdminBackendAccess(Boolean hasAdminBackendAccess) {
        this.hasAdminBackendAccess = hasAdminBackendAccess;
    }

    /**
     * <p>
     * userEquals.
     * </p>
     *
     * @param id a long.
     * @return a boolean.
     */
    public boolean userEquals(long id) {
        return getUser().getId().equals(id);
    }

    /**
     * <p>
     * hasProvidersOfType.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean hasProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().anyMatch(provider -> type.equalsIgnoreCase(provider.getType()));
        }
        return false;
    }

    /**
     * <p>
     * getProvidersOfType.
     * </p>
     *
     * @param type a {@link java.lang.String} object.
     * @return a {@link java.util.List} object.
     */
    public List<IAuthenticationProvider> getProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().filter(provider -> type.equalsIgnoreCase(provider.getType())).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * <p>
     * getNumberOfProviderTypes.
     * </p>
     *
     * @return a int.
     */
    public int getNumberOfProviderTypes() {
        return getAuthenticationProviders().stream().collect(Collectors.groupingBy(IAuthenticationProvider::getType, Collectors.counting())).size();
    }

    /**
     * <p>
     * isAllowPasswordChange.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAllowPasswordChange() {
        return loggedInProvider != null && loggedInProvider.allowsPasswordChange();
    }

    /**
     * <p>
     * isAllowNickNameChange.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAllowNickNameChange() {
        return loggedInProvider != null && loggedInProvider.allowsNicknameChange();

    }

    /**
     * <p>
     * isAllowEmailChange.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isAllowEmailChange() {
        return loggedInProvider != null && loggedInProvider.allowsEmailChange();

    }

    /**
     * <p>
     * isRequireLoginCaptcha.
     * </p>
     *
     * @return a boolean
     */
    public boolean isRequireLoginCaptcha() {
        // TODO
        return false;
    }

    /**
     * Check if the current user is required to agree to the terms of use
     *
     * @return true if a user is logged in and {@link io.goobi.viewer.model.security.user.User#isAgreedToTermsOfUse()} returns false for this user
     */
    public boolean mustAgreeToTermsOfUse() {
        return this.user != null && !this.user.isAgreedToTermsOfUse();
    }

    /**
     * <p>
     * agreeToTermsOfUse.
     * </p>
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
     * <p>
     * rejectTermsOfUse.
     * </p>
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
     * <p>
     * logoutWithMessage.
     * </p>
     *
     * @param messageKey a {@link java.lang.String} object
     * @throws io.goobi.viewer.model.security.authentication.AuthenticationProviderException if any.
     */
    public void logoutWithMessage(String messageKey) throws AuthenticationProviderException {
        this.logout();
        Messages.info(messageKey);

    }

    /**
     * <p>
     * createBackupOfCurrentUser.
     * </p>
     */
    public void createBackupOfCurrentUser() {
        if (getUser() != null) {
            getUser().backupFields();
        }
    }

    public class SessionTimeoutMonitorTask extends TimerTask {
        @Override
        public void run() {
            logger.trace("timeout monitor update task running");
            updateSessionTimeoutCounter();
        }
    }
}
