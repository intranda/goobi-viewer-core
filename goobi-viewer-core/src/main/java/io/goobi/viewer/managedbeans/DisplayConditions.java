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
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.omnifaces.cdi.Eager;

import io.goobi.viewer.controller.json.JsonStringConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.RecordNotFoundException;
import io.goobi.viewer.model.security.AccessConditionUtils;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.viewer.PageType;
import io.goobi.viewer.model.viewer.record.views.RecordPropertyCache;
import io.goobi.viewer.model.viewer.record.views.VisibilityCondition;
import io.goobi.viewer.model.viewer.record.views.VisibilityConditionInfo;
import jakarta.faces.component.UIComponent;
import jakarta.faces.component.UINamingContainer;
import jakarta.faces.component.html.HtmlPanelGrid;
import jakarta.faces.component.html.HtmlPanelGroup;
import jakarta.faces.context.FacesContext;

/**
 * DisplayConditions tests whether GUI elements in record views should be visible by a number of factors. These factors encompass the the current
 * view, access conditions and properties of the current record and page. There are two key methods to test display conditions:
 * <ul>
 * <li>{@link #matchRecord(String)}</li>
 * <li>{@link #matchPage(String)}</li>
 * </ul>
 * The methods check against properties of the record and of the current page respectively. Both that a pseudo-json string as an agument that is
 * explained in more detail in the documentation of both methods.
 * 
 */
@Named
@SessionScoped
@Eager
public class DisplayConditions implements Serializable {

    private static final long serialVersionUID = 6193053985791285569L;
    private static final Logger logger = LogManager.getLogger(DisplayConditions.class);

    @Inject
    protected ActiveDocumentBean activeDocumentBean;
    @Inject
    protected NavigationHelper navigationHelper;
    @Inject
    private HttpServletRequest httpRequest;

    private RecordPropertyCache propertyCache = new RecordPropertyCache();

