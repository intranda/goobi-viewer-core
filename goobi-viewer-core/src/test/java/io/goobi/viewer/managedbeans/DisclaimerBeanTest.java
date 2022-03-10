package io.goobi.viewer.managedbeans;

import static org.junit.Assert.assertTrue;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.administration.legal.ConsentScope;
import io.goobi.viewer.model.administration.legal.Disclaimer;

public class DisclaimerBeanTest {

    private IDAO dao;
    private Disclaimer storedDisclaimer;
    private DisclaimerBean bean;
    
    @Before
    public void setup() throws DAOException {
        storedDisclaimer = new Disclaimer();
        storedDisclaimer.getText().setText("Trigger Warnung", Locale.GERMAN);
        storedDisclaimer.getText().setText("Trigger wariung", Locale.ENGLISH);
        storedDisclaimer.setId(1l);
        storedDisclaimer.setSolrQuery("PI:*");
        storedDisclaimer.setActive(true);
        storedDisclaimer.setAcceptanceScope(new ConsentScope("2d"));
        dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getDisclaimer()).thenReturn(storedDisclaimer);
        
        bean = new DisclaimerBean(dao);
    }
    
    @Test
    public void testWriteJson() {
        
        String string = bean.getDisclaimerConfig();
        assertTrue(StringUtils.isNotBlank(string));
        JSONObject json  = new JSONObject(string);
        
        
    }

}
