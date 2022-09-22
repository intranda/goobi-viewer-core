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
package io.goobi.viewer.model.cms.content;

import java.io.File;
import java.io.IOException;
import java.util.List;

import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSPage;
import io.goobi.viewer.model.translations.IPolyglott;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/**
 * Interface for all classes containing a specific kind of content for a {@link CMSPage}. 
 * Each CMSContent on a CMSPage is wrapped in a {@link CMSContentItem} which itself is contained in
 * a {@link CMSComponent}.
 * @author florian
 *
 */
@Entity
@Inheritance(strategy=InheritanceType.TABLE_PER_CLASS)
public abstract class CMSContent implements IPolyglott {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_content_id")
    private Long id;
    
    /**
     * Mirrors the {@link CMSContentItem#getComponentId()} of the enclosing {@link CMSContentItem}
     * Used to identify the persistent content with the configuration from the xml component file
     */
    @Column(name = "component_id")
    private String componentId;

    /** Reference to the owning <code>PersistentCMSComponent</code>. */
    @ManyToOne
    @JoinColumn(name = "owning_component")
    private PersistentCMSComponent owningComponent;
    
    public abstract String getBackendComponentName();
    
    public CMSContent() {
        //empty
    }
    
    protected CMSContent(CMSContent orig) {
        this.id = orig.id;
        this.componentId = orig.componentId;
        this.owningComponent = orig.owningComponent;
    }
    
    public String getBackendComponentLibrary() {
        return "cms/backend/components/content";
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getComponentId() {
        return componentId;
    }
    
    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }
    
    public PersistentCMSComponent getOwningComponent() {
        return owningComponent;
    }
    
    public void setOwningComponent(PersistentCMSComponent owningComponent) {
        this.owningComponent = owningComponent;
    }

    public abstract CMSContent copy();

    /**
     * Writes HTML fragment value as file for re-indexing. HTML/text fragments are exported directly. Attached media items are exported as long as
     * their content type is one of the supported text document formats.
     *
     * @param pageId ID of the owning CMS page
     * @param outputFolderPath a {@link java.lang.String} object.
     * @param namingScheme a {@link java.lang.String} object.
     * @return Exported Files
     * @should write files correctly
     * @throws java.io.IOException if any.
     * @throws ViewerConfigurationException 
     */
    public abstract List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException;

}
