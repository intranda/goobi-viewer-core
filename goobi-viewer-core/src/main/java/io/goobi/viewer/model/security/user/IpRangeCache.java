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
package io.goobi.viewer.model.security.user;

import java.util.List;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * <p>
 * Application-scoped cache for {@link IpRange} reads. See
 * {@code docs/superpowers/specs/2026-04-22-dao-access-cache-design.md}.
 * </p>
 *
 * <p>
 * Thread-safety and invalidation contract match {@code io.goobi.viewer.model.security.LicenseTypeCache}.
 * </p>
 */
public class IpRangeCache {

    private final Object loadLock = new Object();
    private volatile List<IpRange> cache;

    /**
     * Returns all {@link IpRange}s as an immutable list.
     *
     * @return immutable list of IP ranges; never null
     * @throws DAOException if the underlying DAO load fails
     * @should return all IP ranges from dao
     * @should return same instance on second call
     * @should return immutable list
     * @should initialise lazy collections
     */
    public List<IpRange> getAllIpRanges() throws DAOException {
        return snapshot();
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

    private List<IpRange> snapshot() throws DAOException {
        List<IpRange> local = this.cache;
        if (local != null) {
            return local;
        }
        synchronized (loadLock) {
            local = this.cache;
            if (local == null) {
                IDAO dao = DataManager.getInstance().getDao();
                local = List.copyOf(dao.getAllIpRangesHydrated());
                this.cache = local;
            }
            return local;
        }
    }
}
