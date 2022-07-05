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
package io.goobi.viewer.model.statistics.usage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.time.LocalDate;

import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;

/**
 * @author florian
 *
 */
public class DailySessionUsageStatisticsTest extends AbstractDatabaseEnabledTest {

    @Test
    public void test_persistence() throws DAOException {
        
        IDAO dao = DataManager.getInstance().getDao();
        LocalDate date = LocalDate.now();
        
        
        DailySessionUsageStatistics stats = new DailySessionUsageStatistics(date, "viewer-test");
        
        dao.addUsageStatistics(stats);
        
        DailySessionUsageStatistics loaded = dao.getUsageStatistics(date);
        assertNotNull(loaded);
        assertEquals(stats.getId(), loaded.getId());
        assertEquals(stats.getViewerInstance(), loaded.getViewerInstance());
    }

}
