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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.model.email.EMailSender;
import io.goobi.viewer.model.security.SecurityQuestion;
import io.goobi.viewer.model.security.user.User;
import jakarta.mail.MessagingException;

class FeedbackBeanTest {

    private static final String USER_NAME = "Karla";
    private static final String SENDER_ADDRESS = "karla@mustermann.de";
    private static final String RECIPIENT_ADDRESS = "viewer@goobi.io";
    private static final String CURRENT_VIEWER_URL = "https://viewer.goobi.io/some/page/";
    private static final String PREVIOUS_VIEWER_URL = "https://viewer.goobi.io/some/other/page/";

    private static final String FEEDBACK_MESSAGE = "Feedback message";

    FeedbackBean bean;

    @BeforeEach
    public void setUp() throws UnsupportedEncodingException, MessagingException {

        NavigationHelper navigationHelper = Mockito.mock(NavigationHelper.class);
        Mockito.when(navigationHelper.getCurrentPrettyUrl()).thenReturn(CURRENT_VIEWER_URL);
        Mockito.when(navigationHelper.getPreviousViewUrl()).thenReturn(PREVIOUS_VIEWER_URL);

        SecurityQuestion question = new SecurityQuestion("What is the first letter of the alphabet", Set.of("a", "A"));
        Configuration config = Mockito.mock(Configuration.class);
        Mockito.when(config.getSecurityQuestions()).thenReturn(Collections.singletonList(question));

        CaptchaBean captchaBean = new CaptchaBean(config);// Mockito.mock(CaptchaBean.class);
        captchaBean.resetSecurityQuestion();

        EMailSender emailSender = Mockito.mock(EMailSender.class);
        Mockito.when(emailSender.postMail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(true);

        bean = new FeedbackBean();
        bean.setNavigationHelper(navigationHelper);
        bean.setCaptchaBean(captchaBean);
        bean.setEmailSender(emailSender);
        bean.setUserBean(mockUserBean(null, null));
        bean.setFacesContext(mockFacesContext(Map.of()));

    }

    @Test
    void testNoUser() throws UnsupportedEncodingException, MessagingException {
        bean.init();
        bean.getFeedback().setMessage(FEEDBACK_MESSAGE);
        bean.getFeedback().setName(USER_NAME);
        bean.getFeedback().setSenderAddress(SENDER_ADDRESS);
        bean.getFeedback().setRecipientAddress(RECIPIENT_ADDRESS);
        bean.getCaptchaBean().setSecurityAnswer("A");

        String subjectSender = bean.getFeedback().getEmailSubject("feedbackEmailSubjectSender");
        String subject = bean.getFeedback().getEmailSubject("feedbackEmailSubject");
        String body = bean.getFeedback().getEmailBody("feedbackEmailBody");

        bean.submitFeedbackAction(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> recipientCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(bean.getEmailSender(), Mockito.times(2))
                .postMail(recipientCaptor.capture(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), subjectCaptor.capture(),
                        Mockito.contains(body));
        int indexMailRecipient = recipientCaptor.getAllValues().indexOf(Arrays.asList(RECIPIENT_ADDRESS));
        int indexMailSender = recipientCaptor.getAllValues().indexOf(Arrays.asList(SENDER_ADDRESS));
        int indexSubjectRecipient = subjectCaptor.getAllValues().indexOf(subject);
        int indexSubjectSender = subjectCaptor.getAllValues().indexOf(subjectSender);
        assertTrue(indexMailRecipient > -1);
        assertTrue(indexMailSender > -1);
        assertNotEquals(indexMailRecipient, indexMailSender);
        assertEquals(indexMailRecipient, indexSubjectRecipient);
        assertEquals(indexMailSender, indexSubjectSender);
    }

    @Test
    void testUser() throws UnsupportedEncodingException, MessagingException {
        bean.setUserBean(mockUserBean(USER_NAME, SENDER_ADDRESS));
        bean.init();
        bean.getFeedback().setMessage(FEEDBACK_MESSAGE);
        bean.getFeedback().setRecipientAddress(RECIPIENT_ADDRESS);
        bean.getCaptchaBean().setSecurityAnswer("A");

        String subjectSender = bean.getFeedback().getEmailSubject("feedbackEmailSubjectSender");
        String subject = bean.getFeedback().getEmailSubject("feedbackEmailSubject");
        String body = bean.getFeedback().getEmailBody("feedbackEmailBody");

        bean.submitFeedbackAction(true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> recipientCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);

        Mockito.verify(bean.getEmailSender(), Mockito.times(2))
                .postMail(recipientCaptor.capture(), Mockito.isNull(), Mockito.isNull(), Mockito.isNull(), subjectCaptor.capture(),
                        Mockito.contains(body));
        int indexMailRecipient = recipientCaptor.getAllValues().indexOf(Arrays.asList(RECIPIENT_ADDRESS));
        int indexMailSender = recipientCaptor.getAllValues().indexOf(Arrays.asList(SENDER_ADDRESS));
        int indexSubjectRecipient = subjectCaptor.getAllValues().indexOf(subject);
        int indexSubjectSender = subjectCaptor.getAllValues().indexOf(subjectSender);
        assertTrue(indexMailRecipient > -1);
        assertTrue(indexMailSender > -1);
        assertNotEquals(indexMailRecipient, indexMailSender);
        assertEquals(indexMailRecipient, indexSubjectRecipient);
        assertEquals(indexMailSender, indexSubjectSender);
    }

    @Test
    void testWrongCaptcha() throws UnsupportedEncodingException, MessagingException {
        bean.setUserBean(mockUserBean(USER_NAME, SENDER_ADDRESS));
        bean.init();
        bean.getFeedback().setMessage(FEEDBACK_MESSAGE);
        bean.getFeedback().setRecipientAddress(RECIPIENT_ADDRESS);
        bean.getCaptchaBean().setSecurityAnswer("B");

        bean.submitFeedbackAction(true);
        Mockito.verify(bean.getEmailSender(), Mockito.times(0))
                .postMail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    }

    @Test
    void testFilledHoneypot() throws UnsupportedEncodingException, MessagingException {
        bean.setUserBean(mockUserBean(USER_NAME, SENDER_ADDRESS));
        bean.init();
        bean.getFeedback().setMessage(FEEDBACK_MESSAGE);
        bean.getFeedback().setRecipientAddress(RECIPIENT_ADDRESS);
        bean.setLastName("bla");
        bean.getCaptchaBean().setSecurityAnswer("A");

        bean.submitFeedbackAction(true);
        Mockito.verify(bean.getEmailSender(), Mockito.times(0))
                .postMail(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());

    }

    private static FacesContext mockFacesContext(Map<String, String> requestParameters) {
        FacesContext facesContext = Mockito.mock(FacesContext.class);
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        Mockito.when(externalContext.getRequestParameterMap()).thenReturn(requestParameters);
        return facesContext;
    }

    private static UserBean mockUserBean(String name, String email) {
        UserBean userBean = Mockito.mock(UserBean.class);
        if (StringUtils.isNotBlank(email)) {
            User user = Mockito.mock(User.class);
            Mockito.when(user.getDisplayName()).thenReturn(USER_NAME);
            Mockito.when(user.getEmail()).thenReturn(SENDER_ADDRESS);
            Mockito.when(userBean.getUser()).thenReturn(user);
        }
        return userBean;
    }

}
