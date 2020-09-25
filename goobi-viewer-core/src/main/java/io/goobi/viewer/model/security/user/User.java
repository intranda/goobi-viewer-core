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
package io.goobi.viewer.model.security.user;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.timgroup.jgravatar.Gravatar;
import com.timgroup.jgravatar.GravatarDefaultImage;
import com.timgroup.jgravatar.GravatarRating;

import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.SolrConstants;
import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.CMSPageTemplate;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.security.ILicensee;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.model.transkribus.TranskribusSession;

/**
 * <p>
 * User class.
 * </p>
 */
@Entity
@Table(name = "users")
public class User implements ILicensee, HttpSessionBindingListener, Serializable {

    private static final long serialVersionUID = 549769987121664488L;

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    /** Constant <code>ATTRIBUTE_LOGINS="logins"</code> */
    public static final String ATTRIBUTE_LOGINS = "logins";

    /** Constant <code>AVATAR_DEFAULT_SIZE=96</code> */
    public static final int AVATAR_DEFAULT_SIZE = 96;

    private static final String URI_ID_TEMPLATE = DataManager.getInstance().getConfiguration().getRestApiUrl() + "users/{id}";
    private static final String URI_ID_REGEX = ".*/users/(\\d+)/?$";

    @Transient
    private transient BCrypt bcrypt = new BCrypt();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Index(name = "index_users_email")
    @Column(name = "email", nullable = false)
    private String email;

    // TODO exclude from serialization
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Activation key for the user account. Value must be reset to null when the account is activated.
     */
    @Column(name = "activation_key")
    private String activationKey;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "last_login")
    private Date lastLogin;

    @Column(name = "active", nullable = false)
    private boolean active = false;

    @Column(name = "suspended", nullable = false)
    private boolean suspended = false;

    @Column(name = "superuser", nullable = false)
    private boolean superuser = false;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "nickname")
    private String nickName;

    @Column(name = "comments")
    private String comments;

    @Column(name = "score")
    private long score = 0;

    @Column(name = "use_gravatar")
    private boolean useGravatar = false;

    @Column(name = "agreed_to_terms_of_use")
    private boolean agreedToTermsOfUse = false;
    
