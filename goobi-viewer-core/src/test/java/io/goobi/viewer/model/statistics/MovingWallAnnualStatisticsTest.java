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
package io.goobi.viewer.model.statistics;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MovingWallAnnualStatisticsTest {

    /**
     * @see MovingWallAnnualStatistics#getQuery()
     * @verifies build query correctly
     */
    @Test
    public void getQuery_shouldBuildQueryCorrectly() throws Exception {
        Assertions.assertEquals("+ISWORK:true +DATE_PUBLICRELEASEDATE:[2022-01-01T00:00:00.000Z TO 2022-12-31T23:59:59.999Z]",
                new MovingWallAnnualStatistics(2022).getQuery());
    }
}