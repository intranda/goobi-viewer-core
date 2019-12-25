/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.model.security;

import java.util.List;

import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;

/**
 * <p>ILicensee interface.</p>
 *
 */
public interface ILicensee {

    /**
     * <p>getName.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getName();

    /**
     * <p>getLicenses.</p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<License> getLicenses();

    /**
     * Checks whether the licensee has a license with the given name. IF a privilege name is passed, the licensee must also have this privilege for
     * that license.
     *
     * @param licenseName License name.
     * @param privilegeName Required privilege (optional).
     * @param pi Checks the privilege in connection with a specific record identifier (optional).
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException
     * @throws io.goobi.viewer.exceptions.PresentationException
     * @return a boolean.
     */
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException;

    /**
     * Adds the given license to this ILicensee.
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a boolean.
     */
    public boolean addLicense(License license);

    /**
     * Removes the given license from this ILicensee.
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a boolean.
     */
    public boolean removeLicense(License license);
}
