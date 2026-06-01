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

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Holds a single EntityManagerFactory for the entire test JVM lifetime.
 * Creating an EntityManagerFactory is expensive (EclipseLink scans all entity classes and builds
 * its metamodel), so reusing one across all test classes saves significant time.
 * Safe because: L2 shared cache is disabled in the test persistence unit, every JPADAO operation
 * creates and closes its own EntityManager, and DBUnit resets data via JDBC before each test method.
 */
public final class TestEntityManagerFactoryHolder {

    private static final String PERSISTENCE_UNIT = "intranda_viewer_test";
    private static volatile EntityManagerFactory factory;

    private TestEntityManagerFactoryHolder() {
    }

    public static synchronized EntityManagerFactory get() {
        if (factory == null || !factory.isOpen()) {
            final Thread currentThread = Thread.currentThread();
            final ClassLoader saveClassLoader = currentThread.getContextClassLoader();
            currentThread.setContextClassLoader(new JPAClassLoader(saveClassLoader));
            factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT);
            currentThread.setContextClassLoader(saveClassLoader);
        }
        return factory;
    }
}