    /**
     * Called with a string in form of a modified json object. The object may not contain any quotation marks and values may be preceded by a '!'
     * indicating a negation of the check for this value. A typical form is
     * 
     * <pre>{@code
     * { 
     *     contentType:[IMAGE, AUDIO, VIDEO, MODEL], 
     *     accessCondition: VIEW_IMAGES, 
     *     pageType: ![viewMetadata], 
     *     numPages:2 
     * }
     * }</pre>
     * 
     * The object may contain the following properties. Each given property is checked against the current view, request and record:
     * <ul>
     * 
     * <li>
     * <dl>
     * <dt>contentType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: IMAGE, AUDIO, VIDEO, MODEL, ALTO, TEI, PDF, EPUB</dd>
     * <dd><i>Description:</i> Content file types available for the record in the file system. If the record contains at least one of the listed file
     * types, the check returns true</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>mimeType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: image, video, audio, application, text, model (3D-Objekt), other</dd>
     * <dd><i>Description:</i> Base part of the mime type set for the record in the SOLR database. Generally the mime type of the files within the
     * record's media folder. This check return true if the mime type is any of the values within the list</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>accessCondition</dt>
     * <dd><i>Possible values:</i> An array of any of the string values of the 'PRIV_' constants defined in {@link IPrivilegeHolder}, which is the
     * name of the constant without the 'PRIV_' prefix</dd>
     * <dd><i>Description:</i> The access privilege which must be satisfied by the current request for the current record. Calls the method
     * {@link AccessConditionUtils#checkAccessPermissionByIdentifierAndLogId(String, String, String, HttpServletRequest)} to check access</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>pageType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: viewToc, viewThumbs, viewMetadata, viewFulltext, viewFullscreen,
     * viewObject, viewCalendar, cmsPage</dd>
     * <dd><i>Descrption:</i> Name of the current record page or 'view'. The condition returns true if the current view is one of the values of the
     * given array</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>sourceFormat</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: METS, LIDO, DUBLINCORE, METS_MARC, DENKXWEB</dd>
     * <dd><i>Description:</i> The source metadata format of the current record. This condition returns true if the format is within the given
     * list.</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>docType</dt>
     * <dd><i>Possible values:</i> An array of one or more doc struct types from the SOLR field 'DOCSTRCT' and some additional values document
     * structure: 'group', 'groupMember', 'anchor', 'volume', 'record' and 'subStruct' (structure element within a record)</dd>
     * <dd><i>Description:</i> The structure type of the current record as well as some of the additional values detailing structure hierarchy. The
     * condition returns true if any of the listed values matches one or more structure properties of the current record. 'subStruct' returns true if
     * a structure element within the record is selected</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>numPages</dt>
     * <dd><i>Possible values:</i> The number of pages within the current record. An integer equals '0' or larger</dd>
     * <dd><i>Descrption:</i>This condition returns true if the number of pages of the current record equals at least the given number.</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>tocSize</dt>
     * <dd><i>Possible values:</i> The number of elements within the record's table of content. An integer equals '1' or larger</dd>
     * <dd><i>Descrption:</i> This condition returns true if the table of content of the current record equals at least the given number. The record
     * document itself counts towards this number so the lowest possible value is 1.</dd>
     * </dl>
     * </li>
     * </ul>
     * 
     * A '!' character preceding a string value means that the condition should not match the value for the check to return true; preceding an array,
     * it means that none of the values within the list should match the record. Preceding a number, the '!' means that the actual value must be less
     * than the given number for the condition to return true. Values for contentType and accessCondition are cached per http session and record
     * 
     * @param json
     * @return true if the given conditions are met by the current record, false otherwise
     * @throws IOException An exception occured file system resources
     * @throws IndexUnreachableException An exception occured communicating the the viewer data index (SOLR)
     * @throws DAOException An exception occured communicating with the viewer sql database
     * @throws RecordNotFoundException The current record could not be found in the viewer data index when checking access conditions
     * @throws PresentationException Any other exception encountered while checking file system resources or the SOLR database
     */
    public boolean matchRecord(String json)
            throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {
        String cleanedJson = json.replaceAll(":\\s*!\\[", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(cleanedJson);
        VisibilityCondition condition = new VisibilityCondition(info);
        return condition.matchesRecord(getPageType(), activeDocumentBean.getViewManager(), httpRequest, propertyCache);
    }

    /**
     * Called with a string in form of a modified json object. The object may not contain any quotation marks and values may be preceded by a '!'
     * indicating a negation of the check for this value. A typical form is
     * 
     * <pre>{@code
     * { 
     *     contentType:[IMAGE, AUDIO, VIDEO, MODEL], 
     *     accessCondition: VIEW_IMAGES, 
     *     pageType: ![viewMetadata], 
     * }
     * }</pre>
     * 
     * The object may contain the following properties. Each given property is checked against the current view, request and record:
     * <ul>
     * 
     * <li>
     * <dl>
     * <dt>contentType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: IMAGE, AUDIO, VIDEO, MODEL, ALTO, TEI, PDF, EPUB</dd>
     * <dd><i>Description:</i> Content file types available for the current page in the file system.</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>mimeType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: image, video, audio, application, text, model (3D-Objekt), other</dd>
     * <dd><i>Description:</i> Base part of the mime type set for the page document in the SOLR database.</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>accessCondition</dt>
     * <dd><i>Possible values:</i> An array of any of the string values of the 'PRIV_' constants defined in {@link IPrivilegeHolder}, which is the
     * name of the constant without the 'PRIV_' prefix</dd>
     * <dd><i>Description:</i> The access privilege which must be satisfied by the current request for the current record and page. Calls the method
     * {@link AccessConditionUtils#checkAccessPermissionByIdentifierAndFileNameWithSessionMap(HttpServletRequest, String, String, String)} to check
     * access</dd>
     * </dl>
     * </li>
     * 
     * <li>
     * <dl>
     * <dt>pageType</dt>
     * <dd><i>Possible values:</i> An array of one or more of the following: viewToc, viewThumbs, viewMetadata, viewFulltext, viewFullscreen,
     * viewObject, viewCalendar, cmsPage</dd>
     * <dd><i>Descrption:</i> Name of the current record page or 'view'. The condition returns true if the current view is one of the values of the
     * given array</dd>
     * </dl>
     * </li>
     * 
     * A '!' character preceding a string value means that the condition should not match the value for the check to return true; preceding an array,
     * it means that none of the values within the list should match the page. Values for contentType and accessCondition are cached per http session
     * and page
     * 
     * @param json
     * @return true if the given conditions are met by the current page, false otherwise
     * @throws IOException An exception occured file system resources
     * @throws IndexUnreachableException An exception occured communicating the the viewer data index (SOLR)
     * @throws DAOException An exception occured communicating with the viewer sql database
     * @throws RecordNotFoundException The current record could not be found in the viewer data index when checking access conditions
     * @throws PresentationException Any other exception encountered while checking file system resources or the SOLR database
     */
    public boolean matchPage(String json)
            throws IOException, IndexUnreachableException, DAOException, RecordNotFoundException, PresentationException {
        String cleanedJson = json.replaceAll(":\\s*!\\[", ":[!,");
        VisibilityConditionInfo info = JsonStringConverter.of(VisibilityConditionInfo.class).convert(cleanedJson);
        VisibilityCondition condition = new VisibilityCondition(info);
        if (activeDocumentBean == null || !activeDocumentBean.isRecordLoaded()) {
            return false;
        }
        return condition.matchesPage(getPageType(), activeDocumentBean.getViewManager().getCurrentPage(), httpRequest,
                propertyCache);
    }

    /**
     * Get the {@link PageType} of the current page
     * 
     * @return A {@link PageType}
     */
    public PageType getPageType() {
        return navigationHelper.isCmsPage() ? PageType.cmsPage : navigationHelper.getCurrentPageType();
    }

    /**
     * Get {@link UIComponentHelper} for the {@link UIComponent} with the given identifier within the composite component from which the method is
     * called. This is used to count the number of rendered jsf-components with a specific attribute within said UIComponent.
     *
     * @param id
     * @return a {@link UIComponentHelper} for the {@link UIComponent} with the given id. If no such component exists, the current component
     */
    public UIComponentHelper getTag(String id) {
        UIComponentHelper tag = UIComponentHelper.getCurrentComponent().getChild(id);
        if (tag == null) {
            return UIComponentHelper.getCurrentComponent();
        }
        return tag;
    }

    /**
     * Wrapper for a {@link UIComponent} with convenience methods to count contained jsf components with certain attributes.
     */
    public static class UIComponentHelper {

        private final UIComponent component;

        static UIComponentHelper getCurrentComponent() {
            return new UIComponentHelper(UIComponent.getCurrentComponent(FacesContext.getCurrentInstance()));
        }

        UIComponentHelper(UIComponent component) {
            this.component = component;
        }

        UIComponent getComponent() {
            return component;
        }

        /**
         * Count all rendered child components of the {@link UIComponent} wrapped by this instance which have the attribute 'visibilty-class' with the
         * value given by the passed parameter.
         * 
         * @param visibilityClass Value of the 'visibility-class' attribute of all child components which should be counted
         * @return a number >= 0
         */
        public Long getChildCount(String visibilityClass) {
            return getDescendants(this.component)
                    .stream()
                    .filter(child -> StringUtils.isNotBlank(visibilityClass) ? hasVisibilityTag(child, visibilityClass) : true)
                    .filter(child -> isRendered(child))
                    .filter(child -> !hasNotRenderedParent(child))
                    .count();
        }

        private static boolean isRendered(UIComponent child) {
            try {
                return child.isRendered() && isHasValuesIfRepeat(child);
            } catch (ConcurrentModificationException e) {
                //possibly happens when rendered conditions are tested on child with 'displayConditions.matchPage/matchRecord', according to log entry
                logger.warn("Cannot detect rendered state of child compnent {} because of {}", child.getClientId(), e.toString());
                return true;
            }
        }

        private boolean hasNotRenderedParent(UIComponent child) {
            UIComponent current = child.getParent();
            while (current != null && current != this.component) {
                if (!isRendered(current)) {
                    return true;
                }
                current = current.getParent();
            }

            return false;
        }

        @Deprecated(since = "24.10")
        public boolean isHasChildrenIfComposite(UIComponent child) {
            if (child instanceof UINamingContainer || child instanceof HtmlPanelGroup || child instanceof HtmlPanelGrid) {
                return child.getChildCount() > 0;
            }

            return true;
        }

        UIComponentHelper getChild(String id) {
            if (StringUtils.isNotBlank(id)) {
                return this.getDescendants(this.component)
                        .stream()
                        .filter(child -> id.equals(child.getId()))
                        .findAny()
                        .map(UIComponentHelper::new)
                        .orElse(null);
            }

            throw new IllegalArgumentException("Must pass a non-null value for id of descendant you want to find");
        }

        private List<UIComponent> getDescendants(UIComponent container) {
            List<UIComponent> descs = new ArrayList<>();
            for (UIComponent child : container.getChildren()) {
                descs.add(child);
                descs.addAll(getDescendants(child));
            }
            return descs;
        }

        private static boolean hasVisibilityTag(UIComponent c, String visibilityClass) {
            Object styles = c.getAttributes().get("visibility-class");
            if (styles instanceof Collection) {
                return ((Collection<?>) styles).contains(visibilityClass);
            } else if (styles != null) {
                return styles.toString().equals(visibilityClass);
            } else {
                return false;
            }
        }

        /**
         * Check if the given element is a ui:repeat. If so and if its value has no elements, return false. Otherwise true
         * 
         * @param c
         * @return
         */
        private static boolean isHasValuesIfRepeat(UIComponent c) {
            // TODO Find replacement for com.sun.faces.*
            //            if (c instanceof com.sun.faces.facelets.component.UIRepeat repeat) {
            //                Object value = repeat.getValue();
            //                if (value instanceof Collection) {
            //                    return !((Collection<?>) value).isEmpty();
            //                }
            //                return false;
            //            }

            return true;
        }
    }

    public void clearCache() {
        propertyCache = new RecordPropertyCache();
    }

}
