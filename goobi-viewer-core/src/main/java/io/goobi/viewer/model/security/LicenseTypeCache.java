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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * <p>
 * Application-scoped cache for {@link LicenseType} reads. Replaces repeated JPQL queries during
 * access-condition evaluation with a single immutable snapshot that is invalidated by
 * {@code JPADAO} write methods after a successful commit. See
 * {@code docs/superpowers/specs/2026-04-22-dao-access-cache-design.md}.
 * </p>
 *
 * <p>
 * Thread-safety: reads are lock-free via a {@code volatile} snapshot; the snapshot is built under
 * an internal lock on miss, and invalidation acquires the same lock before clearing the reference.
 * </p>
 *
 * <p>
 * Known limitation: changes made to the database outside the viewer's write path (e.g. direct SQL)
 * are not detected until the application is restarted.
 * </p>
 */
public class LicenseTypeCache {

    private final Object loadLock = new Object();
    // S3077 false positive: the cached list is never mutated after publication; on miss a new immutable
    // list is built under loadLock and atomically replaced via a single volatile write
    // (safe-publication idiom, JCIP §3.5.3). Reads stay lock-free.
    @SuppressWarnings("java:S3077")
    private volatile List<LicenseType> cache;

    /**
     * Returns all {@link LicenseType}s (core and non-core). The returned list is immutable.
     *
     * @return all license types as an immutable list; never null
     * @throws DAOException if the underlying DAO load fails
     * @should return all license types from dao
     * @should return same instance on second call
     * @should return immutable list
     * @should initialise lazy collections
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        return snapshot();
    }

    /**
     * Returns all non-core {@link LicenseType}s (equivalent to {@code JPADAO.getRecordLicenseTypes()}).
     *
     * @return filtered immutable list of license types with {@code core=false}
     * @throws DAOException if the underlying DAO load fails
     * @should filter out core license types
     */
    public List<LicenseType> getRecordLicenseTypes() throws DAOException {
        return snapshot().stream().filter(lt -> !lt.isCore()).toList();
    }

    /**
     * Returns the license type with the given name, or null if not found.
     *
     * @param name license type name; may be null (returns null)
     * @return matching {@link LicenseType} or null
     * @throws DAOException if the underlying DAO load fails
     * @should return license type by name
     * @should return null when name not found
     * @should return null when name is null
     */
    public LicenseType getLicenseType(String name) throws DAOException {
        if (name == null) {
            return null;
        }
        return snapshot().stream().filter(lt -> name.equals(lt.getName())).findFirst().orElse(null);
    }

    /**
     * Returns the subset of license types whose name matches any of {@code names}.
     *
     * @param names non-null collection of names; may be empty
     * @return matching license types as an immutable list; never null
     * @throws DAOException if the underlying DAO load fails
     * @should return subset matching names
     * @should return empty list for empty input
     * @should throw NullPointerException for null input
     */
    public List<LicenseType> getLicenseTypes(Collection<String> names) throws DAOException {
        Objects.requireNonNull(names, "names");
        if (names.isEmpty()) {
            return Collections.emptyList();
        }
        return snapshot().stream().filter(lt -> names.contains(lt.getName())).toList();
    }

    /**
     * Drops the current snapshot. The next read reloads from the DAO. Called by {@code JPADAO}
     * write methods after a successful commit.
     *
     * @should force reload on next read
     */
    public void invalidate() {
        synchronized (loadLock) {
            this.cache = null;
        }
    }

    private List<LicenseType> snapshot() throws DAOException {
        List<LicenseType> local = this.cache;
        if (local != null) {
            return local;
        }
        synchronized (loadLock) {
            local = this.cache;
            if (local == null) {
                IDAO dao = DataManager.getInstance().getDao();
                local = List.copyOf(dao.getAllLicenseTypesHydrated());
                this.cache = local;
            }
            return local;
        }
    }
}
