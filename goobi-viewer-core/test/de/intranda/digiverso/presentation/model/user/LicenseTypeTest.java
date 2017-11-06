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
package de.intranda.digiverso.presentation.model.user;

import java.util.Calendar;

import org.junit.Assert;
import org.junit.Test;

import de.intranda.digiverso.presentation.model.security.LicenseType;

public class LicenseTypeTest {

    /**
     * @see LicenseType#getProcessedConditions()
     * @verifies replace NOW/YEAR with the current year if not using a date field
     */
    @Test
    public void getProcessedConditions_shouldReplaceNOWYEARWithTheCurrentYearIfNotUsingADateField() throws Exception {
        LicenseType lt = new LicenseType();
        lt.setConditions("-MDNUM_PUBLICRELEASEYEAR:[* TO NOW/YEAR]");
        Assert.assertEquals("-MDNUM_PUBLICRELEASEYEAR:[* TO " + String.valueOf(Calendar.getInstance().get(Calendar.YEAR)) + "]", lt
                .getProcessedConditions());
        lt.setConditions("-DATE_PUBLICRELEASEDATE:[* TO NOW/DAY]");
        Assert.assertEquals("-DATE_PUBLICRELEASEDATE:[* TO NOW/DAY]", lt.getProcessedConditions());
    }
}