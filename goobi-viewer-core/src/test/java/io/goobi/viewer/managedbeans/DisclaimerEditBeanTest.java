/*
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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.model.administration.legal.Disclaimer;

class DisclaimerEditBeanTest {

    /**
     * @see DisclaimerEditBean#setDisclaimerActive(boolean)
     * @verifies persist new disclaimer if none exists
     */
    @Test
    void setDisclaimerActive_shouldPersistNewDisclaimerIfNoneExists() throws Exception {
        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getDisclaimer()).thenReturn(null);
        DisclaimerEditBean bean = new DisclaimerEditBean(dao);
        Mockito.clearInvocations(dao);

        bean.setDisclaimerActive(true);

        ArgumentCaptor<Disclaimer> captor = ArgumentCaptor.forClass(Disclaimer.class);
        Mockito.verify(dao).saveDisclaimer(captor.capture());
        assertTrue(captor.getValue().isActive());
        assertTrue(bean.getDisclaimerForEdit().isActive());
    }
}
