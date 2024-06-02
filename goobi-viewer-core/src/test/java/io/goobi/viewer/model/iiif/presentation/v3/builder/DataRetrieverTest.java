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
package io.goobi.viewer.model.iiif.presentation.v3.builder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import de.intranda.monitoring.timer.Timer;
import de.intranda.monitoring.timer.TimerOutput;
import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.Configuration;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.search.CollectionResult;
import io.goobi.viewer.model.viewer.StructElement;
import io.goobi.viewer.solr.SolrSearchIndex;

/**
 * @author florian
 *
 */
class DataRetrieverTest extends AbstractSolrEnabledTest{

    private DataRetriever dataRetriever = new DataRetriever();

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
    }

    @AfterEach
    public void tearDown() throws Exception {
       super.tearDown();
    }

    @Test
    void testGetTopCollections() throws IndexUnreachableException {
        try (Timer timer = new Timer(TimerOutput.SIMPLE, (time -> {}))){
            List<CollectionResult> results = dataRetriever.getTopLevelCollections("DC");
            assertFalse(results.isEmpty());
            results.forEach(c -> {
                assertFalse(c.getName().contains("."));
                assertTrue(c.getCount() > 0);
//                System.out.println(c.getName() + " / " + c.getChildCount() + " / " + c.getCount());
            });
        }
    }

    @Test
    void testGetChildCollections() throws IndexUnreachableException {
        try (Timer timer = new Timer(TimerOutput.SIMPLE, (time -> {}))){
            List<CollectionResult> results = dataRetriever.getChildCollections("DC", "dctext");
            assertFalse(results.isEmpty());
            results.forEach(c -> {
//                System.out.println(c.getName() + " / " + c.getChildCount() + " / " + c.getCount());
                assertTrue(c.getCount() > 0);
            });
        }
    }

    @Test
    void testGetContainedRecords() throws IndexUnreachableException, PresentationException {
        try (Timer timer = new Timer(TimerOutput.SIMPLE, (time -> {}))){
            List<StructElement> results = dataRetriever.getContainedRecords("DC", "dctext.ocr");
            assertFalse(results.isEmpty());
            results.forEach(ele -> {
//                System.out.println(ele.getLabel() + " (" + (ele.isAnchor() ? "anchor" : "record") + ")");
                assertTrue(StringUtils.isNotBlank(ele.getLabel()));
            });
        }
    }


}
