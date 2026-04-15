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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSComponentScope;
import io.goobi.viewer.model.security.user.User;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;

class CmsPageEditBeanTest {

    private static final String FILENAME_COMPONENT = "text";
    private static final String DESCRIPTION_COMPONENT = "description_component";
    private static final String NAME_COMPONENT = "name_component";
    private static final Long SELECTED_PAGE_ID = 11l;
    private static final Long PAGE_TEMPLATE_ID = 7l;
    private static final String TEMPLATE_NAME = "Template_sample";
    private static final String PAGE_NAME = "Title of page";
    private static final String RELATED_PI = "AC01";

    CmsPageEditBean bean;

    @BeforeEach
    void setUp() throws DAOException {

        CMSSidebarWidgetsBean widgetsBean = Mockito.mock(CMSSidebarWidgetsBean.class);
        Mockito.when(widgetsBean.getAllWidgets()).thenReturn(Collections.emptyList());

        CollectionViewBean collectionViewBean = Mockito.mock(CollectionViewBean.class);

        CMSPage selectedPage = new CMSPage();
        selectedPage.setId(SELECTED_PAGE_ID);
        CMSPageTemplate selectedTemplate = new CMSPageTemplate();
        selectedTemplate.setId(PAGE_TEMPLATE_ID);
        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getCMSPage(SELECTED_PAGE_ID)).thenReturn(selectedPage);
        Mockito.when(dao.getCMSPageTemplate(PAGE_TEMPLATE_ID)).thenReturn(selectedTemplate);

