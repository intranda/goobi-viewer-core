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
package de.intranda.digiverso.presentation.managedbeans;

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
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

import org.apache.commons.lang.StringUtils;
import org.jdom2.JDOMException;
import org.primefaces.expression.impl.ThisExpressionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.TranskribusUtils;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.HTTPException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.faces.validators.PasswordValidator;
import de.intranda.digiverso.presentation.filters.LoginFilter;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.search.Search;
import de.intranda.digiverso.presentation.model.search.SearchHelper;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.authentication.AuthenticationProviderException;
import de.intranda.digiverso.presentation.model.security.authentication.IAuthenticationProvider;
import de.intranda.digiverso.presentation.model.security.authentication.LocalAuthenticationProvider;
import de.intranda.digiverso.presentation.model.security.authentication.LoginResult;
import de.intranda.digiverso.presentation.model.security.user.User;
import de.intranda.digiverso.presentation.model.urlresolution.ViewHistory;
import de.intranda.digiverso.presentation.model.viewer.Feedback;
import de.intranda.digiverso.presentation.servlets.utils.ServletUtils;

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
    private String password;
    private String activationKey;
    /** Selected OpenID Connect provider. */
    private IAuthenticationProvider authenticationProvider;
    private List<IAuthenticationProvider> authenticationProviders;

    // Passwords for creating an new local user account
    private String passwordOne = "";
    private String passwordTwo = "";
    private String redirectUrl = null;
    private Feedback feedback;
    private String transkribusUserName;
    private String transkribusPassword;

    private CompletableFuture<Optional<User>> loginFuture = null;

    /** Empty constructor. */
    public UserBean() {
        // the emptiness inside
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
     * @throws DAOException
     */
    public String createNewUserAccount() throws DAOException {
        if (nickName != null && DataManager.getInstance().getDao().getUserByNickname(nickName) != null) {
            // Do not allow the same nickname being used for multiple users
            Messages.error(Helper.getTranslation("user_nicknameTaken", null).replace("{0}", nickName.trim()));
        } else if (DataManager.getInstance().getDao().getUserByEmail(email) != null) {
            // Do not allow the same email address being used for multiple users
            Messages.error("newUserExist");
            logger.debug("User account already exists for '" + email + "'.");
        } else if (StringUtils.isNotBlank(passwordOne) && passwordOne.equals(passwordTwo)) {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setNickName(nickName);
            newUser.setNewPassword(passwordOne);
            resetPasswordFields();
            if (sendActivationEmail(newUser)) {
                // Only attempt to persist the new user if the activation email could be sent
                if (DataManager.getInstance().getDao().addUser(newUser)) {
                    String msg = Helper.getTranslation("user_accountCreated", null);
                    Messages.info(msg.replace("{0}", email));
                    logger.debug("User account created for '" + email + "'.");
                } else {
                    Messages.error("errSave");
                }
                return "user?faces-redirect=true";
            }
            Messages.error(Helper.getTranslation("errSendEmail", null).replace("{0}",
                    DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
        } else {
            Messages.error("user_passwordMismatch");
        }

        return "pretty:createUserAccount";
    }

    /**
     * 
     * @return
     * @throws DAOException
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
                        Messages.info(Helper.getTranslation("user_accountActivationSuccess", null));
                        logger.debug("User account successfully activated: " + user.getEmail());
                    } else {
                        Messages.error(Helper.getTranslation("errSave", null));
                    }
                } else {
                    logger.debug("Activation key mismatch (expected: '" + user.getActivationKey() + "' (received: '" + activationKey + "').");
                    Messages.error(Helper.getTranslation("user_accountActivationWrongData", null));
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
     * @throws AuthenticationProviderException If an error occured while logging in
     * @throws IllegalStateException If a user is already logged in
     * @return the url mapping to navigate to
     * @throws ExecutionException
     * @throws InterruptedException
     *
     */
    public String login() throws AuthenticationProviderException, IllegalStateException, InterruptedException, ExecutionException {
        if (getUser() != null) {
            throw new IllegalStateException("errAlreadyLoggedIn");
        }
        if ("#".equals(this.redirectUrl)) {
            HttpServletRequest request = BeanUtils.getRequest();
            this.redirectUrl = ViewHistory.getCurrentView(request)
                    .map(path -> ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + path.getCombinedPrettyfiedUrl())
                    .orElse("");
        }
        logger.trace("login");
        if (getAuthenticationProvider() != null) {
            getAuthenticationProvider().login(email, password).thenAccept(result -> completeLogin(result));
        }
        return null;
    }

    private void completeLogin(LoginResult result) {
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
                        wipeSession(request);
                        DataManager.getInstance().getBookshelfManager().addSessionBookshelfToUser(user, request);
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
                            logger.trace("Redirecting to user page");
                            response.sendRedirect(ServletUtils.getServletPathWithHostAsUrlFromRequest(request) + "/user/");
                        }
                        SearchHelper.updateFilterQuerySuffix(request);
                        return;
                    } catch (DAOException | IOException | IndexUnreachableException | PresentationException e) {
                        //user may login, but setting up viewer account failed
                        getAuthenticationProvider().logout();
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
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public String logout() throws AuthenticationProviderException {
        logger.trace("logout");
        user.setTranskribusSession(null);
        setUser(null);
        password = null;
        if (getAuthenticationProvider() != null) {
            getAuthenticationProvider().logout();
        }
        setAuthenticationProvider(null);

        HttpServletRequest request = BeanUtils.getRequest();
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

        if (StringUtils.isNotEmpty(redirectUrl)) {
            if ("#".equals(redirectUrl)) {
                logger.trace("Stay on current page");
                return "";
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
        }

        //        Messages.info("logoutSuccessful");
        return "pretty:user";
    }

    /**
     * Removes the user and permission attributes from the session.
     * 
     * @param request
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
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
            } catch (Throwable e) {
            }

            this.authenticationProviders = null;

        }
    }

    /**
     * 
     * @return
     * @throws DAOException
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
                    Messages.error(Helper.getTranslation("user_nicknameTaken", null).replace("{0}", user.getNickName().trim()));
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
     * @return
     * @throws DAOException
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
                user.setActivationKey(Helper.generateMD5(String.valueOf(System.currentTimeMillis())));
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
            sb.append(Helper.getTranslation("user_activationEmailBody", null).replace("{0}", baseUrl).replace("{1}", activationUrl).replace("{2}",
                    DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));

            // Send
            try {
                if (Helper.postMail(Collections.singletonList(user.getEmail()), Helper.getTranslation("user_activationEmailSubject", null),
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
     * @return
     * @throws DAOException
     */
    public String sendPasswordResetLinkAction() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (user != null && !user.isOpenIdUser()) {
            if (user.isActive()) {
                user.setActivationKey(Helper.generateMD5(String.valueOf(System.currentTimeMillis())));
                String requesterIp = "???";
                if (FacesContext.getCurrentInstance().getExternalContext() != null
                        && FacesContext.getCurrentInstance().getExternalContext().getRequest() != null) {
                    requesterIp = Helper.getIpAddress((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest());
                }
                String resetUrl = navigationHelper.getApplicationUrl() + "user/resetpw/" + user.getEmail() + "/" + user.getActivationKey() + "/";

                if (DataManager.getInstance().getDao().updateUser(user)) {
                    try {
                        if (Helper.postMail(Collections.singletonList(email),
                                Helper.getTranslation("user_retrieveAccountConfirmationEmailSubject", null),
                                Helper.getTranslation("user_retrieveAccountConfirmationEmailBody", null).replace("{0}", requesterIp).replace("{1}",
                                        resetUrl))) {
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
                Messages.error(Helper.getTranslation("user_retrieveAccountError", null).replace("{0}",
                        DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
                return "userRetrieveAccount";
            }

            // Send new activation mail if not yet activated
            if (sendActivationEmail(user)) {
                Messages.info(Helper.getTranslation("user_activationEmailReSent", null));
            } else {
                Messages.error(Helper.getTranslation("errSendEmail", null));
            }
            return "pretty:user";
        }

        Messages.error("user_retrieveAccountUserNotFound");
        return "userRetrieveAccount";
    }

    /**
     * Generates a new user password if the key is correct.
     *
     * @return
     * @throws DAOException
     */
    public String resetPasswordAction() throws DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only reset password for non-OpenID user accounts, do not reset not yet activated accounts
        if (user != null && user.isActive() && !user.isOpenIdUser()) {
            if (StringUtils.isNotEmpty(activationKey) && activationKey.equals(user.getActivationKey())) {
                String newPassword = Helper.generateMD5(String.valueOf(System.currentTimeMillis())).substring(0, 8);
                user.setNewPassword(newPassword);
                user.setActivationKey(null);
                try {
                    if (Helper.postMail(Collections.singletonList(email), Helper.getTranslation("user_retrieveAccountNewPasswordEmailSubject", null),
                            Helper.getTranslation("user_retrieveAccountNewPasswordEmailBody", null).replace("{0}", newPassword))
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
            Messages.error(Helper.getTranslation("user_retrieveAccountError", null).replace("{0}",
                    DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
            return "user?faces-redirect=true";
        }

        Messages.error("user_retrieveAccountUserNotFound");
        return "user?faces-redirect=true";
    }

    /**
     * Returns saved searches for the logged in user.
     *
     * @return
     * @throws DAOException
     * @should return searches for correct user
     * @should return null if no user logged in
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
     * @param search
     * @return
     * @throws DAOException
     */
    public String deleteSearchAction(Search search) throws DAOException {
        if (search != null) {
            logger.debug("Deleting search query: " + search.getId());
            if (DataManager.getInstance().getDao().deleteSearch(search)) {
                String msg = Helper.getTranslation("savedSearch_deleteSuccess", null);
                Messages.info(msg.replace("{0}", search.getName()));
            } else {
                String msg = Helper.getTranslation("savedSearch_deleteFailure", null);
                Messages.error(msg.replace("{0}", search.getName()));
            }
        }

        return "";
    }

    /**
     * 
     * @return
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

    public void createFeedback() {
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

    public String submitFeedbackAction() {
        try {
            if (Helper.postMail(Collections.singletonList(DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()),
                    feedback.getEmailSubject("feedbackEmailSubject"), feedback.getEmailBody("feedbackEmailBody"))) {
                Messages.info("feedbackSubmitted");
            } else {
                logger.error("{} could not send feedback.", feedback.getEmail());
                Messages.error(Helper.getTranslation("errFeedbackSubmit", null).replace("{0}",
                        DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
            }
        } catch (UnsupportedEncodingException e) {
            logger.error(e.getMessage(), e);
            Messages.error(Helper.getTranslation("errFeedbackSubmit", null).replace("{0}",
                    DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
        } catch (MessagingException e) {
            logger.error(e.getMessage(), e);
            Messages.error(Helper.getTranslation("errFeedbackSubmit", null).replace("{0}",
                    DataManager.getInstance().getConfiguration().getFeedbackEmailAddress()));
        }
        return "";
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(User user) {
        this.user = user;
    }

    /**
     * @return the nickName
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * @param nickName the nickName to set
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isLoggedIn() {
        return user != null && user.isActive() && !user.isSuspended();
    }

    public boolean isAdmin() {
        return user != null && user.isSuperuser();
    }

    /**
     *
     * @return
     */
    public boolean isUserRegistrationEnabled() {
        return DataManager.getInstance().getConfiguration().isUserRegistrationEnabled();
    }

    /**
     *
     * @return
     */
    public boolean isShowOpenId() {
        return DataManager.getInstance().getConfiguration().isShowOpenIdConnect();
    }

    public synchronized List<IAuthenticationProvider> getAuthenticationProviders() {
        if (this.authenticationProviders == null) {
            this.authenticationProviders = DataManager.getInstance().getConfiguration().getAuthenticationProviders();
        }
        return this.authenticationProviders;
    }

    public IAuthenticationProvider getLocalAuthenticationProvider() {
        return getProvidersOfType("local").stream().findFirst().orElse(null);
    }

    public void setAuthenticationProvider(IAuthenticationProvider provider) {
        this.authenticationProvider = provider;
    }

    public IAuthenticationProvider getAuthenticationProvider() {
        return authenticationProvider;
    }

    public String loginTest() {
        user = new User();
        return null;
    }

    public String getPasswordOne() {
        return this.passwordOne;
    }

    public void setPasswordOne(String passwordOne) {
        this.passwordOne = passwordOne;
    }

    public String getPasswordTwo() {
        return this.passwordTwo;
    }

    public void setPasswordTwo(String passwordTwo) {
        this.passwordTwo = passwordTwo;
    }

    public void resetPasswordFields() {
        passwordOne = "";
        passwordTwo = "";
    }

    /**
     * @return the redirectUrl
     */
    public String getRedirectUrl() {
        return redirectUrl;
    }

    /**
     * @param redirectUrl the redirectUrl to set
     */
    public void setRedirectUrl(String redirectUrl) {
        if (!"RES_NOT_FOUND".equals(redirectUrl)) {
            this.redirectUrl = redirectUrl;
            logger.trace("Redirect URL: {}", redirectUrl);
        }
    }

    /**
     * @return the activationKey
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * @param activationKey the activationKey to set
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * @return the feedback
     */
    public Feedback getFeedback() {
        return feedback;
    }

    /**
     * @param feedback the feedback to set
     */
    public void setFeedback(Feedback feedback) {
        this.feedback = feedback;
    }

    /**
     * @return the transkribusUserName
     */
    public String getTranskribusUserName() {
        return transkribusUserName;
    }

    /**
     * @param transkribusUserName the transkribusUserName to set
     */
    public void setTranskribusUserName(String transkribusUserName) {
        this.transkribusUserName = transkribusUserName;
    }

    /**
     * @return the transkribusPassword
     */
    public String getTranskribusPassword() {
        return transkribusPassword;
    }

    /**
     * @param transkribusPassword the transkribusPassword to set
     */
    public void setTranskribusPassword(String transkribusPassword) {
        this.transkribusPassword = transkribusPassword;
    }

    public boolean userEquals(long id) {
        return getUser().getId().equals(id);
    }

    public boolean hasProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().anyMatch(provider -> type.equalsIgnoreCase(provider.getType()));
        } else {
            return false;
        }
    }

    public List<IAuthenticationProvider> getProvidersOfType(String type) {
        if (type != null) {
            return getAuthenticationProviders().stream().filter(provider -> type.equalsIgnoreCase(provider.getType())).collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public int getNumberOfProviderTypes() {
        return getAuthenticationProviders().stream().collect(Collectors.groupingBy(IAuthenticationProvider::getType, Collectors.counting())).size();
    }
    
    public boolean isAllowPasswordChange() {
        return getAuthenticationProvider() != null && getAuthenticationProvider().allowsPasswordChange();
    }
}
