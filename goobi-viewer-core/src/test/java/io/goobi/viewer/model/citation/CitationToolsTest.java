package io.goobi.viewer.model.citation;


import org.junit.Assert;
import org.junit.Test;

import de.undercouch.citeproc.csl.CSLType;

public class CitationToolsTest {
    /**
    * @see CitationTools#getCSLTypeForDocstrct(String)
    * @verifies return correct type
    */
    @Test
    public void getCSLTypeForDocstrct_shouldReturnCorrectType() throws Exception {
       Assert.assertEquals(CSLType.BOOK, CitationTools.getCSLTypeForDocstrct("monograph"));
       Assert.assertEquals(CSLType.MANUSCRIPT, CitationTools.getCSLTypeForDocstrct("manuscript"));
       Assert.assertEquals(CSLType.WEBPAGE, CitationTools.getCSLTypeForDocstrct("object"));
    }
}