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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.clients.ClientApplication.AccessStatus;
import io.goobi.viewer.model.security.clients.ClientApplicationManager;

/**
 * @author florian
 *
 */
class AccessConditionUtilsClientsTest extends AbstractDatabaseEnabledTest {

    LicenseType lt;
    License license;
    ClientApplication client;
    ClientApplication allClients;
    Set<String> recordAccessConditions = new HashSet<>();

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        lt = new LicenseType();
        lt.setName("license type 1 name");

        license = new License();
        license.setLicenseType(lt);
        license.setPrivileges(Collections.singleton(IPrivilegeHolder.PRIV_LIST));

        client = new ClientApplication("12345");
        client.setAccessStatus(AccessStatus.GRANTED);

        recordAccessConditions.add(lt.getName());

        allClients = new ClientApplication(ClientApplicationManager.GENERAL_CLIENT_IDENTIFIER);
        // new ArrayList<>(allClients.getLicenses()).forEach(l -> allClients.removeLicense(l));
        ClientApplicationManager manager = new ClientApplicationManager(DataManager.getInstance().getDao()) {
            public ClientApplication getAllClientsFromDatabase() {
                return allClients;
            };
        };
        manager.setAllClients(allClients);
        DataManager.getInstance().setClientManager(manager);
    }

    @Test
    void checkAccessPermission_shouldReturnFalseIfClientNotContainsLicense() throws Exception {
        Assertions.assertFalse(AccessConditionUtils.checkAccessPermission(Arrays.asList(lt), recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.of(client), null).isGranted());
    }

    @Test
    void checkAccessPermission_shouldReturnTrueIfClientContainsLicense() throws Exception {
        license.getLicensees().get(0).setClient(client);
        DataManager.getInstance().getDao().addLicense(license);
        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(Arrays.asList(lt), recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.of(client), null).isGranted());
    }

    @Test
    void checkAccessPermission_shouldReturnTrueIfAllClientsContainsLicense() throws Exception {
        license.getLicensees().get(0).setClient(client);
        DataManager.getInstance().getDao().addLicense(license);
        Set<String> conditions = new HashSet<>();
        conditions.add(lt.getName());
        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(Arrays.asList(lt), conditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.of(client), null).isGranted());
    }

    @Test
    void checkAccessPermission_shouldReturnFalseIfClientIsOutsiteIpRange() throws Exception {
        license.getLicensees().get(0).setClient(client);
        DataManager.getInstance().getDao().addLicense(license);
        
        License license2 = new License();
        license2.setLicenseType(lt);
        license2.setPrivileges(Collections.singleton(IPrivilegeHolder.PRIV_LIST));
        license2.getLicensees().get(0).setClient(allClients);
        DataManager.getInstance().getDao().addLicense(license2);

        client.setSubnetMask("11.22.33.45/32");

        Assertions.assertFalse(AccessConditionUtils.checkAccessPermission(Arrays.asList(lt), recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.of(client), null).isGranted());
    }

    @Test
    void checkAccessPermission_shouldReturnTrueIfClientIsInsideIpRange() throws Exception {
        license.getLicensees().get(0).setClient(client);
        DataManager.getInstance().getDao().addLicense(license);
        
        License license2 = new License();
        license2.setLicenseType(lt);
        license2.setPrivileges(Collections.singleton(IPrivilegeHolder.PRIV_LIST));
        license2.getLicensees().get(0).setClient(allClients);
        DataManager.getInstance().getDao().addLicense(license2);
        
        client.setSubnetMask("11.22.33.45/31");

        Assertions.assertTrue(AccessConditionUtils.checkAccessPermission(Arrays.asList(lt), recordAccessConditions, IPrivilegeHolder.PRIV_LIST, null,
                "11.22.33.44", Optional.of(client), null).isGranted());
    }
}
