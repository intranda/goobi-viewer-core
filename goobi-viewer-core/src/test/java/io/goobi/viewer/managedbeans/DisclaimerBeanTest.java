package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.AbstractTest;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.Disclaimer;
import io.goobi.viewer.solr.SolrSearchIndex;

public class DisclaimerBeanTest extends AbstractTest {

    private IDAO dao;
    private SolrSearchIndex searchIndex;
    private Disclaimer storedDisclaimer;
    private DisclaimerBean bean;
    private NavigationHelper navigationHelper;
    private ActiveDocumentBean activeDocumentBean;
    private UserBean userBean;
    
    @Before
    public void setup() throws DAOException, PresentationException, IndexUnreachableException {
        storedDisclaimer = new Disclaimer();
        storedDisclaimer.getText().setText("Trigger Warnung", Locale.GERMAN);
        storedDisclaimer.getText().setText("Trigger wariung", Locale.ENGLISH);
        storedDisclaimer.setId(1l);
        storedDisclaimer.setSolrQuery("PI:*");
        storedDisclaimer.setActive(true);
        storedDisclaimer.setAcceptanceScope(new ConsentScope("2d"));
        
        dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getDisclaimer()).thenReturn(storedDisclaimer);
        
        searchIndex = Mockito.mock(SolrSearchIndex.class);
        Mockito.when(searchIndex.count(Mockito.anyString())).thenReturn(1l);
        
        navigationHelper = Mockito.mock(NavigationHelper.class);
        Mockito.when(navigationHelper.isDocumentPage()).thenReturn(true);
        Mockito.when(navigationHelper.getLocale()).thenReturn(Locale.GERMAN);

        
        activeDocumentBean = Mockito.mock(ActiveDocumentBean.class);
        Mockito.when(activeDocumentBean.getPersistentIdentifier()).thenReturn("PI1");

        userBean = Mockito.mock(UserBean.class);
        
        
        bean = new DisclaimerBean(dao, searchIndex);
        bean.navigationHelper = navigationHelper;
        bean.activeDocumentBean = activeDocumentBean;
        bean.userBean = userBean;
    }
    
    @Test
    public void testWriteJson() {
        
        String string = bean.getDisclaimerConfig();
        assertTrue(StringUtils.isNotBlank(string));
        JSONObject json  = new JSONObject(string);
        assertEquals(storedDisclaimer.getText().getText(Locale.GERMAN), json.get("disclaimerText"));
        assertTrue(json.getBoolean("active"));
    }
    
    

}
