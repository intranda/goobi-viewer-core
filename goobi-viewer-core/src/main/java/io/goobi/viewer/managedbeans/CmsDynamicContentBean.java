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
import java.util.List;
import java.util.Optional;

import javax.faces.component.UIComponent;
import javax.faces.component.html.HtmlPanelGroup;
import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jboss.weld.exceptions.IllegalStateException;

import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CmsDynamicContentBean implements Serializable {

    private static final Logger logger = LogManager.getLogger(CmsDynamicContentBean.class);

    private static final long serialVersionUID = 644204008911471246L;
    private transient HtmlPanelGroup topBarGroup = null;
    private CMSPage cmsPage = null;

    @Deprecated //no longer needed to set
    public void setCmsPage(CMSPage page) {
        this.cmsPage = page;
    }

    public HtmlPanelGroup getTopBarContent() {
        this.cmsPage = Optional.ofNullable(BeanUtils.getCmsBean()).map(CmsBean::getCurrentPage).orElse(null);
        if (topBarGroup == null) {
            try {
                loadTopBarContent();
            } catch (IllegalStateException e) {
                logger.error("Error initializing topbar content: {}", e.getMessage());
            }
        }
        return topBarGroup;
    }

    /**
     * @param topBarGroup the topBarGroup to set
     */
    public void setTopBarContent(HtmlPanelGroup topBarGroup) {
        this.topBarGroup = topBarGroup;
    }

    private void loadTopBarContent() {

        this.topBarGroup = new HtmlPanelGroup();
        if (this.cmsPage == null) {
            throw new IllegalStateException("CMSPage must be set before loading content");
        }
        try {
            List<CMSComponent> components = this.cmsPage.getTopbarComponents();
            for (CMSComponent component : components) {
                UIComponent ui = component.getUiComponent();
                this.topBarGroup.getChildren().add(ui);
            }
        } catch (PresentationException e) {
            logger.error("Error building header components for page {}", cmsPage.getId(), e);

        }
    }

}
