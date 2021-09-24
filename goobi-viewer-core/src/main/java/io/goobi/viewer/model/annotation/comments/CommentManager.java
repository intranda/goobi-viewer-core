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
package io.goobi.viewer.model.annotation.comments;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.notification.ChangeNotificator;
import io.goobi.viewer.model.annotation.serialization.AnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.AnnotationLister;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.security.user.User;

/**
 * Class to create comments from a text input for a given PI and page order and to save them using a given {@link AnnotationSaver}
 * 
 * @author florian
 *
 */
public class CommentManager implements AnnotationLister {

    private final static Logger logger = LoggerFactory.getLogger(CommentManager.class);
    
    private final AnnotationSaver saver;
    private final AnnotationDeleter deleter;
    private final List<ChangeNotificator> notificators;
    private final AnnotationConverter converter = new AnnotationConverter();
    private final AnnotationLister lister;

    public CommentManager(AnnotationSaver saver, AnnotationDeleter deleter, AnnotationLister lister, ChangeNotificator... notificators) {
        this.saver = saver;
        this.deleter = deleter;
        this.lister = lister;
        this.notificators = Arrays.asList(notificators);
    }

    public void createComment(String text, User creator, String pi, Integer pageOrder, String license) {
        String textCleaned = checkAndCleanScripts(text, creator, pi, pageOrder);
        PersistentAnnotation comment =
                createAnnotation(createTextualBody(textCleaned), createTarget(pi, pageOrder), Motivation.COMMENTING, createAgent(creator), license);
        try {
            saver.save(comment);
            notificators.forEach(n -> n.notifyCreation(comment, BeanUtils.getLocale()));
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    public void editComment(PersistentAnnotation comment, String text, User editor, String license) {
        String textCleaned = checkAndCleanScripts(text, editor, comment.getTargetPI(), comment.getTargetPageOrder());
        PersistentAnnotation editedComment = new PersistentAnnotation(comment);
        editedComment.setBody(createTextualBody(textCleaned).toString());

        try {
            saver.save(editedComment);
            notificators.forEach(n -> n.notifyEdit(comment, editedComment, BeanUtils.getLocale()));
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    public void deleteComment(PersistentAnnotation comment) {
        try {
            deleter.delete(comment);
            notificators.forEach(n -> n.notifyDeletion(comment, BeanUtils.getLocale()));
        } catch (IOException e) {
            notificators.forEach(n -> n.notifyError(e, BeanUtils.getLocale()));
        }
    }

    /**
     * @param createTextualBody
     * @param createTarget
     * @param commenting
     * @param idAsURI
     * @param license
     * @return
     */
    private PersistentAnnotation createAnnotation(IResource body, IResource target, String motivation, Agent creator, String license) {
        WebAnnotation annotation = new WebAnnotation();
        annotation.setBody(body);
        annotation.setTarget(target);
        annotation.setMotivation(motivation);
        annotation.setCreator(creator);
        annotation.setCreated(LocalDateTime.now());
        annotation.setRights(license);
        return converter.getAsPersistentAnnotation(annotation);
    }

    /**
     * @param creator
     * @return
     */
    private Agent createAgent(User creator) {
        return new Agent(creator.getIdAsURI(), AgentType.PERSON, creator.getDisplayName());
    }

    /**
     * @param pi
     * @param pageOrder
     * @return
     */
    private IResource createTarget(String pi, Integer pageOrder) {
        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> urls.path(ApiUrls.RECORDS_PAGES, ApiUrls.RECORDS_PAGES_CANVAS).params(pi, pageOrder).buildURI())
                .map(uri -> new SimpleResource(uri))
                .orElse(null);

    }

    /**
     * @param text2
     * @return
     */
    private IResource createTextualBody(String text) {
        return new TextualResource(text);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAllAnnotations()
     */
    @Override
    public List<PersistentAnnotation> getAllAnnotations() {
        return lister.getAnnotations(0, Integer.MAX_VALUE, null, Arrays.asList(Motivation.COMMENTING), null, null, null, null, null, false);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getTotalAnnotationCount()
     */
    @Override
    public long getTotalAnnotationCount() {
        return getAllAnnotations().size();
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotations(int, int, java.lang.String, java.util.List, java.util.List, java.util.List, java.lang.String, java.lang.Integer, java.lang.String, boolean)
     */
    @Override
    public List<PersistentAnnotation> getAnnotations(int firstIndex, int items, String textQuery, List<String> motivations, List<Long> generators,
            List<Long> creators, String targetPi, Integer targetPage, String sortField, boolean sortDescending) {
        List<String> allMotivations = new ArrayList<>();
        allMotivations.add(Motivation.COMMENTING);
        if(motivations  != null) {
            allMotivations.addAll(allMotivations);
        }
        return lister.getAnnotations(firstIndex, items, textQuery, allMotivations, generators, creators, targetPi, targetPage, sortField, sortDescending);
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.model.annotation.serialization.AnnotationLister#getAnnotationCount(java.lang.String, java.util.List, java.util.List, java.util.List, java.lang.String, java.lang.Integer)
     */
    @Override
    public long getAnnotationCount(String textQuery, List<String> motivations, List<Long> generators, List<Long> creators, String targetPi,
            Integer targetPage) {
        List<String> allMotivations = new ArrayList<>();
        allMotivations.add(Motivation.COMMENTING);
        if(motivations  != null) {
            allMotivations.addAll(allMotivations);
        }
        return lister.getAnnotationCount(textQuery, allMotivations, generators, creators, targetPi, targetPage);
    }

    private String checkAndCleanScripts(String text, User editor, String pi, Integer page) {
        if (text != null) {
            String cleanText = StringTools.stripJS(text);
            if (cleanText.length() < text.length()) {
                logger.warn("User {} attempted to add a script block into a comment for {}, page {}, which was removed:\n{}", editor.getId(), pi, page, text);
                text = cleanText;
            }
            return cleanText;
        } else {
            return text;
        }
    }
}
