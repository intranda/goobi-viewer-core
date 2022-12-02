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

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import io.goobi.viewer.AbstractDatabaseEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.user.icon.UserAvatarOption;
import io.goobi.viewer.solr.SolrConstants;

public class UserTest extends AbstractDatabaseEnabledTest {
    
    /**
     * @see User#User(User)
     * @verifies clone blueprint correctly
     */
    @Test
    public void User_shouldCloneBlueprintCorrectly() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        License license = new License();
        
        User blueprint = new User();
        
        blueprint.setId(123L);
        blueprint.setEmail("foo@example.com");
        blueprint.setPasswordHash("!§$%");
        blueprint.setActivationKey("555");
        blueprint.setLastLogin(now);
        blueprint.setActive(true);
        blueprint.setSuspended(true);
        blueprint.setSuperuser(true);
        blueprint.setLastName("Last");
        blueprint.setFirstName("First");
        blueprint.setNickName("nn");
        blueprint.setComments("Lorem ipsum");
        blueprint.setScore(22);
        blueprint.setAgreedToTermsOfUse(true);
        blueprint.setAvatarType(UserAvatarOption.GRAVATAR);
        blueprint.setLocalAvatarUpdated(DateTools.getMillisFromLocalDateTime(now, false));
        blueprint.getLicenses().add(license);
        blueprint.getOpenIdAccounts().add("google:foo@example.com");
        blueprint.getUserProperties().put("foo", "bar");
        
        User clone = new User(blueprint);
        Assert.assertEquals(blueprint.getId(), clone.getId());
        Assert.assertEquals(blueprint.getEmail(), clone.getEmail());
        Assert.assertEquals(blueprint.getPasswordHash(), clone.getPasswordHash());
        Assert.assertEquals(blueprint.getActivationKey(), clone.getActivationKey());
        Assert.assertEquals(blueprint.getLastLogin(), clone.getLastLogin());
        Assert.assertEquals(blueprint.isActive(), clone.isActive());
        Assert.assertEquals(blueprint.isSuspended(), clone.isSuspended());
        Assert.assertEquals(blueprint.isSuperuser(), clone.isSuperuser());
        Assert.assertEquals(blueprint.getLastName(), clone.getLastName());
        Assert.assertEquals(blueprint.getFirstName(), clone.getFirstName());
        Assert.assertEquals(blueprint.getNickName(), clone.getNickName());
        Assert.assertEquals(blueprint.getComments(), clone.getComments());
        Assert.assertEquals(blueprint.getScore(), clone.getScore());
        Assert.assertEquals(blueprint.isAgreedToTermsOfUse(), clone.isAgreedToTermsOfUse());
        Assert.assertEquals(blueprint.getAvatarType(), clone.getAvatarType());
        Assert.assertEquals(blueprint.getLocalAvatarUpdated(), clone.getLocalAvatarUpdated());
        
        Assert.assertEquals(1, clone.getLicenses().size());
        Assert.assertEquals(license, clone.getLicenses().get(0));
        
        Assert.assertEquals(1, clone.getOpenIdAccounts().size());
        Assert.assertEquals(blueprint.getOpenIdAccounts().get(0), clone.getOpenIdAccounts().get(0));
        
        Assert.assertEquals(1, clone.getUserProperties().size());
        Assert.assertEquals(blueprint.getUserProperties().get("foo"), clone.getUserProperties().get("foo"));
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition is open access
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionIsOpenAccess() throws Exception {
        User user = new User();
        user.setSuperuser(false);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(SolrConstants.OPEN_ACCESS_VALUE)),
                IPrivilegeHolder.PRIV_LIST, "PPN123").isGranted());
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if user is superuser
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfUserIsSuperuser() throws Exception {
        User user = new User();
        user.setSuperuser(true);
        Assert.assertTrue(
                user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("restricted")), IPrivilegeHolder.PRIV_LIST, "PPN123")
                        .isGranted());
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if user has license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfUserHasLicense() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        List<String> licenceTypes = Arrays.asList(new String[] { "license type 1 name", "license type 3 name" });
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<>(licenceTypes), IPrivilegeHolder.PRIV_LIST, "PPN123").isGranted());
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return false if user has no license
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnFalseIfUserHasNoLicense() throws Exception {
        User user = DataManager.getInstance().getDao().getUser(2);
        Assert.assertNotNull(user);
        Assert.assertFalse(user.canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList("license type 1 name")),
                IPrivilegeHolder.PRIV_VIEW_IMAGES, "PPN123").isGranted());
    }

    /**
     * @see User#canSatisfyAllAccessConditions(Set,String,String)
     * @verifies return true if condition list empty
     */
    @Test
    public void canSatisfyAllAccessConditions_shouldReturnTrueIfConditionListEmpty() throws Exception {
        User user = new User();
        user.setSuperuser(false);
        Assert.assertTrue(user.canSatisfyAllAccessConditions(new HashSet<String>(0), IPrivilegeHolder.PRIV_LIST, "PPN123").isGranted());
    }

    /**
     * @see User#getId(URI)
     * @verifies extract id correctly
     */
    @Test
    public void getId_shouldExtractIdCorrectly() throws Exception {
        Assert.assertEquals(Long.valueOf(1234567890L), User.getId(new URI("https://example.com/viewer/users/1234567890/")));
    }
}
