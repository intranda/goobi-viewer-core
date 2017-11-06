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
package de.intranda.digiverso.presentation.model.security;

import java.util.List;

import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;

public interface ILicensee {

    public String getName();

    public List<License> getLicenses();

    /**
     * Checks whether the licensee has a license with the given name. IF a privilege name is passed, the licensee must also have this privilege for
     * that license.
     *
     * @param licenseName License name.
     * @param privilegeName Required privilege (optional).
     * @param pi Checks the privilege in connection with a specific record identifier (optional).
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException;

    /**
     * Adds the given license to this ILicensee.
     *
     * @param license
     * @return
     */
    public boolean addLicense(License license);

    /**
     * Removes the given license from this ILicensee.
     *
     * @param license
     * @return
     */
    public boolean removeLicense(License license);
}
