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
package io.goobi.viewer.model.cms.pages.content;

import java.util.Collections;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.context.FacesContext;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.exceptions.IllegalArgumentException;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediaContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSMediumTextContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSShortTextContent;
import io.goobi.viewer.model.jsf.DynamicContentBuilder;
import io.goobi.viewer.model.jsf.JsfComponent;

/**
 * Wraps a {@link CMSContent} within a {@link CMSPage}
 * 
 * @author florian
 *
 */
public class CMSContentItem {

    private static final Logger logger = LogManager.getLogger(CMSContentItem.class);

    /**
     * Local identifier within the component. Used to reference this item within the component xhtml
     */
    private final String itemId;

    /**
     * The actual {@link CMSContent} wrapped in this item
     */
    private final CMSContent content;

    private final String label;

    private final String description;

    private final String htmlGroup;

    private final JsfComponent jsfComponent;

    private final boolean required;

    private final CMSComponent owningComponent;

    private UIComponent uiComponent;

    public CMSContentItem(CMSContentItem orig) {
        this.itemId = orig.itemId;
        this.content = orig.content.copy();
        this.label = orig.label;
        this.description = orig.description;
        this.jsfComponent = orig.jsfComponent;
        this.required = orig.required;
        this.owningComponent = orig.owningComponent;
        this.htmlGroup = orig.htmlGroup;
    }

    /**
     * 
     * @param itemId
     * @param content
     */
    public CMSContentItem(String itemId, CMSContent content, String label, String description, String htmlGroup, JsfComponent jsfComponent,
            CMSComponent owningComponent, boolean required) {
        if (StringUtils.isNotBlank(itemId)) {
            this.itemId = itemId;
        } else {
            throw new IllegalArgumentException("ItemId of CMSContentItem may not be blank");
        }
        if (content != null) {
            this.content = content;
            this.content.setItemId(this.getItemId());
            this.content.setRequired(required);
        } else {
            throw new IllegalArgumentException("CMSContent of COMSContentItem may not be null");
        }
        this.label = label;
        this.description = description;
        this.jsfComponent = jsfComponent;
        this.required = required;
        this.owningComponent = owningComponent;
        this.htmlGroup = htmlGroup;
    }

    public boolean isRequired() {
        return required;
    }

    public String getItemId() {
        return itemId;
    }

    public CMSContent getContent() {
        return content;
    }

    public String getLabel() {
        return label;
    }

    public String getItemLabel() {
        return getLabel();
    }

    public String getDescription() {
        return description;
    }

    public JsfComponent getJsfComponent() {
        return jsfComponent;
    }

    @Override
    public int hashCode() {
        return itemId.hashCode();
    }

    public boolean isMandatory() {
        return this.required;
    }

    /**
     * Two CMSContentItems are equal if their {@link #itemId}s are equal
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            return ((CMSContentItem) obj).itemId.equals(this.itemId);
        }
        return false;
    }

    public UIComponent getUiComponent() throws PresentationException {

        if (this.uiComponent == null) {
            DynamicContentBuilder builder = new DynamicContentBuilder();
            this.uiComponent = FacesContext.getCurrentInstance().getApplication().createComponent(HtmlPanelGroup.COMPONENT_TYPE);
            this.uiComponent.setId(FilenameUtils.getBaseName("cms_" + this.getOwningComponent().getTemplateFilename()) + "_" + this.getOwningComponent().getOrder() + "_" + this.itemId);
            UIComponent wrapper = builder.createTag("div",
                    Collections.singletonMap("class", this.content.isTranslatable() ? "content-item-wrapper -translatable" : "content-item-wrapper"));
            wrapper.setId(this.uiComponent.getId() + "_wrapper");
            this.uiComponent.getChildren().add(wrapper);
            if (StringUtils.isBlank(this.getJsfComponent().getFilename())) {
                logger.warn("No backend component available for contentItem {}", this.getContent().getBackendComponentName());
            } else {
                UIComponent component = builder.build(this.getJsfComponent(), wrapper, Collections.singletonMap("contentItem", this));
                if (component == null) {
                    throw new PresentationException("Error building jsf-component from " + this.getJsfComponent()
                            + ". Please check that the file exists and is a valid jsf component file.");
                }

            }
        }
        return uiComponent;
    }

    public void setUiComponent(UIComponent uiComponent) {
        this.uiComponent = uiComponent;
    }

    /**
     * Check if the {@link #content} attribute exists (i.e. database data is present). If so, and it is textual or media content, also check if text
     * is empty or the media item exists
     * 
     * @return true if no database entry exists or if it doesn't contain data which can be presented
     */
    public boolean isEmpty() {
        return Optional.ofNullable(this.content).map(CMSContent::isEmpty).orElse(true);
    }

    public boolean isShortText() {
        return Optional.ofNullable(this.content).map(CMSShortTextContent.class::isInstance).orElse(false);
    }

    public boolean isHtmlText() {
        return Optional.ofNullable(this.content).map(CMSMediumTextContent.class::isInstance).orElse(false);
    }

    public boolean isMedia() {
        return Optional.ofNullable(this.content).map(CMSMediaContent.class::isInstance).orElse(false);
    }

    public String getHtmlGroup() {
        return Optional.ofNullable(htmlGroup).orElse("");
    }

    public CMSComponent getOwningComponent() {
        return owningComponent;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(itemId);
        if (this.content != null) {
            sb.append(" - ").append(this.content.getClass().getSimpleName());
        }
        if (StringUtils.isNotBlank(this.getHtmlGroup())) {
            sb.append(" (").append(this.getHtmlGroup()).append(")");
        }
        return sb.toString();
    }

}
