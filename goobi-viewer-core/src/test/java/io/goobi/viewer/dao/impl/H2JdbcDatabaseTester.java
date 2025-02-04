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
package io.goobi.viewer.dao.impl;

import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.ext.h2.H2DataTypeFactory;

/**
 * Custom JdbcDatabaseTester class that uses the H2DataTypeFactory so that no warning is displayed on every query.
 */
public class H2JdbcDatabaseTester extends JdbcDatabaseTester {

    public H2JdbcDatabaseTester() throws ClassNotFoundException {
        super("org.h2.Driver", "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "user", "password");
    }

    @Override
    public IDatabaseConnection getConnection() throws Exception {
        IDatabaseConnection databaseConnection = super.getConnection();
        DatabaseConfig databaseConfig = databaseConnection.getConfig();
        databaseConfig.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new H2DataTypeFactory());
        databaseConfig.setProperty(DatabaseConfig.FEATURE_QUALIFIED_TABLE_NAMES, Boolean.TRUE);
        databaseConfig.setProperty(DatabaseConfig.FEATURE_CASE_SENSITIVE_TABLE_NAMES, Boolean.TRUE);
        return databaseConnection;
    }

}
