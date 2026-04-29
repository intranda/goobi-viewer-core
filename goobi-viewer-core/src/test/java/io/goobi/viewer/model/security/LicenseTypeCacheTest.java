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
package io.goobi.viewer.model.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;

/**
 * Integration tests for {@link LicenseTypeCache} against the H2 test fixture.
 */
class LicenseTypeCacheTest extends AbstractDatabaseEnabledTest {

    private LicenseTypeCache cache;

    @BeforeEach
    void initCache() {
        cache = new LicenseTypeCache();
    }

    /**
     * @see LicenseTypeCache#getAllLicenseTypes()
     * @verifies return all license types from dao
     */
    @Test
    void getAllLicenseTypes_shouldReturnAllLicenseTypesFromDao() throws Exception {
        List<LicenseType> result = cache.getAllLicenseTypes();
        assertEquals(NUM_LICENSE_TYPES, result.size());
    }

    /**
     * @see LicenseTypeCache#getAllLicenseTypes()
     * @verifies return same instance on second call
     */
    @Test
    void getAllLicenseTypes_shouldReturnSameInstanceOnSecondCall() throws Exception {
        List<LicenseType> first = cache.getAllLicenseTypes();
        List<LicenseType> second = cache.getAllLicenseTypes();
        assertSame(first, second);
    }

    /**
     * @see LicenseTypeCache#getAllLicenseTypes()
     * @verifies return immutable list
     */
    @Test
    void getAllLicenseTypes_shouldReturnImmutableList() throws Exception {
        List<LicenseType> result = cache.getAllLicenseTypes();
        assertThrows(UnsupportedOperationException.class, () -> result.add(new LicenseType()));
    }

    /**
     * @see LicenseTypeCache#getAllLicenseTypes()
     * @verifies initialise lazy collections
     */
    @Test
    void getAllLicenseTypes_shouldInitialiseLazyCollections() throws Exception {
        List<LicenseType> result = cache.getAllLicenseTypes();
        // Must not throw LazyInitializationException after the DAO closed its EntityManager.
        for (LicenseType lt : result) {
            lt.getOverriddenLicenseTypes().size();
            lt.getImagePlaceholders().size();
        }
    }

    /**
     * @see LicenseTypeCache#getRecordLicenseTypes()
     * @verifies filter out core license types
     */
    @Test
    void getRecordLicenseTypes_shouldFilterOutCoreLicenseTypes() throws Exception {
        List<LicenseType> result = cache.getRecordLicenseTypes();
        assertTrue(result.stream().noneMatch(LicenseType::isCore));
        // Baseline from JPADAOTest#getRecordLicenseTypes_shouldOnlyReturnNonOpenAccessLicenseTypes.
        assertEquals(5, result.size());
    }

    /**
     * @see LicenseTypeCache#getLicenseType(String)
     * @verifies return license type by name
     */
    @Test
    void getLicenseType_shouldReturnLicenseTypeByName() throws Exception {
        LicenseType result = cache.getLicenseType("license type 1 name");
        assertNotNull(result);
        assertEquals("license type 1 name", result.getName());
    }

    /**
     * @see LicenseTypeCache#getLicenseType(String)
     * @verifies return null when name not found
     */
    @Test
    void getLicenseType_shouldReturnNullWhenNameNotFound() throws Exception {
        assertNull(cache.getLicenseType("does-not-exist"));
    }

    /**
     * @see LicenseTypeCache#getLicenseType(String)
     * @verifies return null when name is null
     */
    @Test
    void getLicenseType_shouldReturnNullWhenNameIsNull() throws Exception {
        assertNull(cache.getLicenseType(null));
    }

    /**
     * @see LicenseTypeCache#getLicenseTypes(java.util.Collection)
     * @verifies return subset matching names
     */
    @Test
    void getLicenseTypes_shouldReturnSubsetMatchingNames() throws Exception {
        List<LicenseType> result = cache.getLicenseTypes(Arrays.asList("license type 1 name", "license type 2 name"));
        assertEquals(2, result.size());
    }

    /**
     * @see LicenseTypeCache#getLicenseTypes(java.util.Collection)
     * @verifies return empty list for empty input
     */
    @Test
    void getLicenseTypes_shouldReturnEmptyListForEmptyInput() throws Exception {
        assertTrue(cache.getLicenseTypes(Collections.emptyList()).isEmpty());
    }

    /**
     * @see LicenseTypeCache#getLicenseTypes(java.util.Collection)
     * @verifies throw NullPointerException for null input
     */
    @Test
    void getLicenseTypes_shouldThrowNullPointerExceptionForNullInput() throws Exception {
        assertThrows(NullPointerException.class, () -> cache.getLicenseTypes(null));
    }

    /**
     * @see LicenseTypeCache#invalidate()
     * @verifies force reload on next read
     */
    @Test
    void invalidate_shouldForceReloadOnNextRead() throws Exception {
        List<LicenseType> first = cache.getAllLicenseTypes();
        cache.invalidate();
        List<LicenseType> second = cache.getAllLicenseTypes();
        assertEquals(first.size(), second.size());
        assertNotSame(first, second, "expected fresh snapshot after invalidate()");
    }
}