//    @Column(name = "dummy")
//    private boolean dummy = false;

    /** List contains both old style OpenID 2.0 identifiers and OAuth subs. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "openid_accounts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "claimed_identifier")
    private List<String> openIdAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<License> licenses = new ArrayList<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Set<String> recordsForWhichUserMaySetRepresentativeImage = new HashSet<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Set<String> recordsForWhichUserMayEditOverviewPage = new HashSet<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Set<String> recordsForWhichUserMayDeleteOcrPage = new HashSet<>();

    @Transient
    private User copy;

    @Transient
    private TranskribusSession transkribusSession;

    /**
     * Empty constructor for XStream.
     */
    public User() {
        // the emptiness inside
    }

    /**
     * 
     * @param nickname
     */
    public User(String nickname) {
        this.nickName = nickname;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (email == null ? 0 : email.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        User other = (User) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
        } else if (!id.equals(other.id)) {
            return false;
        }
        if (email == null) {
            if (other.email != null) {
                return false;
            }
        } else if (!email.equals(other.email)) {
            return false;
        }
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public User clone() {
        User user = new User();

        user.setId(id);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setActivationKey(activationKey);
        user.setLastLogin(lastLogin);
        user.setActive(active);
        user.setSuspended(suspended);
        user.setSuperuser(superuser);
        user.setLastName(lastName);
        user.setFirstName(firstName);
        user.setNickName(nickName);
        user.setComments(comments);
        user.setScore(score);
        user.setUseGravatar(useGravatar);
        for (License license : licenses) {
            user.getLicenses().add(license);
        }
        for (String openIdAccount : openIdAccounts) {
            user.getOpenIdAccounts().add(openIdAccount);
        }

        return user;
    }

    /**
     * Returns the name best suited for displaying (depending on which values are available).
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayName() {
        if (StringUtils.isNotBlank(nickName)) {
            return nickName;
        }
        if (BeanUtils.getUserBean() != null && BeanUtils.getUserBean().isAdmin()) {
            return email;
        }

        return NetTools.scrambleEmailAddress(email);
    }

    /**
     * If the display name is the e-mail address and the logged in user (!= this user) is not an superuser, obfuscate the address.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayNameObfuscated() {
        String displayName = getDisplayName();
        if (!displayName.equals(nickName) && BeanUtils.getUserBean() != null && !BeanUtils.getUserBean().isAdmin()) {
            return new StringBuilder().append(ViewerResourceBundle.getTranslation("user_anonymous", null))
                    .append(" (")
                    .append(id)
                    .append(')')
                    .toString();
        }

        return displayName;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        // logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi);
        if (StringUtils.isEmpty(privilegeName)) {
            return true;
        }
        for (License license : getLicenses()) {
            // logger.trace("license: {}, {}", license.getId(),
            // license.getPrivileges().toString());
            // logger.trace("license type: {}", license.getLicenseType().getName());
            if (license.isValid() && license.getLicenseType().getName().equals(licenseName)) {
                // TODO why not do this check right at the beginning?
                if (license.getPrivileges().contains(privilegeName) || license.getLicenseType().getPrivileges().contains(privilegeName)) {
                    if (StringUtils.isEmpty(license.getConditions())) {
                        logger.debug("Permission found for user: {} ", id);
                        return true;
                    } else if (StringUtils.isNotEmpty(pi)) {
                        // If PI and Solr condition subquery are present, check via Solr
                        StringBuilder sbQuery = new StringBuilder();
                        sbQuery.append(SolrConstants.PI).append(':').append(pi).append(" AND (").append(license.getConditions()).append(')');
                        if (DataManager.getInstance()
                                .getSearchIndex()
                                .getFirstDoc(sbQuery.toString(), Collections.singletonList(SolrConstants.IDDOC)) != null) {
                            logger.debug("Permission found for user: {} (query: {})", id, sbQuery.toString());
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * Checks whether the user has the role with the given name.
     *
     * @param roleName The role name to check.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated
    public boolean hasRole(String roleName) throws DAOException {
        // return DataManager.getInstance().getConfiguration().getUserHasRole(roleName,
        // getId());
        Role role = DataManager.getInstance().getDao().getRole(roleName);
        return !DataManager.getInstance().getDao().getUserRoles(null, this, role).isEmpty();
    }

    /**
     * Checks whether the user has the given privilege directly.
     *
     * @param privilegeName a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    @Deprecated
    public boolean hasUserPrivilege(String privilegeName) throws DAOException {
        for (UserRole role : DataManager.getInstance().getDao().getUserRoles(null, this, null)) {
            if (role.getRole().hasPrivilege(privilegeName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a list of UserGroups of which this user is the owner.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroupOwnerships() throws DAOException {
        return DataManager.getInstance().getDao().getUserGroups(this);
    }

    /**
     * <p>
     * getUserGroupMemberships.
     * </p>
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserRole> getUserGroupMemberships() throws DAOException {
        return DataManager.getInstance().getDao().getUserRoles(null, this, null);
    }

    /**
     * Returns a list of UserGroups of which this user is a member.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getUserGroupsWithMembership() throws DAOException {
        List<UserGroup> ret = new ArrayList<>();
        for (UserGroup ug : DataManager.getInstance().getDao().getAllUserGroups()) {
            if (isGroupMember(ug)) {
                ret.add(ug);
            }
        }
        return ret;
    }

    /**
     * <p>
     * isGroupMember.
     * </p>
     *
     * @param group a {@link io.goobi.viewer.model.security.user.UserGroup} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isGroupMember(UserGroup group) throws DAOException {
        for (UserRole membership : group.getMemberships()) {
            if (membership.getUser().equals(this) && membership.getUserGroup().equals(group)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns a list of all groups with this user's involvement (either as owner or member).
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<UserGroup> getAllUserGroups() throws DAOException {
        List<UserGroup> ret = getUserGroupsWithMembership();
        ret.addAll(getUserGroupOwnerships());

        return ret;
    }

    /**
     * Checks whether the user can satisfy at least one of the given access conditions with a license that contains the given privilege name. If one
     * of the conditions is OPENACCESS, true is always returned. Superusers always get access.
     *
     * @param conditionList a {@link java.util.Set} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
     * @should return true if user is superuser
     * @should return true if condition is open access
     * @should return true if user has license
     * @should return false if user has no license
     * @should return true if condition list empty
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean canSatisfyAllAccessConditions(Set<String> conditionList, String privilegeName, String pi)
            throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("canSatisfyAllAccessConditions({},{},{})", conditionList, privilegeName, pi);
        if (isSuperuser()) {
            // logger.trace("User '{}' is superuser, access granted.", getDisplayName());
            return true;
        }
        // always allow access if the only condition is open access and there is no special license configured for it
        if (conditionList.size() == 1 && conditionList.contains(SolrConstants.OPEN_ACCESS_VALUE)
                && DataManager.getInstance().getDao().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) == null) {
            return true;
        }

        Map<String, Boolean> permissionMap = new HashMap<>(conditionList.size());
        for (String accessCondition : conditionList) {
            permissionMap.put(accessCondition, false);
            // Check individual licenses
            if (hasLicense(accessCondition, privilegeName, pi)) {
                permissionMap.put(accessCondition, true);
                continue;
            }
            // Check group ownership licenses
            for (UserGroup group : getUserGroupOwnerships()) {
                if (group.hasLicense(accessCondition, privilegeName, pi)) {
                    permissionMap.put(accessCondition, true);
                    continue;
                }
            }
            // Check group membership licenses
            for (UserGroup group : getUserGroupsWithMembership()) {
                if (group.hasLicense(accessCondition, privilegeName, pi)) {
                    permissionMap.put(accessCondition, true);
                }
            }

        }
        // It should be sufficient if the user can satisfy one required licence
        return permissionMap.isEmpty() || permissionMap.containsValue(true);
        // return !permissionMap.containsValue(false);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addLicense(License license) {
        if (licenses == null) {
            licenses = new ArrayList<>();
        }
        if (!licenses.contains(license)) {
            licenses.add(license);
            license.setUser(this);
            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeLicense(License license) {
        if (license != null && licenses != null) {
            // license.setUser(null);
            return licenses.remove(license);
        }

        return false;
    }

    /**
     * <p>
     * isHasCmsPrivilege.
     * </p>
     *
     * @param privilege a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasCmsPrivilege(String privilege) throws PresentationException, IndexUnreachableException, DAOException {
        return isHasPrivilege(LicenseType.LICENSE_TYPE_CMS, privilege);
    }

    /**
     * <p>
     * isHasCrowdsourcingPrivilege.
     * </p>
     *
     * @param privilege a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasCrowdsourcingPrivilege(String privilege) throws PresentationException, IndexUnreachableException, DAOException {
        return isHasPrivilege(LicenseType.LICENSE_TYPE_CROWDSOURCING_CAMPAIGNS, privilege);
    }

    /**
     * <p>
     * isHasPrivilege.
     * </p>
     *
     * @param licenseType a {@link java.lang.String} object.
     * @param privilege a {@link java.lang.String} object.
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasPrivilege(String licenseType, String privilege) throws PresentationException, IndexUnreachableException, DAOException {
        return canSatisfyAllAccessConditions(Collections.singletonMap(licenseType, null).keySet(), privilege, null);
    }

    /**
     * Checks whether this user has the permission to set the representative image for the currently open record. TODO For some reason this method is
     * called 8x in a row.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isMaySetRepresentativeImage() throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("isMaySetRepresentativeImage");
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE, IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE,
                recordsForWhichUserMaySetRepresentativeImage);
    }

    /**
     * Checks whether this user has the permission to delete all ocr-content of one page in crowdsourcing.
     *
     * @return a boolean.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isMayDeleteCrowdsourcingFulltext() throws IndexUnreachableException, PresentationException, DAOException {
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_DELETE_OCR_PAGE, IPrivilegeHolder.PRIV_DELETE_OCR_PAGE,
                recordsForWhichUserMayDeleteOcrPage);
    }

    /**
     * 
     * @return true if there are CMS pages or campaigns created by this user; false otherwise
     */
    public boolean isCmsCreator() {

        return false;
    }

    /**
     *
     * @param licenseType
     * @param privilegeName
     * @param alreadyCheckedPiList
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private boolean isHasPrivilegeForCurrentRecord(String licenseType, String privilegeName, Set<String> alreadyCheckedPiList)
            throws IndexUnreachableException, PresentationException, DAOException {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null && adb.getViewManager() != null) {
            String pi = adb.getViewManager().getPi();
            return isHasPrivilegeForRecord(pi, licenseType, privilegeName, alreadyCheckedPiList);
        }

        return false;
    }

    /**
     *
     * @param pi
     * @param licenseType
     * @param privilegeName
     * @param alreadyCheckedPiList
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private boolean isHasPrivilegeForRecord(String pi, String licenseType, String privilegeName, Set<String> alreadyCheckedPiList)
            throws PresentationException, IndexUnreachableException, DAOException {
        if (alreadyCheckedPiList == null) {
            alreadyCheckedPiList = new HashSet<>();
        }
        if (alreadyCheckedPiList.contains(pi)) {
            return true;
        }
        if (canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(licenseType)), privilegeName, pi)) {
            alreadyCheckedPiList.add(pi);
            return true;
        }

        return false;
    }

    /**
     * Used by the crowdsourcing module.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getAvatarUrl() {
        if (useGravatar) {
            return getGravatarUrl(AVATAR_DEFAULT_SIZE);
        }

        return BeanUtils.getNavigationHelper().getApplicationUrl() + "resources/crowdsourcing/img/profile-small.png";
    }

    /**
     * Used by the crowdsourcing module.
     *
     * @param size a int.
     * @return a {@link java.lang.String} object.
     */
    public String getAvatarUrl(int size) {
        if (useGravatar) {
            return getGravatarUrl(size);
        }

        return BeanUtils.getNavigationHelper().getApplicationUrl() + "resources/crowdsourcing/img/profile-small.png";
    }

    /**
     * <p>
     * getGravatarUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getGravatarUrl() {
        return getGravatarUrl(AVATAR_DEFAULT_SIZE);
    }

    /**
     * Empty setter so that HTML pages do not throw missing property errors.
     *
     * @param gravatarUrl a {@link java.lang.String} object.
     */
    public void setGravatarUrl(String gravatarUrl) {
        // nothing
    }

    /**
     * Generates and returns a Gravatar url for the user's e-mail address.
     *
     * @param size a int.
     * @return Gravatar URL
     */
    public String getGravatarUrl(int size) {
        if (StringUtils.isNotEmpty(email)) {
            Gravatar gravatar =
                    new Gravatar().setSize(size).setRating(GravatarRating.GENERAL_AUDIENCES).setDefaultImage(GravatarDefaultImage.IDENTICON);
            String url = gravatar.getUrl(email);
            return url.replace("http:", "");
        }

        return "//www.gravatar.com/avatar/";
    }

    /**
     * Generates salt and a password hash for the given password string.
     *
     * @param password a {@link java.lang.String} object.
     * @return a boolean.
     */
    public boolean setNewPassword(String password) {
        if (StringUtils.isNotBlank(password)) {
            setPasswordHash(BCrypt.hashpw(password, BCrypt.gensalt()));
            return true;
        }

        return false;
    }

    /**
     * Authentication check for regular (i.e. non-OpenID) accounts.
     *
     * @param email a {@link java.lang.String} object.
     * @param password a {@link java.lang.String} object.
     * @return The user, if successful.
     * @throws io.goobi.viewer.exceptions.AuthenticationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User auth(String email, String password) throws AuthenticationException, DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only allow non-openID accounts
        if (user != null && user.getPasswordHash() != null && bcrypt.checkpw(password, user.getPasswordHash())) {
            user.setLastLogin(new Date());
            return user;
        }

        throw new AuthenticationException("User not found or passwort incorrect");
    }

    /**
     * <p>
     * hasPriviledgeForAllTemplates.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPriviledgeForAllTemplates() {

        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return false;
        }
        // Full admins get all values
        if (isSuperuser()) {
            return true;
        }

        List<License> allLicenses = new ArrayList<>(licenses);
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).collect(Collectors.toList()));
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllTemplates());
    }

    /**
     * <p>
     * hasPrivilegesForTemplate.
     * </p>
     *
     * @param templateId a {@link java.lang.String} object.
     * @return true exactly if the user is not restricted to certain cmsTemplates or if the given templateId is among the allowed templates for the
     *         user of a usergroup she is in
     */
    public boolean hasPrivilegesForTemplate(String templateId) {
        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return false;
        }
        // Full admins get all values
        if (hasPriviledgeForAllTemplates()) {
            return true;
        }

        // Check user licenses
        for (License license : licenses) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            if (license.getAllowedCmsTemplates().contains(templateId)) {
                return true;
            }
        }

        // Check user group licenses
        try {
            for (UserGroup userGroup : getUserGroupsWithMembership()) {
                for (License license : userGroup.getLicenses()) {
                    if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                        continue;
                    }
                    if (license.getAllowedCmsTemplates().contains(templateId)) {
                        return true;
                    }
                }
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * <p>
     * getAllowedTemplates.
     * </p>
     *
     * @param allTemplates a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public List<CMSPageTemplate> getAllowedTemplates(List<CMSPageTemplate> allTemplates) {
        if (allTemplates == null || allTemplates.isEmpty()) {
            return allTemplates;
        }
        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return Collections.emptyList();
        }
        // Full admins get all values
        if (isSuperuser()) {
            return allTemplates;
        }

        Set<String> allowedTemplateIds = new HashSet<>(allTemplates.size());
        // Check user licenses
        for (License license : licenses) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            // If no restriction is set, return all values
            if (license.isPrivCmsAllTemplates()) {
                return allTemplates;
            }
            if (!license.getAllowedCmsTemplates().isEmpty()) {
                allowedTemplateIds.addAll(license.getAllowedCmsTemplates());
            }
        }
        // Check user group licenses
        try {
            for (UserGroup userGroup : getUserGroupsWithMembership()) {
                for (License license : userGroup.getLicenses()) {
                    if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                        continue;
                    }
                    // If no restriction is set, return all values
                    if (license.isPrivCmsAllTemplates()) {
                        return allTemplates;
                    }
                    if (!license.getAllowedCmsTemplates().isEmpty()) {
                        allowedTemplateIds.addAll(license.getAllowedCmsTemplates());
                    }
                }
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }
        // allowedTemplateIds.add("template_general_generic");
        if (allowedTemplateIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<CMSPageTemplate> ret = new ArrayList<>(allTemplates.size());
        for (CMSPageTemplate template : allTemplates) {
            if (allowedTemplateIds.contains(template.getId())) {
                ret.add(template);
            }
        }

        logger.trace("getAllowedTemplates END");
        return ret;
    }

    /**
     * <p>
     * hasPrivilegeForAllCategories.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPrivilegeForAllCategories() {

        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return false;
        }
        // Full admins get all values
        if (isSuperuser()) {
            return true;
        }

        List<License> allLicenses = new ArrayList<>(licenses);
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).collect(Collectors.toList()));
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllCategories());
    }

    /**
     * <p>
     * getAllowedCategories.
     * </p>
     *
     * @param allCategories a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public List<CMSCategory> getAllowedCategories(List<CMSCategory> allCategories) {
        if (allCategories == null || allCategories.isEmpty()) {
            return allCategories;
        }
        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return Collections.emptyList();
        }
        // Full admins get all values
        if (isSuperuser()) {
            return allCategories;
        }

        List<CMSCategory> ret = new ArrayList<>(allCategories.size());
        // Check user licenses
        for (License license : licenses) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            // If no restriction is set, return all values
            if (license.isPrivCmsAllCategories()) {
                return allCategories;
            }
            if (!license.getAllowedCategories().isEmpty()) {
                ret.addAll(license.getAllowedCategories());
            }
        }
        // Check user group licenses
        try {
            for (UserGroup userGroup : getUserGroupsWithMembership()) {
                for (License license : userGroup.getLicenses()) {
                    if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                        continue;
                    }
                    // If no restriction is set, return all values
                    if (license.isPrivCmsAllCategories()) {
                        return allCategories;
                    }
                    if (!license.getAllowedCategories().isEmpty()) {
                        ret.addAll(license.getAllowedCategories());
                    }
                }
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * <p>
     * hasPrivilegeForAllSubthemeDiscriminatorValues.
     * </p>
     *
     * @return a boolean.
     */
    public boolean hasPrivilegeForAllSubthemeDiscriminatorValues() {

        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return false;
        }
        // Full admins get all values
        if (isSuperuser()) {
            return true;
        }

        List<License> allLicenses = new ArrayList<>(licenses);
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).collect(Collectors.toList()));
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllSubthemes());
    }

    /**
     * <p>
     * getAllowedSubthemeDiscriminatorValues.
     * </p>
     *
     * @param rawValues All possible values
     * @return filtered list of allowed values
     */
    public List<String> getAllowedSubthemeDiscriminatorValues(List<String> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return rawValues;
        }
        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return Collections.emptyList();
        }
        // Full admins get all values
        if (isSuperuser()) {
            return rawValues;
        }

        List<String> ret = new ArrayList<>(rawValues.size());
        // Check user licenses
        for (License license : licenses) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            // If no restriction is set, return all values
            if (license.isPrivCmsAllSubthemes()) {
                return rawValues;
            }
            if (!license.getAllowedCmsTemplates().isEmpty()) {
                ret.addAll(license.getSubthemeDiscriminatorValues());
            }
        }
        // Check user group licenses
        try {
            for (UserGroup userGroup : getUserGroupsWithMembership()) {
                for (License license : userGroup.getLicenses()) {
                    if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                        continue;
                    }
                    // If no restriction is set, return all values
                    if (license.isPrivCmsAllSubthemes()) {
                        return rawValues;
                    }
                    if (!license.getAllowedCmsTemplates().isEmpty()) {
                        ret.addAll(license.getSubthemeDiscriminatorValues());
                    }
                }
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return ret;
    }

    /**
     * <p>
     * getAllowedCrowdsourcingCampaigns.
     * </p>
     *
     * @param allCampaigns a {@link java.util.List} object.
     * @return a {@link java.util.List} object.
     */
    public List<Campaign> getAllowedCrowdsourcingCampaigns(List<Campaign> allCampaigns) {
        if (allCampaigns == null || allCampaigns.isEmpty()) {
            return allCampaigns;
        }

        // Full admins get all values
        if (isSuperuser()) {
            return allCampaigns;
        }

        List<Campaign> ret = new ArrayList<>(allCampaigns.size());
        for (Campaign campaign : allCampaigns) {
            logger.trace("campaign: {}", campaign.getTitle());
            // Skip inactive campaigns
            if (!campaign.isHasStarted() || campaign.isHasEnded()) {
                continue;
            }
            switch (campaign.getVisibility()) {
                case PUBLIC:
                    ret.add(campaign);
                    break;
                case RESTRICTED:
                    // Check user licenses
                    for (License license : licenses) {
                        if (!LicenseType.LICENSE_TYPE_CROWDSOURCING_CAMPAIGNS.equals(license.getLicenseType().getName())) {
                            continue;
                        }
                        if (license.getAllowedCrowdsourcingCampaigns().contains(campaign)) {
                            ret.add(campaign);
                            break;
                        }
                    }
                    // Check user group licenses
                    try {
                        for (UserGroup userGroup : getUserGroupsWithMembership()) {
                            for (License license : userGroup.getLicenses()) {
                                if (!LicenseType.LICENSE_TYPE_CROWDSOURCING_CAMPAIGNS.equals(license.getLicenseType().getName())) {
                                    continue;
                                }
                                if (license.getAllowedCrowdsourcingCampaigns().contains(campaign)) {
                                    ret.add(campaign);
                                    break;
                                }
                            }
                        }
                    } catch (DAOException e) {
                        logger.error(e.getMessage(), e);
                    }
                    break;
                default:
                    break;
            }
        }

        logger.trace("{} allowed campaigns", ret.size());
        return ret;
    }

    /***********************************
     * Getter and Setter
     ***************************************/

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>passwordHash</code>.
     * </p>
     *
     * @return the passwordHash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * <p>
     * Setter for the field <code>passwordHash</code>.
     * </p>
     *
     * @param passwordHash the passwordHash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * <p>
     * Getter for the field <code>activationKey</code>.
     * </p>
     *
     * @return the activationKey
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * <p>
     * Setter for the field <code>activationKey</code>.
     * </p>
     *
     * @param activationKey the activationKey to set
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * <p>
     * Getter for the field <code>lastLogin</code>.
     * </p>
     *
     * @return the lastLogin
     */
    public Date getLastLogin() {
        return lastLogin;
    }

    /**
     * <p>
     * Setter for the field <code>lastLogin</code>.
     * </p>
     *
     * @param lastLogin the lastLogin to set
     */
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * <p>
     * isActive.
     * </p>
     *
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * <p>
     * Setter for the field <code>active</code>.
     * </p>
     *
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * <p>
     * isSuspended.
     * </p>
     *
     * @return the suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * <p>
     * Setter for the field <code>suspended</code>.
     * </p>
     *
     * @param suspended the suspended to set
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * <p>
     * Getter for the field <code>nickName</code>.
     * </p>
     *
     * @return the nickName
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * <p>
     * Setter for the field <code>nickName</code>.
     * </p>
     *
     * @param nickName the nickName to set
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * <p>
     * Getter for the field <code>lastName</code>.
     * </p>
     *
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * <p>
     * Setter for the field <code>lastName</code>.
     * </p>
     *
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * <p>
     * Getter for the field <code>firstName</code>.
     * </p>
     *
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * <p>
     * Setter for the field <code>firstName</code>.
     * </p>
     *
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * <p>
     * Getter for the field <code>openIdAccounts</code>.
     * </p>
     *
     * @return the openIdAccounts
     */
    public List<String> getOpenIdAccounts() {
        return openIdAccounts;
    }

    /**
     * <p>
     * Setter for the field <code>openIdAccounts</code>.
     * </p>
     *
     * @param openIdAccounts the openIdAccounts to set
     */
    public void setOpenIdAccounts(List<String> openIdAccounts) {
        this.openIdAccounts = openIdAccounts;
    }

    /**
     * <p>
     * Setter for the field <code>email</code>.
     * </p>
     *
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * <p>
     * Getter for the field <code>email</code>.
     * </p>
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * <p>
     * Getter for the field <code>comments</code>.
     * </p>
     *
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * <p>
     * Setter for the field <code>comments</code>.
     * </p>
     *
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * <p>
     * Getter for the field <code>score</code>.
     * </p>
     *
     * @return the score
     */
    public long getScore() {
        return score;
    }

    /**
     * <p>
     * Setter for the field <code>score</code>.
     * </p>
     *
     * @param score the score to set
     */
    public void setScore(long score) {
        this.score = score;
    }

    /**
     * <p>
     * isUseGravatar.
     * </p>
     *
     * @return the useGravatar
     */
    public boolean isUseGravatar() {
        return useGravatar;
    }

    /**
     * <p>
     * Setter for the field <code>useGravatar</code>.
     * </p>
     *
     * @param useGravatar the useGravatar to set
     */
    public void setUseGravatar(boolean useGravatar) {
        this.useGravatar = useGravatar;
    }

