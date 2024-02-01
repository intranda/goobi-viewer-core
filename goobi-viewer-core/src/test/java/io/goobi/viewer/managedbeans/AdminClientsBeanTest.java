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
package io.goobi.viewer.managedbeans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;

/**
 * @author florian
 *
 */
class AdminClientsBeanTest {

    @Test
    void test_tableDataProvider() throws DAOException {
        
        List<ClientApplication> clients = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            clients.add(createClient(i+1));
        }

        IDAO dao = Mockito.mock(IDAO.class);
        Mockito.when(dao.getAllClientApplications()).thenReturn(clients);
        
        AdminClientsBean bean = new AdminClientsBean(dao, 10);
        
        List<ClientApplication> pageList = bean.getConfiguredClientsModel().getPaginatorList();
        assertEquals(10, pageList.size());
        assertEquals("Name_1", pageList.get(0).getName());
        
        bean.getConfiguredClientsModel().cmdMoveNext();
        pageList = bean.getConfiguredClientsModel().getPaginatorList();
        assertEquals(10, pageList.size());
        assertEquals("Name_11", pageList.get(0).getName());
        
        bean.getConfiguredClientsModel().getFilter(AdminClientsBean.DEFAULT_TABLE_FILTER).setValue("10");
        bean.getConfiguredClientsModel().cmdMoveFirst();
        pageList = bean.getConfiguredClientsModel().getPaginatorList();
        assertEquals(2, pageList.size());
        assertEquals("Name_10", pageList.get(0).getName());
        assertEquals("Name_100", pageList.get(1).getName());
        
        bean.getConfiguredClientsModel().setSortField("name");
        bean.getConfiguredClientsModel().setSortOrder(SortOrder.DESCENDING);
        pageList = bean.getConfiguredClientsModel().getPaginatorList();
        assertEquals(2, pageList.size());
        assertEquals("Name_10", pageList.get(1).getName());
        assertEquals("Name_100", pageList.get(0).getName());
    }

    /**
     * @param i
     * @return
     */
    private ClientApplication createClient(int i) {
        ClientApplication client = new ClientApplication(Integer.toString(i));
        client.setName("Name_" + i);
        client.setClientIp(i + "." + i + "." + i + "." + i);
        client.setAccessStatus(AccessStatus.GRANTED);
        return client;
    }

}