        bean = new CmsPageEditBean();
        bean.setWidgetsBean(widgetsBean);
        bean.setCollectionViewBean(collectionViewBean);
        bean.setUserBean(mockUserBean(true));
        bean.setDao(dao);
    }

    private static FacesContext mockFacesContext(Map<String, String> requestParameters) {
        FacesContext facesContext = Mockito.mock(FacesContext.class);
        ExternalContext externalContext = Mockito.mock(ExternalContext.class);
        Mockito.when(facesContext.getExternalContext()).thenReturn(externalContext);
        Mockito.when(externalContext.getRequestParameterMap()).thenReturn(requestParameters);
        return facesContext;
    }

    private static UserBean mockUserBean(boolean asCmsAdmin) {
        UserBean userBean = Mockito.mock(UserBean.class);
        User user = Mockito.mock(User.class);
        Mockito.when(user.isCmsAdmin()).thenReturn(asCmsAdmin);
        Mockito.when(userBean.getUser()).thenReturn(user);
        Mockito.when(user.hasPrivilegeForAllSubthemeDiscriminatorValues()).thenReturn(true);
        Mockito.when(user.hasPrivilegeForAllCategories()).thenReturn(true);

        return userBean;
    }

    private static CMSTemplateManager createTemplateManager() {
        CMSTemplateManager templateManager = Mockito.mock(CMSTemplateManager.class);
        CMSComponent component = new CMSComponent(null, NAME_COMPONENT, DESCRIPTION_COMPONENT, null, FILENAME_COMPONENT, CMSComponentScope.PAGEVIEW,
                Collections.emptyMap(), Collections.emptyList(), null);
        Mockito.when(templateManager.getComponent(FILENAME_COMPONENT)).thenReturn(Optional.of(component));
        return templateManager;
    }

    /**
     * When calling bean without context-parameters, create an empty new page
     * 
     * @throws DAOException
     * @verifies new page
     * @see CmsPageEditBean#getSelectedPage
     */
    @Test
    void getSelectedPage_shouldNewPage() {
        FacesContext facesContext = mockFacesContext(Map.of());
        bean.setFacesContext(facesContext);
        bean.setup();
        assertNotNull(bean.getSelectedPage());
        assertNull(bean.getSelectedPage().getId());
        assertNull(bean.getSelectedPage().getTemplateId());
        assertFalse(bean.isEditMode());
    }

    /**
     * When bean is called with context-parameter 'templateId', a new page based on the correspending template should be loaded
     * 
     * @throws DAOException
     * @verifies new page from template
     * @see CmsPageEditBean#getSelectedPage
     */
    @Test
    void getSelectedPage_shouldNewPageFromTemplate() {
        FacesContext facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setup();
        assertNotNull(bean.getSelectedPage());
        assertNull(bean.getSelectedPage().getId());
        assertEquals(PAGE_TEMPLATE_ID, bean.getSelectedPage().getTemplateId());
        assertFalse(bean.isEditMode());
    }

    /**
     * When bean is called with context-parameter 'templateId', a new page based on the correspending template should be loaded
     * 
     * @throws DAOException
     * @verifies new page from template with title and pi
     * @see CmsPageEditBean#getSelectedPage
     */
    @Test
    void getSelectedPage_shouldNewPageFromTemplateWithTitleAndPi() {
        FacesContext facesContext = mockFacesContext(Map.of("templateId", PAGE_TEMPLATE_ID.toString(), "title", PAGE_NAME, "relatedPi", RELATED_PI));
        bean.setFacesContext(facesContext);
        bean.setup();
        assertNotNull(bean.getSelectedPage());
        assertNull(bean.getSelectedPage().getId());
        assertEquals(PAGE_TEMPLATE_ID, bean.getSelectedPage().getTemplateId());
        assertEquals(PAGE_NAME, bean.getSelectedPage().getTitle());
        assertEquals(RELATED_PI, bean.getSelectedPage().getRelatedPI());
        assertFalse(bean.isEditMode());
    }

    /**
     * When Bean is called with context-parameter 'selectedPageId', load the corresponding page
     * 
     * @throws DAOException
     * @verifies edit page
     * @see CmsPageEditBean#getSelectedPage
     */
    @Test
    void getSelectedPage_shouldEditPage() {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setup();
        assertNotNull(bean.getSelectedPage());
        assertEquals(SELECTED_PAGE_ID, bean.getSelectedPage().getId());
        assertTrue(bean.isEditMode());
    }

    /**
     * When calling save as admin, {@link IDAO#updateCMSPage(CMSPage)} should be called. Also
     * {@link CollectionViewBean#removeCollectionsForPage(CMSPage)} should be called to reset collections loaded by this page
     * 
     * @throws DAOException
     * @verifies save page
     * @see CmsPageEditBean#getSelectedPage
     */
    @Test
    void getSelectedPage_shouldSavePage() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setup();
        bean.saveSelectedPage();
        Mockito.verify(bean.getDao(), Mockito.times(1)).updateCMSPage(bean.getSelectedPage());
        Mockito.verify(bean.getCollectionViewBean(), Mockito.times(1)).removeCollectionsForPage(bean.getSelectedPage());
    }

    /**
     * When saving page with setSaveAsTemplate == true, make sure a template with the set name ans lockComponents property is created
     * 
     * @throws DAOException
     * @verifies save as template
     * @see CmsPageEditBean#getDao
     */
    @Test
    void getDao_shouldSaveAsTemplate() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setup();
        bean.setSaveAsTemplate(true);
        bean.setTemplateLockComponents(true);
        bean.setTemplateName(TEMPLATE_NAME);
        bean.saveSelectedPage();
        Mockito.verify(bean.getDao(), Mockito.times(1)).addCMSPageTemplate(Mockito.argThat(template -> template.getName() == TEMPLATE_NAME));
        Mockito.verify(bean.getDao(), Mockito.times(1)).addCMSPageTemplate(Mockito.argThat(CMSPageTemplate::isLockComponents));

    }

    /**
     * If the user is no cmsAdmin, calling save should not update page
     * 
     * @throws DAOException
     * @verifies save page no admin
     * @see CmsPageEditBean#setFacesContext
     */
    @Test
    void setFacesContext_shouldSavePageNoAdmin() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setUserBean(mockUserBean(false));
        bean.setFacesContext(facesContext);
        bean.setup();
        bean.saveSelectedPage();
        Mockito.verify(bean.getDao(), Mockito.times(0)).updateCMSPage(Mockito.any());
    }

    /**
     * When calling delete in bean, call {@link IDAO#deleteCMSPage(CMSPage)}
     * 
     * @throws DAOException
     * @verifies delete page for given input
     * @see CmsPageEditBean#deletePage
     */
    @Test
    void deletePage_shouldDeletePageForGivenInput() throws DAOException {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setup();
        CMSPage page = bean.getSelectedPage();
        bean.deleteSelectedPage();
        Mockito.verify(bean.getDao(), Mockito.times(1)).deleteCMSPage(page);
    }

    /**
     * @verifies return true for given input
     * @see CmsPageEditBean#addComponent()
     */
    @Test
    void addComponent_shouldReturnTrueForGivenInput() {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setTemplateManager(createTemplateManager());
        bean.setup();
        bean.setSelectedComponent(FILENAME_COMPONENT);
        assertTrue(bean.getSelectedPage().getComponents().isEmpty());
        bean.addComponent();
        assertFalse(bean.getSelectedPage().getComponents().isEmpty());
        assertEquals(NAME_COMPONENT, bean.getSelectedPage().getComponents().get(0).getLabel());
    }

    /**
     * @verifies return true for given input
     * @see CmsPageEditBean#deleteComponent(CMSComponent)
     */
    @Test
    void deleteComponent_shouldReturnTrueForGivenInput() {
        FacesContext facesContext = mockFacesContext(Map.of("selectedPageId", SELECTED_PAGE_ID.toString()));
        bean.setFacesContext(facesContext);
        bean.setTemplateManager(createTemplateManager());
        bean.setup();
        bean.setSelectedComponent(FILENAME_COMPONENT);
        assertTrue(bean.getSelectedPage().getComponents().isEmpty());
        bean.addComponent();
        assertFalse(bean.getSelectedPage().getComponents().isEmpty());
        bean.deleteComponent(bean.getSelectedPage().getComponents().get(0));
        assertTrue(bean.getSelectedPage().getComponents().isEmpty());
    }

    /**
     * Verifies that setUserRestrictedValues() stores an independent copy of the allowed categories
     * rather than a raw subList view. If a raw subList were stored, modifying the backing list
     * afterwards would cause ConcurrentModificationException in JSF's ListDataModel during rendering.
     * @verifies categories is defensive copy
     */
    @Test
    void setUserRestrictedValues_shouldCategoriesIsDefensiveCopy() throws DAOException {
        List<CMSCategory> backingCategories = new ArrayList<>(List.of(new CMSCategory("cat1"), new CMSCategory("cat2")));

        User restrictedUser = Mockito.mock(User.class);
        Mockito.when(restrictedUser.isCmsAdmin()).thenReturn(true);
        Mockito.when(restrictedUser.hasPrivilegeForAllSubthemeDiscriminatorValues()).thenReturn(true);
        Mockito.when(restrictedUser.hasPrivilegeForAllCategories()).thenReturn(false);
        // Simulate User.getAllowedCategories() returning the original list directly (as it does for superusers)
        Mockito.when(restrictedUser.getAllowedCategories(ArgumentMatchers.anyList())).thenReturn(backingCategories);

        UserBean restrictedUserBean = Mockito.mock(UserBean.class);
        Mockito.when(restrictedUserBean.getUser()).thenReturn(restrictedUser);

        CmsBean mockCmsBean = Mockito.mock(CmsBean.class);
        Mockito.when(mockCmsBean.getAllCategories()).thenReturn(backingCategories);

        bean.setUserBean(restrictedUserBean);
        bean.setCmsBean(mockCmsBean);
        bean.setFacesContext(mockFacesContext(Map.of()));
        bean.setup();

        // Simulate concurrent modification: another request modifies the backing list
        backingCategories.add(new CMSCategory("cat3"));

        // If categories were stored as a raw subList view, this would throw ConcurrentModificationException
        assertDoesNotThrow(() -> bean.getSelectedPage().getCategories().size());
        assertEquals(1, bean.getSelectedPage().getCategories().size());
    }
}
