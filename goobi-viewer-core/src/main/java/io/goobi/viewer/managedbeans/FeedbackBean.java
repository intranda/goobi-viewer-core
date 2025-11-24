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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.faces.validators.EmailValidator;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.email.EMailSender;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.viewer.Feedback;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.mail.MessagingException;

@Named
@RequestScoped
public class FeedbackBean implements Serializable {

    private static final long serialVersionUID = 9100712106412532312L;
    private static final Logger logger = LogManager.getLogger(FeedbackBean.class);

    @Inject
    private UserBean userBean;
    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private CaptchaBean captchaBean;
    @Inject
    private EMailSender emailSender;
    @Inject
    private FacesContext facesContext;

    private final Configuration config;

    private Feedback feedback;
    private User user;
    private String lastName;

    public FeedbackBean() {
        this.config = DataManager.getInstance().getConfiguration();
    }

    public FeedbackBean(Configuration config) {
        this.config = config;
    }

    @PostConstruct
    public void init() {
        this.user = userBean.getUser();
        createFeedback();
    }

    /**
     * <p>
     * createFeedback.
     * </p>
     */
    public void createFeedback() {

        this.lastName = null;

        feedback = new Feedback();
        if (user != null) {
            feedback.setSenderAddress(user.getEmail());
            feedback.setName(user.getDisplayName());
        }
        feedback.setRecipientAddress(this.config.getDefaultFeedbackEmailAddress());
        setFeedbackUrl(true);
    }

    private String getReferrerUrl() {
        return Optional.ofNullable(facesContext)
                .map(FacesContext::getExternalContext)
                .map(ExternalContext::getRequestHeaderMap)
                .map(map -> map.get("referer"))
                .orElse(null);
    }

    /**
     * <p>
     * submitFeedbackAction.
     * </p>
     *
     * @param setCurrentUrl
     * @return a {@link java.lang.String} object.
     */
    public String submitFeedbackAction(boolean setCurrentUrl) {
        // Check whether the security question has been answered correct, if configured
        if (captchaBean != null && !captchaBean.checkAnswer()) {
            Messages.error("user__security_question_wrong");
            return "";
        }

        // Check whether the invisible field lastName has been filled (real users cannot do that)
        if (StringUtils.isNotEmpty(lastName)) {
            logger.debug("Honeypot field entry: {}", lastName);
            return "";
        }
        if (!EmailValidator.validateEmailAddress(this.feedback.getSenderAddress(), false)) {
            Messages.error("email_errlnvalid");
            logger.debug("Invalid email: {}", this.feedback.getSenderAddress());
            return "";
        }
        if (StringUtils.isBlank(feedback.getName())) {
            Messages.error("errFeedbackNameRequired");
            return "";
        }
        if (StringUtils.isBlank(feedback.getMessage())) {
            Messages.error("errFeedbackMessageRequired");
            return "";
        }
        if (StringUtils.isBlank(feedback.getRecipientAddress())) {
            Messages.error("errFeedbackRecipientRequired");
            return "";
        }

        //set current url to feedback
        setFeedbackUrl(setCurrentUrl);

        try {
            if (emailSender.postMail(Collections.singletonList(feedback.getRecipientAddress()), null, null,
                    Collections.singletonList(feedback.getSenderAddress()), feedback.getEmailSubject("feedbackEmailSubject"),
                    feedback.getEmailBody("feedbackEmailBody"))) {
                // Send confirmation to sender
                if (StringUtils.isNotEmpty(feedback.getSenderAddress())
                        && !emailSender.postMail(Collections.singletonList(feedback.getSenderAddress()), null, null, null,
                                feedback.getEmailSubject("feedbackEmailSubjectSender"), feedback.getEmailBody("feedbackEmailBody"))) {
                    logger.warn("Could not send feedback confirmation to sender.");
                }
                Messages.info("feedbackSubmitted");
            } else {
                logger.error("{} could not send feedback.", feedback.getSenderAddress());
                Messages.error(ViewerResourceBundle.getTranslation("errFeedbackSubmit", null)
                        .replace("{0}", feedback.getRecipientAddress()));
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage(), e);
            Messages.error(ViewerResourceBundle.getTranslation("errFeedbackSubmit", null)
                    .replace("{0}", feedback.getRecipientAddress()));
        }
        //eventually always create a new feedback object to erase prior inputs
        createFeedback();
        return "";
    }

    private void setFeedbackUrl(boolean setCurrentUrl) {
        if (setCurrentUrl && navigationHelper != null) {
            feedback.setUrl(navigationHelper.getCurrentPrettyUrl());
        } else if (navigationHelper != null) {
            feedback.setUrl(navigationHelper.getPreviousViewUrl());
        } else {
            feedback.setUrl(getReferrerUrl());
        }
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

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public boolean isLoggedIn() {
        return this.userBean.isLoggedIn();
    }

    public CaptchaBean getCaptchaBean() {
        return this.captchaBean;
    }

    /**
     * @param captchaBean the captchaBean to set
     */
    void setCaptchaBean(CaptchaBean captchaBean) {
        this.captchaBean = captchaBean;
    }

    /**
     * @return the userBean
     */
    UserBean getUserBean() {
        return userBean;
    }

    /**
     * @param userBean the userBean to set
     */
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /**
     * @return the navigationHelper
     */
    NavigationHelper getNavigationHelper() {
        return navigationHelper;
    }

    /**
     * @param navigationHelper the navigationHelper to set
     */
    void setNavigationHelper(NavigationHelper navigationHelper) {
        this.navigationHelper = navigationHelper;
    }

    /**
     * @return the emailSender
     */
    EMailSender getEmailSender() {
        return emailSender;
    }

    /**
     * @param emailSender the emailSender to set
     */
    void setEmailSender(EMailSender emailSender) {
        this.emailSender = emailSender;
    }

    /**
     * @param facesContext the facesContext to set
     */
    void setFacesContext(FacesContext facesContext) {
        this.facesContext = facesContext;
    }
}
