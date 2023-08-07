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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSComponentScope;
import io.goobi.viewer.model.security.user.User;

public class CMSPageTemplateEditBeanTest {

    private static final String FILENAME_COMPONENT = "text";
    private static final String DESCRIPTION_COMPONENT = "description_component";
    private static final String NAME_COMPONENT = "name_component";
    private static final Long PAGE_TEMPLATE_ID = 7l;
    
    CMSPageTemplateEditBean bean;
    
    @Before
    public void setup() throws DAOException {
        
        CMSSidebarWidgetsBean widgetsBean = Mockito.mock(CMSSidebarWidgetsBean.class);
        Mockito.when(widgetsBean.getAllWidgets()).thenReturn(Collections.emptyList());
                
        CMSPageTemplate selectedTemplate = new CMSPageTemplate();
        selectedTemplate.setId(PAGE_TEMPLATE_ID);
        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getCMSPageTemplate(PAGE_TEMPLATE_ID)).thenReturn(selectedTemplate);
        Mockito.when(dao.removeCMSPageTemplate(Mockito.any())).thenReturn(true);
        
        bean = new CMSPageTemplateEditBean();
        bean.widgetsBean = widgetsBean;
        bean.userBean = mockUserBean(true);
        bean.dao = dao;
    }

    private FacesContext mockFacesContext(Map<String, String> requestParameters) {
        FacesContext facesContext = Mockito.mock(FacesContext.class);
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        Mockito.when(externalContext.getRequestParameterMap()).thenReturn(requestParameters);
        return facesContext;
    }
    
    private UserBean mockUserBean(boolean asCmsAdmin) {
        UserBean userBean = Mockito.mock(UserBean.class);
        User user = Mockito.mock(User.class);
        Mockito.when(user.isCmsAdmin()).thenReturn(asCmsAdmin);
        Mockito.when(userBean.getUser()).thenReturn(user);
        Mockito.when(user.hasPrivilegeForAllSubthemeDiscriminatorValues()).thenReturn(true);
        Mockito.when(user.hasPrivilegeForAllCategories()).thenReturn(true);

        return userBean;
    }
    

    private CMSTemplateManager createTemplateManager() {
        CMSTemplateManager templateManager = Mockito.mock(CMSTemplateManager.class);
        CMSComponent component = new CMSComponent(null, NAME_COMPONENT, DESCRIPTION_COMPONENT, null, FILENAME_COMPONENT, CMSComponentScope.PAGEVIEW, Collections.emptyMap(), null);
        Mockito.when(templateManager.getComponent(FILENAME_COMPONENT)).thenReturn(Optional.of(component));
        return templateManager;
    }
    
    @Test
    public void testEditTemplate() {
        bean.facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString()));
        bean.templateManager = createTemplateManager();
        
        bean.setup();
        assertEquals(PAGE_TEMPLATE_ID, bean.getSelectedTemplate().getId());
        assertTrue(bean.isEditMode());
    }

    @Test
    public void testCreateTemplate() {
        bean.facesContext = mockFacesContext(Map.of());
        bean.templateManager = createTemplateManager();
        
        bean.setup();
        assertNull(bean.getSelectedTemplate().getId());
        assertFalse(bean.isEditMode());
    }
    
    @Test
    public void testSaveTemplate() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString()));
        bean.facesContext = facesContext;
        bean.setup();
        bean.saveSelectedTemplate();
        Mockito.verify(bean.dao, Mockito.times(1)).updateCMSPageTemplate(bean.getSelectedTemplate());
    }
    
    @Test
    public void testDeleteTemplate() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString()));
        bean.facesContext = facesContext;
        bean.setup();
        CMSPageTemplate template = bean.getSelectedTemplate();
        bean.deleteSelectedTemplate();
        Mockito.verify(bean.dao, Mockito.times(1)).removeCMSPageTemplate(template);
        assertNull(bean.getSelectedTemplate());
    }
    
    @Test
    public void testAddComponent() {
        FacesContext facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString()));
        bean.facesContext = facesContext;
        bean.templateManager = createTemplateManager();
        bean.setup();
        bean.setSelectedComponent(FILENAME_COMPONENT);
        assertTrue(bean.getSelectedTemplate().getComponents().isEmpty());
        bean.addComponent();
        assertFalse(bean.getSelectedTemplate().getComponents().isEmpty());
        assertEquals(NAME_COMPONENT, bean.getSelectedTemplate().getComponents().get(0).getLabel());
    }

}
