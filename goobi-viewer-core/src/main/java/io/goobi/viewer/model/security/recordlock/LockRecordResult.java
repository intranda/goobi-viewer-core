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
package io.goobi.viewer.model.security.recordlock;

/**
 * Possible result for a record lock action, specifically {@link RecordLockManager#lockRecord(String, String, Integer)}
 */
public enum LockRecordResult {

    /**
     * The action was carried out and the record is locked
     */
    RECORD_LOCKED,
    /**
     * No action was carried out, either because no locking is required, or because the record is already locked by this sessions
     */
    NO_ACTION,
    /**
     * The record could not be locked because the lock limit has already been exceeded
     */
    LIMIT_EXCEEDED;

}
