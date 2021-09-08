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
import java.util.Arrays;
import java.util.List;

import de.intranda.api.annotation.AgentType;
import de.intranda.api.annotation.IResource;
import de.intranda.api.annotation.SimpleResource;
import de.intranda.api.annotation.wa.Agent;
import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.TextualResource;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.v2.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.notification.ChangeNotificator;
import io.goobi.viewer.model.annotation.serialization.AnnotationDeleter;
import io.goobi.viewer.model.annotation.serialization.AnnotationSaver;
import io.goobi.viewer.model.security.user.User;

/**
 * Class to create comments from a text input for a given PI and page order and to save them using a given {@link AnnotationSaver} 
 * 
 * @author florian
 *
 */
public class CommentManager {

    private final AnnotationSaver saver;
    private final AnnotationDeleter deleter;
    private final List<ChangeNotificator> notificators;
    private final AnnotationConverter converter = new AnnotationConverter();
    
    public CommentManager(AnnotationSaver saver, AnnotationDeleter deleter, ChangeNotificator... notificators) {
        this.saver = saver;
        this.deleter = deleter;
        this.notificators = Arrays.asList(notificators);
    }
    
    public void createComment(String text, User creator, String pi, Integer pageOrder, String license) {
        PersistentAnnotation comment = createAnnotation(createTextualBody(text), createTarget(pi, pageOrder), Motivation.COMMENTING, createAgent(creator), license);
        try {            
            saver.save(comment);
            notificators.forEach(n -> n.notifyCreation(comment));
        } catch(IOException e) {
            notificators.forEach(n -> n.notifyError(e));
        }
    }
    
    public void editComment(PersistentAnnotation comment, String text, User editor, String license) {
        PersistentAnnotation editedComment = new PersistentAnnotation(comment);
        editedComment.setBody(createTextualBody(text).toString());
        
        try {            
            saver.save(editedComment);
            notificators.forEach(n -> n.notifyEdit(comment, editedComment));
        } catch(IOException e) {
            notificators.forEach(n -> n.notifyError(e));
        }
    }
    
    public void deleteComment(PersistentAnnotation comment) {
        try {
            deleter.delete(comment);
            notificators.forEach(n -> n.notifyDeletion(comment));
        } catch(IOException e) {
            notificators.forEach(n -> n.notifyError(e));
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
        return DataManager.getInstance().getRestApiManager().getDataApiManager()
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
    
    
}
