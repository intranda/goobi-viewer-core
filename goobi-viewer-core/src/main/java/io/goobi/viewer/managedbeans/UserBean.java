/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.managedbeans;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.MessagingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ocpsoft.pretty.PrettyContext;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.controller.TranskribusUtils;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.HTTPException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.faces.validators.PasswordValidator;
import io.goobi.viewer.filters.LoginFilter;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.search.Search;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.authentication.AuthenticationProviderException;
import io.goobi.viewer.model.security.authentication.IAuthenticationProvider;
import io.goobi.viewer.model.security.authentication.LoginResult;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import io.goobi.viewer.model.urlresolution.ViewHistory;
import io.goobi.viewer.model.urlresolution.ViewerPath;
import io.goobi.viewer.model.viewer.Feedback;
import io.goobi.viewer.servlets.utils.ServletUtils;

/**
 * <p>
 * UserBean class.
 * </p>
 */
@Named
@SessionScoped
public class UserBean implements Serializable {

    private static final long serialVersionUID = 5917173704087714181L;

    private static final Logger logger = LoggerFactory.getLogger(UserBean.class);

    @Inject
    private NavigationHelper navigationHelper;

    private User user;
    private String nickName;
    private String email;
    private transient String password;
    private String activationKey;
    /** Selected OpenID Connect provider. */
    private IAuthenticationProvider authenticationProvider;
    private IAuthenticationProvider loggedInProvider;
    private List<IAuthenticationProvider> authenticationProviders;

    // Passwords for creating an new local user account
    private transient String passwordOne = "";
    private transient String passwordTwo = "";

    private SecurityQuestion securityQuestion = null;
    private String securityAnswer;
    /** Honey pot field invisible to human users. */
    private String lastName;
    private String redirectUrl = null;
    private Feedback feedback;
    private String transkribusUserName;
    private String transkribusPassword;

    // private CompletableFuture<Optional<User>> loginFuture = null;

    /**
     * Empty constructor.
     */
    public UserBean() {
        // the emptiness inside
        this.authenticationProvider = getLocalAuthenticationProvider();
    }

    /**
     * Required setter for ManagedProperty injection
     *
     * @param navigationHelper the navigationHelper to set
     */
    public void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * Create a new local User and save the data (in moment by xstream)
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
        if (securityQuestion != null && !securityQuestion.isAnswerCorrect(securityAnswer)) {
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
            logger.debug("User account already exists for e-mail address '{}'.", NetTools.scrambleEmailAddress(email));
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
                    .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
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
        logger.debug("Failed user registration attempt from {}, e-mail '{}'", NetTools.scrambleIpAddress(ipAddress),
                NetTools.scrambleEmailAddress(email));
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
            User user = DataManager.getInstance().getDao().getUserByEmail(email);
            if (user != null && !user.isActive()) {
                if (activationKey.equals(user.getActivationKey())) {
                    // Activate user
                    user.setActivationKey(null);
                    user.setActive(true);
                    if (DataManager.getInstance().getDao().updateUser(user)) {
                        Messages.info(ViewerResourceBundle.getTranslation("user_accountActivationSuccess", null));
                        logger.debug("User account successfully activated: " + user.getEmail());
                    } else {
                        Messages.error(ViewerResourceBundle.getTranslation("errSave", null));
                    }
                } else {
                    logger.debug("Activation key mismatch (expected: '" + user.getActivationKey() + "' (received: '" + activationKey + "').");
                    Messages.error(ViewerResourceBundle.getTranslation("user_accountActivationWrongData", null));
                }
            } else {
                logger.debug("User not found or account already activated: " + email);
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
            HttpServletRequest request = BeanUtils.getRequest();
            this.redirectUrl = ViewHistory.getCurrentView(request)
                    .map(path -> ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + path.getCombinedPrettyfiedUrl())
                    .orElse("");
        }
        logger.trace("login");
        if (provider != null) {
            provider.login(email, password).thenAccept(result -> completeLogin(provider, result));
        }

        return null;
    }

