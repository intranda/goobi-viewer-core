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
package io.goobi.viewer;

import java.io.FileInputStream;

import org.dbunit.IDatabaseTester;
import org.dbunit.dataset.xml.FlatXmlDataSetBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.impl.H2JdbcDatabaseTester;
import io.goobi.viewer.dao.impl.JPADAO;

/**
 * JUnit test classes that extend this class can use the embedded H2 DBMS server setup with a fixed database and the embedded Solr server setup with a
 * fixed viewer index. This class is necessary because Java unfortunately doesn't support multi-inheritance.
 */
public abstract class AbstractDatabaseAndSolrEnabledTest extends AbstractSolrEnabledTest {

    private static IDatabaseTester databaseTester;

    //    protected static IDataSet getDataSet() throws Exception {
    //        return new XmlDataSet(new FileInputStream("resources/test_db_dataset.xml"));
    //    }

    @BeforeAll
    public static void setUpClass() throws Exception {
        AbstractSolrEnabledTest.setUpClass();
        DataManager.getInstance().injectDao(new JPADAO("intranda_viewer_test"));
        databaseTester = new H2JdbcDatabaseTester();
        try (FileInputStream fis = new FileInputStream("src/test/resources/test_db_dataset.xml")) {
            databaseTester.setDataSet(new FlatXmlDataSetBuilder().setColumnSensing(true).build(fis));
        }
        // databaseTester.setDataSet(getDataSet());
    }

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        // databaseTester.setDataSet(new FlatXmlDataSetBuilder().setColumnSensing(true).build(new FileInputStream("resources/test_db_dataset.xml")));
        databaseTester.onSetup();
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        super.tearDown();
        databaseTester.onTearDown();
        ((JPADAO) DataManager.getInstance().getDao()).clear();

        // FlatXmlDataSet
        // .write(databaseTester.getConnection().createDataSet(), new FileOutputStream("resources/" + System.currentTimeMillis() + ".xml"));
    }

    @AfterAll
    public static void tearDownClass() throws Exception {
        AbstractSolrEnabledTest.tearDownClass();
        if (DataManager.getInstance().getDao() != null) {
            DataManager.getInstance().getDao().shutdown();
        }
    }
}