//    /**
//     * @return the dummy
//     */
//    public boolean isDummy() {
//        return dummy;
//    }
//
//    /**
//     * @param dummy the dummy to set
//     */
//    public void setDummy(boolean dummy) {
//        this.dummy = dummy;
//    }

    /**
     * 
     * @return true if user email address equals the configured anonymous user address; false otherwise
     */
    public boolean isAnonymous() {
        String anonymousAddress = DataManager.getInstance().getConfiguration().getAnonymousUserEmailAddress();
        return StringUtils.isNotEmpty(anonymousAddress) && anonymousAddress.equals(email);
    }

    /**
     * <p>
     * raiseScore.
     * </p>
     *
     * @param amount a int.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void raiseScore(int amount) throws DAOException {
        score += amount;
        DataManager.getInstance().getDao().updateUser(this);
    }

    /**
     * <p>
     * getRank.
     * </p>
     *
     * @return a int.
     */
    public int getRank() {
        if (score < 100) {
            return 0;
        }
        if (score < 500) {
            return 1;
        }
        if (score < 1000) {
            return 2;
        }

        return 3;
    }

    /** {@inheritDoc} */
    @Override
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * <p>
     * Setter for the field <code>licenses</code>.
     * </p>
     *
     * @param licenses the licenses to set
     */
    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    /**
     * <p>
     * isSuperuser.
     * </p>
     *
     * @return the superuser
     */
    public boolean isSuperuser() {
        return superuser;
    }

    /**
     * <p>
     * isCmsAdmin.
     * </p>
     *
     * @return true if user is superuser or has CMS-specific privileges
     */
    public boolean isCmsAdmin() {
        try {
            return isSuperuser() || isHasCmsPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException e) {
            logger.error(e.getMessage(), e);
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * <p>
     * Setter for the field <code>superuser</code>.
     * </p>
     *
     * @param superuser the superuser to set
     */
    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    /**
     * <p>
     * isOpenIdUser.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isOpenIdUser() {
        return openIdAccounts != null && !openIdAccounts.isEmpty();
    }

    /**
     * <p>
     * Getter for the field <code>copy</code>.
     * </p>
     *
     * @return the copy
     */
    public User getCopy() {
        return copy;
    }

    /**
     * <p>
     * Setter for the field <code>copy</code>.
     * </p>
     *
     * @param copy the copy to set
     */
    public void setCopy(User copy) {
        this.copy = copy;
    }

    /**
     * <p>
     * Getter for the field <code>transkribusSession</code>.
     * </p>
     *
     * @return the transkribusSession
     */
    public TranskribusSession getTranskribusSession() {
        return transkribusSession;
    }

    /**
     * <p>
     * Setter for the field <code>transkribusSession</code>.
     * </p>
     *
     * @param transkribusSession the transkribusSession to set
     */
    public void setTranskribusSession(TranskribusSession transkribusSession) {
        this.transkribusSession = transkribusSession;
    }

    /**
     * {@inheritDoc}
     *
     * Required by the ILicensee interface.
     */
    @Override
    public String getName() {
        if (StringUtils.isNotEmpty(getLastName())) {
            return getLastName() + ", " + getFirstName();
        }

        return getDisplayName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.
     * HttpSessionBindingEvent)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void valueBound(HttpSessionBindingEvent event) {
        // Add this user to the global set of logged in users
        Set<User> logins = (Set<User>) event.getSession().getServletContext().getAttribute(ATTRIBUTE_LOGINS);
        if (logins == null) {
            logins = new HashSet<>();
            event.getSession().getServletContext().setAttribute(ATTRIBUTE_LOGINS, logins);
        }
        logins.add(this);
        logger.debug("User added to context: {}", getId());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http
     * .HttpSessionBindingEvent)
     */
    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        // Remove this user from the global set of logged in users
        Set<User> logins = (Set<User>) event.getSession().getServletContext().getAttribute(ATTRIBUTE_LOGINS);
        if (logins != null) {
            logins.remove(this);
            logger.debug("User removed from context: {}", getId());
        }
    }

    /**
     * <p>
     * backupFields.
     * </p>
     */
    public void backupFields() {
        copy = clone();
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * <p>
     * setBCrypt.
     * </p>
     *
     * @param bcrypt a {@link io.goobi.viewer.controller.BCrypt} object.
     */
    protected void setBCrypt(BCrypt bcrypt) {
        this.bcrypt = bcrypt;
    }

    /**
     * Get the {@link io.goobi.viewer.model.security.user.User#id} of a user from a URI
     *
     * @param idAsURI a {@link java.net.URI} object.
     * @return a {@link java.lang.Long} object.
     */
    public static Long getId(URI idAsURI) {
        if (idAsURI == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(URI_ID_REGEX).matcher(idAsURI.toString());
        if (matcher.find()) {
            String idString = matcher.group(1);
            return Long.parseLong(idString);
        }

        return null;
    }

    /**
     * <p>
     * getIdAsURI.
     * </p>
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
    }

    /**
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("halbgeviertstrich", BCrypt.gensalt()));
    }
    
    /**
     * @param agreedToTermsOfUse the agreedToTermsOfUse to set
     */
    public void setAgreedToTermsOfUse(boolean agreedToTermsOfUse) {
        this.agreedToTermsOfUse = agreedToTermsOfUse;
    }
    
    /**
     * @return the agreedToTermsOfUse
     */
    public boolean isAgreedToTermsOfUse() {
        return agreedToTermsOfUse;
    }
}