    /**
     * 
     * @param provider
     * @param result
     * @throws IllegalStateException
     */
    private void completeLogin(IAuthenticationProvider provider, LoginResult result) {
        HttpServletResponse response = result.getResponse();
        HttpServletRequest request = result.getRequest();
        try {

            Optional<User> oUser = result.getUser().filter(u -> u.isActive() && !u.isSuspended());

            if (result.isRefused()) {
                Messages.error("errLoginWrong");
            } else if (result.getUser().map(u -> !u.isActive()).orElse(false)) {
                Messages.error("errLoginInactive");
            } else if (result.getUser().map(u -> u.isSuspended()).orElse(false)) {
                Messages.error("errLoginSuspended");
            } else if (oUser.isPresent()) { //login successful
                try {
                    User user = oUser.get();
                    if (this.user != null) {
                        if (this.user.equals(user)) {
                            logger.debug("User already logged in");
                            return;
                        }
                        // Exception if different user logged in
                        throw new AuthenticationProviderException("errLoginError");
                    }
                    wipeSession(request);
                    DataManager.getInstance().getBookmarkManager().addSessionBookmarkListToUser(user, request);
                    // Update last login
                    user.setLastLogin(new Date());
                    if (!DataManager.getInstance().getDao().updateUser(user)) {
                        logger.error("Could not update user in DB.");
                    }
                    setUser(user);
                    if (request != null && request.getSession(false) != null) {
                        request.getSession(false).setAttribute("user", user);
                    }
                    if (response != null && StringUtils.isNotEmpty(redirectUrl)) {
                        logger.trace("Redirecting to {}", redirectUrl);
                        String redirectUrl = this.redirectUrl;
                        this.redirectUrl = "";
                        response.sendRedirect(redirectUrl);
                    } else if (response != null) {
                        Optional<ViewerPath> currentPath = ViewHistory.getCurrentView(request);
                        if (currentPath.isPresent()) {
                            logger.trace("Redirecting to current url " + currentPath.get().getCombinedPrettyfiedUrl());
                            response.sendRedirect(
                                    ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + currentPath.get().getCombinedPrettyfiedUrl());
                        } else {
                            logger.trace("Redirecting to start page");
                            response.sendRedirect(ServletUtils.getServletPathWithHostAsUrlFromRequest(request));
                        }
                    }
                    SearchHelper.updateFilterQuerySuffix(request);

                    // Add this user to configured groups
                    if (provider.getAddUserToGroups() != null && !provider.getAddUserToGroups().isEmpty()) {
                        Role role = DataManager.getInstance().getDao().getRole("member");
                        if (role != null) {
                            for (String groupName : provider.getAddUserToGroups()) {
                                UserGroup userGroup = DataManager.getInstance().getDao().getUserGroup(groupName);
                                if (userGroup != null && !userGroup.getMembers().contains(user)) {
                                    userGroup.addMember(user, role);
                                    logger.debug("Added user {} to user group '{}'", user.getId(), userGroup.getName());
                                }
                            }
                        }
                    }
                    this.loggedInProvider = provider;
                    return;
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
            result.setRedirected();
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
        HttpServletResponse response = BeanUtils.getResponse();
        String redirectUrl = redirect(request, response);

        user.setTranskribusSession(null);
        setUser(null);
        password = null;
        if (loggedInProvider != null) {
            loggedInProvider.logout();
            loggedInProvider = null;
        }
        try {
            wipeSession(request);
            SearchHelper.updateFilterQuerySuffix(request);
        } catch (IndexUnreachableException | PresentationException | DAOException e) {
            throw new AuthenticationProviderException(e);
        }
        try {
            request.logout();
        } catch (ServletException e) {
            logger.error(e.getMessage(), e);
        }
        request.getSession(false).invalidate();
        return redirectUrl;
    }

    /**
     * @param request
     * @param response
     * @throws AuthenticationProviderException
     */
    private String redirect(HttpServletRequest request, HttpServletResponse response) throws AuthenticationProviderException {
        Optional<ViewerPath> oCurrentPath = ViewHistory.getCurrentView(request);
        if (StringUtils.isNotEmpty(redirectUrl)) {
            if ("#".equals(redirectUrl)) {
                logger.trace("Stay on current page");
            }
            logger.trace("Redirecting to {}", redirectUrl);
            String redirectUrl = this.redirectUrl;
            this.redirectUrl = "";
            //            Messages.info("logoutSuccessful");

            // Do not redirect to user backend pages because LoginFilter won't work here for some reason
            String servletPath = BeanUtils.getServletPathWithHostAsUrlFromJsfContext();
            if (redirectUrl.length() < servletPath.length() || !LoginFilter.isRestrictedUri(redirectUrl.substring(servletPath.length()))) {
                return redirectUrl;
            }
        } else if (oCurrentPath.isPresent()) {
            ViewerPath currentPath = oCurrentPath.get();
            if (LoginFilter.isRestrictedUri(currentPath.getCombinedUrl())) {
                logger.trace("Redirecting to start page");
                return "pretty:index";
            }
            logger.trace("Redirecting to current url {}", currentPath.getCombinedPrettyfiedUrl());
            String redirect = currentPath.getCombinedPrettyfiedUrl();
            return redirect;
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
     * Removes the user and permission attributes from the session.
     *
     * @param request a {@link javax.servlet.http.HttpServletRequest} object.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void wipeSession(HttpServletRequest request) throws IndexUnreachableException, PresentationException, DAOException {
        logger.trace("wipeSession");
        if (request != null) {
            HttpSession session = request.getSession(false);
            if (session == null) {
                return;
            }
            session.removeAttribute("user");

            //        // Remove priv maps
            Enumeration<String> attributeNames = session.getAttributeNames();
            Set<String> attributesToRemove = new HashSet<>();
            while (attributeNames.hasMoreElements()) {
                String attribute = attributeNames.nextElement();
                if (attribute.startsWith(IPrivilegeHolder._PRIV_PREFIX)) {
                    attributesToRemove.add(attribute);

                }
            }
            if (!attributesToRemove.isEmpty()) {
                for (String attribute : attributesToRemove) {
                    session.removeAttribute(attribute);
                    logger.trace("Removed session attribute: {}", attribute);
                }
            }

            try {
                BeanUtils.getCmsBean().invalidate();
                BeanUtils.getActiveDocumentBean().resetAccess();
            } catch (Throwable e) {
            }

            this.authenticationProviders = null;

        }
    }

    /**
     * <p>
     * saveUserAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveUserAction() throws DAOException {
        try {
            if (user != null) {
                // Retrieving a new user from the DB overrides the current object and resets the field, so save a copy
                User copy = user.clone();
                // Copy of the copy contains the previous nickname, in case the chosen one is already taken
                if (user.getCopy() != null) {
                    copy.setCopy(user.getCopy().clone());
                } else {
                    logger.warn("No backup user object found, cannot restore data in case of cancellation / error.");
                }
                // Do not allow the same nickname being used for multiple users
                User nicknameOwner = DataManager.getInstance().getDao().getUserByNickname(user.getNickName()); // This basically resets all changes
                if (nicknameOwner != null && nicknameOwner.getId() != user.getId()) {
                    Messages.error(ViewerResourceBundle.getTranslation("user_nicknameTaken", null).replace("{0}", user.getNickName().trim()));
                    user = copy;
                    if (copy.getCopy() != null) {
                        user.setNickName(copy.getCopy().getNickName());
                    }
                    return "pretty:userEdit";
                }
                user = copy;
                if (StringUtils.isNotBlank(passwordOne)) {
                    if (!PasswordValidator.validatePassword(passwordOne)) {
                        Messages.error("password_errInvalid");
                        return "pretty:userEdit";
                    }
                    if (!passwordOne.equals(passwordTwo)) {
                        Messages.error("user_passwordMismatch");
                        return "pretty:userEdit";
                    }
                    user.setNewPassword(passwordOne);
                    logger.debug("Set new password for user {}", user.getEmail());
                    Messages.info("user_passwordChanged");
                }
                if (DataManager.getInstance().getDao().updateUser(user)) {
                    user.setCopy(null);
                    logger.debug("User '" + user.getEmail() + "' updated successfully.");
                    Messages.info("user_saveSuccess");
                    return "pretty:user";
                }
                Messages.error("errSave");
            }
        } finally {
            resetPasswordFields();
        }

        return "pretty:userEdit";
    }

    /**
     * Returns a list of all existing users (minus the superusers and the current user).
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<User> getAllUsers() throws DAOException {
        List<User> ret = new ArrayList<>();

        for (User user : DataManager.getInstance().getDao().getAllUsers(true)) {
            if (!user.isSuperuser() && !user.equals(getUser())) {
                ret.add(user);
            }
        }

        return ret;
    }

    /**
     * 
     * @param user
     * @return
     */
    private boolean sendActivationEmail(User user) {
        if (StringUtils.isNotEmpty(user.getEmail())) {
            // Generate and save the activation key, if not yet set
            if (user.getActivationKey() == null) {
                user.setActivationKey(StringTools.generateMD5(UUID.randomUUID() + String.valueOf(System.currentTimeMillis())));
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
                    .replace("{2}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));

            // Send
            try {
                if (NetTools.postMail(Collections.singletonList(user.getEmail()),
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
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (user != null && !user.isOpenIdUser()) {
            if (user.isActive()) {
                user.setActivationKey(StringTools.generateMD5(String.valueOf(System.currentTimeMillis())));
                String requesterIp = "???";
                if (FacesContext.getCurrentInstance().getExternalContext() != null
                        && FacesContext.getCurrentInstance().getExternalContext().getRequest() != null) {
                    requesterIp = NetTools.getIpAddress((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
                }
                String resetUrl = navigationHelper.getApplicationUrl() + "user/resetpw/" + user.getEmail() + "/" + user.getActivationKey() + "/";

                if (DataManager.getInstance().getDao().updateUser(user)) {
                    try {
                        if (NetTools.postMail(Collections.singletonList(email),
                                ViewerResourceBundle.getTranslation("user_retrieveAccountConfirmationEmailSubject", null),
                                ViewerResourceBundle.getTranslation("user_retrieveAccountConfirmationEmailBody", null)
                                        .replace("{0}", requesterIp)
                                        .replace("{1}", resetUrl))) {
                            email = null;
                            Messages.info("user_retrieveAccountConfirmationEmailMessage");
                            return "user?faces-redirect=true";
                        }
                        logger.error("Could not send passwort reset link e-mail to: " + user.getEmail());
                    } catch (UnsupportedEncodingException e) {
                        logger.error(e.getMessage(), e);
                    } catch (MessagingException e) {
                        logger.error(e.getMessage(), e);
                    }
                }
                Messages.error(ViewerResourceBundle.getTranslation("user_retrieveAccountError", null)
                        .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
                return "userRetrieveAccount";
            }

            // Send new activation mail if not yet activated
            if (sendActivationEmail(user)) {
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
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (user != null && user.isActive() && !user.isOpenIdUser()) {
            if (StringUtils.isNotEmpty(activationKey) && activationKey.equals(user.getActivationKey())) {
                String newPassword = StringTools.generateMD5(String.valueOf(System.currentTimeMillis())).substring(0, 8);
                user.setNewPassword(newPassword);
                user.setActivationKey(null);
                try {
                    if (NetTools.postMail(Collections.singletonList(email),
                            ViewerResourceBundle.getTranslation("user_retrieveAccountNewPasswordEmailSubject", null),
                            ViewerResourceBundle.getTranslation("user_retrieveAccountNewPasswordEmailBody", null).replace("{0}", newPassword))
                            && DataManager.getInstance().getDao().updateUser(user)) {
                        email = null;
                        Messages.info("user_retrieveAccountPasswordResetMessage");
                        return "user?faces-redirect=true";
                    }
                    logger.error("Could not send new password e-mail to: " + user.getEmail());
                } catch (UnsupportedEncodingException e) {
                    logger.error(e.getMessage(), e);
                } catch (MessagingException e) {
                    logger.error(e.getMessage(), e);
                }
            }
            Messages.error(ViewerResourceBundle.getTranslation("user_retrieveAccountError", null)
                    .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
            return "user?faces-redirect=true";
        }

        Messages.error("user_retrieveAccountUserNotFound");
        return "user?faces-redirect=true";
    }

    /**
     * Returns saved searches for the logged in user.
     *
     * @should return searches for correct user
     * @should return null if no user logged in
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Search> getSearches() throws DAOException {
        if (user != null) {
            return DataManager.getInstance().getDao().getSearches(user);
        }

        return null;
    }

    /**
     * Deletes the given persistent user search.
     *
     * @param search a {@link io.goobi.viewer.model.search.Search} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteSearchAction(Search search) throws DAOException {
        if (search != null) {
            logger.debug("Deleting search query: " + search.getId());
            if (DataManager.getInstance().getDao().deleteSearch(search)) {
                String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteSuccess", null);
                Messages.info(msg.replace("{0}", search.getName()));
            } else {
                String msg = ViewerResourceBundle.getTranslation("savedSearch_deleteFailure", null);
                Messages.error(msg.replace("{0}", search.getName()));
            }
        }

        return "";
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
        } catch (HTTPException e) {
            if (e.getCode() == 401 || e.getCode() == 403) {
                Messages.error("transkribus_loginDataError");
            }
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
     * createFeedback.
     * </p>
     */
    public void createFeedback() {
        logger.trace("createFeedback");
        lastName = null;
        securityAnswer = null;
        securityQuestion = null;

        String url = FacesContext.getCurrentInstance().getExternalContext().getRequestHeaderMap().get("referer");
        feedback = new Feedback();
        if (user != null) {
            feedback.setEmail(user.getEmail());
        }
        if (StringUtils.isEmpty(url)) {
            url = navigationHelper.getCurrentPrettyUrl();
        }
        feedback.setUrl(url);
    }

    /**
     * <p>
     * submitFeedbackAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String submitFeedbackAction() {
        // Check whether the security question has been answered correct, if configured
        if (securityQuestion != null && !securityQuestion.isAnswerCorrect(securityAnswer)) {
            Messages.error("user__security_question_wrong");
            logger.debug("Wrong security question answer.");
            return "";
        }
        // Check whether the invisible field lastName has been filled (real users cannot do that)
        if (StringUtils.isNotEmpty(lastName)) {
            logger.debug("Honeypot field entry: {}", lastName);
            return "";
        }
        try {
            if (NetTools.postMail(Collections.singletonList(DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()),
                    feedback.getEmailSubject("feedbackEmailSubject"), feedback.getEmailBody("feedbackEmailBody"))) {
                Messages.info("feedbackSubmitted");
            } else {
                logger.error("{} could not send feedback.", feedback.getEmail());
                Messages.error(ViewerResourceBundle.getTranslation("errFeedbackSubmit", null)
                        .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            Messages.error(ViewerResourceBundle.getTranslation("errFeedbackSubmit", null)
                    .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
        } catch (MessagingException e) {
            logger.error(e.getMessage(), e);
            Messages.error(ViewerResourceBundle.getTranslation("errFeedbackSubmit", null)
                    .replace("{0}", DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
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
        if (this.authenticationProviders == null) {
            this.authenticationProviders = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        }
        return this.authenticationProviders;
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
     * loginTest.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String loginTest() {
        user = new User();
        return null;
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
     * Selects a random security question from configured list and sets <code>currentSecurityQuestion</code> to it.
     * 
     * @should not reset securityQuest if not yet answered
     */
    public boolean resetSecurityQuestion() {
        List<SecurityQuestion> questions = DataManager.getInstance().getConfiguration().getSecurityQuestions();
        if (questions.isEmpty()) {
            return true;
        }
        if (securityQuestion != null && !securityQuestion.isAnswered()) {
            // Do not reset if not set set or not yet answered
            return true;
        }

        Random random = new Random();
        securityQuestion = questions.get(random.nextInt(questions.size()));

        return true;
    }

    /**
     * @return the securityQuestion
     */
    public SecurityQuestion getSecurityQuestion() {
        return securityQuestion;
    }

    /**
     * @return the securityAnswer
     */
    public String getSecurityAnswer() {
        return securityAnswer;
    }

    /**
     * @param securityAnswer the securityAnswer to set
     */
    public void setSecurityAnswer(String securityAnswer) {
        this.securityAnswer = securityAnswer;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
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
     * Getter for the field <code>feedback</code>.
     * </p>
     *
     * @return the feedback
     */
    public Feedback getFeedback() {
        return feedback;
    }

    /**
     * <p>
     * Setter for the field <code>feedback</code>.
     * </p>
     *
     * @param feedback the feedback to set
     */
    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
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
     * Check if the current user is required to agree to the terms of use
     * 
     * @return true if  termsOfUse is active, a user is logged in and {@link User#isAgreedToTermsOfUse()} returns false for this user
     */
    public boolean mustAgreeToTermsOfUse() {
        if(this.user != null && !this.user.isAgreedToTermsOfUse()) {
            try {
                boolean active = DataManager.getInstance().getDao().isTermsOfUseActive();
                return active;
            } catch(DAOException e) {
                logger.error("Unable to query terms of use active state" , e);
            }
        }
        return false;
    }
    
    public void agreeToTermsOfUse() throws DAOException {
        if(this.user != null) {
            this.user.setAgreedToTermsOfUse(true);
            DataManager.getInstance().getDao().updateUser(this.user);
        }
    }
    
    public void rejectTermsOfUse() throws DAOException {
        if(this.user != null) {
            this.user.setAgreedToTermsOfUse(false);
            DataManager.getInstance().getDao().updateUser(this.user);
        }
    }
}
