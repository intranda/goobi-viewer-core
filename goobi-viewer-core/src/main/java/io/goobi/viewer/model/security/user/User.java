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

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.PrivateOwned;

import de.unigoettingen.sub.commons.cache.ContentServerCacheManager;
import de.unigoettingen.sub.commons.util.PathConverter;
import io.goobi.viewer.api.rest.v1.authentication.UserAvatarResource;
import io.goobi.viewer.controller.BCrypt;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.FileTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.exceptions.AuthenticationException;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.security.AccessPermission;
import io.goobi.viewer.model.security.IPrivilegeHolder;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.License.AccessType;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.icon.UserAvatarOption;
import io.goobi.viewer.model.transkribus.TranskribusSession;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import jakarta.servlet.http.Part;

/**
 * Represents a registered viewer user with authentication credentials, roles, and associated licences.
 */
@Entity
@Table(name = "viewer_users")
public class User extends AbstractLicensee implements HttpSessionBindingListener, Serializable, Comparable<User> {

    private static final long serialVersionUID = 549769987121664488L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(User.class);

    /** Constant <code>ATTRIBUTE_LOGINS="logins"</code>. */
    public static final String ATTRIBUTE_LOGINS = "logins";

    /** Constant <code>AVATAR_DEFAULT_SIZE=140</code>. */
    public static final int AVATAR_DEFAULT_SIZE = 140;

    private static final String URI_ID_TEMPLATE = DataManager.getInstance().getConfiguration().getRestApiUrl() + "users/{id}";
    private static final String URI_ID_REGEX = "/users/(\\d{1,19})/?$";

    static final String EMAIL_ADDRESS_ANONYMOUS = "anonymous@goobi.io";

    @Transient
    private transient BCrypt bcrypt = new BCrypt();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Index(name = "index_users_email")
    @Column(name = "email", nullable = false)
    private String email;

    // TODO exclude from serialization (without using the "transient" keyword)
    @Column(name = "password_hash")
    private String passwordHash;

    /**
     * Activation key for the user account. Value must be reset to null when the account is activated.
     */
    @Column(name = "activation_key")
    private String activationKey;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

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

    @Column(name = "avatar_type")
    @Enumerated(EnumType.STRING)
    private UserAvatarOption avatarType = UserAvatarOption.DEFAULT;

    @Column(name = "local_avatar_updated", nullable = true)
    private Long localAvatarUpdated = null;

    @Column(name = "agreed_to_terms_of_use")
    private boolean agreedToTermsOfUse = false;

    /** List contains both old style OpenID 2.0 identifiers and OAuth subs. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "openid_accounts", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "claimed_identifier")
    private List<String> openIdAccounts = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_properties", joinColumns = @JoinColumn(name = "user_id"))
    @MapKeyColumn(name = "property_name")
    @Column(name = "property_value")
    @PrivateOwned
    private Map<String, String> userProperties = new HashMap<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Map<String, AccessPermission> recordsForWhichUserMaySetRepresentativeImage = new HashMap<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Map<String, AccessPermission> recordsForWhichUserMayEditOverviewPage = new HashMap<>();

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    private Map<String, AccessPermission> recordsForWhichUserMayDeleteOcrPage = new HashMap<>();

    @Transient
    private User copy;

    @Transient
    private TranskribusSession transkribusSession;

    /**
     * Empty constructor.
     */
    public User() {
        // the emptiness inside
    }

    /**
     *
     * @param nickname the display nickname to assign to this user
     */
    public User(String nickname) {
        this.nickName = nickname;
    }

    /**
     * Cloning constructor.
     * 
     * @param blueprint User to clone
     * @should clone blueprint correctly
     */
    public User(User blueprint) {
        if (blueprint == null) {
            throw new IllegalArgumentException("blueprint may not be null");
        }

        setId(blueprint.id);
        setEmail(blueprint.email);
        setPasswordHash(blueprint.passwordHash);
        setActivationKey(blueprint.activationKey);
        setLastLogin(blueprint.lastLogin);
        setActive(blueprint.active);
        setSuspended(blueprint.suspended);
        setSuperuser(blueprint.superuser);
        setLastName(blueprint.lastName);
        setFirstName(blueprint.firstName);
        setNickName(blueprint.nickName);
        setComments(blueprint.comments);
        setScore(blueprint.score);
        setAgreedToTermsOfUse(blueprint.agreedToTermsOfUse);
        setAvatarType(blueprint.avatarType);
        setLocalAvatarUpdated(blueprint.localAvatarUpdated);
        // Clone OpenID identifiers
        for (String openIdAccount : blueprint.getOpenIdAccounts()) {
            getOpenIdAccounts().add(openIdAccount);
        }
        // Clone properties
        for (Entry<String, String> entry : blueprint.getUserProperties().entrySet()) {
            userProperties.put(entry.getKey(), entry.getValue());
        }
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (email == null ? 0 : email.hashCode());
        return result;
    }

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

    /**
     * Returns the name best suited for displaying (depending on which values are available).
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayName() {
        if (StringUtils.isNotBlank(nickName)) {
            return nickName;
        }
        // Accessing beans from a different thread will throw an unhandled exception that will result in a white screen when logging in
        try {
            if (BeanUtils.getUserBean() != null && BeanUtils.getUserBean().isAdmin()) {
                return email;
            }
        } catch (NullPointerException e) {
            logger.warn(e.getMessage());
        }

        return NetTools.scrambleEmailAddress(email);
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
     * getUserGroupMemberships.
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
     * isGroupMember.
     *
     * @param group the user group to check membership in
     * @return true if this user is a member of the given group, false otherwise
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isGroupMember(UserGroup group) throws DAOException {
        for (UserRole membership : group.getMemberships()) {
            if (this.equals(membership.getUser()) && membership.getUserGroup().equals(group)) {
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
    public List<UserGroup> getAllUserGroups() {
        try {
            List<UserGroup> ret = getUserGroupsWithMembership();
            ret.addAll(getUserGroupOwnerships());
            return ret;
        } catch (DAOException e) {
            logger.error("Error getting user groups for user {}", this.id, e);
            return Collections.emptyList();
        }
    }

    /**
     * Checks whether the user can satisfy at least one of the given access conditions with a license that contains the given privilege name. If one
     * of the conditions is OPENACCESS, true is always returned. Superusers always get access.
     *
     * @param requiredAccessConditions set of access condition names to satisfy
     * @param privilegeName the privilege to check against each condition
     * @param pi persistent identifier of the record being accessed
     * @should return true if user is superuser
     * @should return true if condition is open access
     * @should return true if user has license
     * @should return false if user has no license
     * @should return true if condition list empty
     * @return {@link AccessPermission}
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public AccessPermission canSatisfyAllAccessConditions(Set<String> requiredAccessConditions, String privilegeName, String pi)
            throws PresentationException, IndexUnreachableException, DAOException {
        // logger.trace("canSatisfyAllAccessConditions({},{},{})", requiredAccessConditions, privilegeName, pi); //NOSONAR Debug
        if (isSuperuser()) {
            // logger.trace("User '{}' is superuser, access granted.", getDisplayName()); //NOSONAR Debug
            return AccessPermission.granted();
        }
        if (requiredAccessConditions.isEmpty()) {
            return AccessPermission.granted();
        }
        // always allow access if the only condition is open access and there is no special license configured for it
        if (requiredAccessConditions.size() == 1 && requiredAccessConditions.contains(SolrConstants.OPEN_ACCESS_VALUE)
                    && DataManager.getInstance().getDao().getLicenseType(SolrConstants.OPEN_ACCESS_VALUE) == null) {
                return AccessPermission.granted();
            }

        Map<String, AccessPermission> permissionMap = HashMap.newHashMap(requiredAccessConditions.size());
        for (String accessCondition : requiredAccessConditions) {
            // Check individual licenses
            AccessPermission access = hasLicense(accessCondition, privilegeName, pi);
            if (access.isGranted()) {
                permissionMap.put(accessCondition, access);
                continue;
            }
            // Check group ownership licenses
            for (UserGroup group : getUserGroupOwnerships()) {
                access = group.hasLicense(accessCondition, privilegeName, pi);
                if (access.isGranted()) {
                    permissionMap.put(accessCondition, access);
                    break;
                }
            }
            // Check group membership licenses
            for (UserGroup group : getUserGroupsWithMembership()) {
                access = group.hasLicense(accessCondition, privilegeName, pi);
                if (access.isGranted()) {
                    permissionMap.put(accessCondition, access);
                    break;
                }
            }

        }

        return getAccessPermissionFromMap(permissionMap);
    }

    /**
     * isHasCmsPrivilege.
     *
     * @param privilege the CMS privilege name to check
     * @return boolean
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasCmsPrivilege(String privilege) throws PresentationException, IndexUnreachableException, DAOException {
        return isHasPrivilege(LicenseType.LICENSE_TYPE_CMS, privilege);
    }

    /**
     * isHasPrivilege.
     *
     * @param licenseType the license type name to check
     * @param privilege the privilege name to check
     * @return boolean
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasPrivilege(String licenseType, String privilege)
            throws PresentationException, IndexUnreachableException, DAOException {
        return canSatisfyAllAccessConditions(Collections.singletonMap(licenseType, null).keySet(), privilege, null).isGranted();
    }

    /**
     * Checks whether this user has the permission to set the representative image for the currently open record. TODO For some reason this method is
     * called 8x in a row.
     *
     * @return boolean
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isMaySetRepresentativeImage() throws IndexUnreachableException, PresentationException, DAOException {
        // logger.trace("isMaySetRepresentativeImage"); //NOSONAR Debug
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE, IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE,
                recordsForWhichUserMaySetRepresentativeImage);
    }

    /**
     * Checks whether this user has the permission to delete all ocr-content of one page in crowdsourcing.
     *
     * @return true if this user is allowed to delete OCR page content for the current record, false otherwise
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
     * @param licenseType the license type name to check
     * @param privilegeName the access privilege name to check
     * @param alreadyCheckedPiMap cache of previously checked PIs and their access results
     * @return boolean
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private boolean isHasPrivilegeForCurrentRecord(String licenseType, String privilegeName,
            Map<String, AccessPermission> alreadyCheckedPiMap)
            throws IndexUnreachableException, PresentationException, DAOException {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null && adb.getViewManager() != null) {
            String pi = adb.getViewManager().getPi();
            return isHasPrivilegeForRecord(pi, licenseType, privilegeName, alreadyCheckedPiMap).isGranted();
        }

        return false;
    }

    /**
     *
     * @param pi the persistent identifier of the record to check
     * @param licenseType the license type name to check
     * @param privilegeName the access privilege name to check
     * @param alreadyCheckedPiMap cache of previously checked PIs and their access results
     * @return {@link AccessPermission}
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    private AccessPermission isHasPrivilegeForRecord(String pi, String licenseType, String privilegeName,
            final Map<String, AccessPermission> alreadyCheckedPiMap)
            throws PresentationException, IndexUnreachableException, DAOException {
        if (alreadyCheckedPiMap == null) {
            throw new IllegalArgumentException("alreadyCheckedPiMap may not be null");
        }
        if (alreadyCheckedPiMap.containsKey(pi)) {
            return alreadyCheckedPiMap.get(pi);
        }
        AccessPermission access = canSatisfyAllAccessConditions(new HashSet<>(Collections.singletonList(licenseType)), privilegeName, pi);
        if (access.isGranted()) {
            alreadyCheckedPiMap.put(pi, access);
            return access;
        }

        return AccessPermission.denied();
    }

    /**
     * get the url for the avatar. If useGravatar is active, return
     * {@link io.goobi.viewer.model.security.user.icon.GravatarUserAvatar#getGravatarUrl(int size)}. Otherwise build a resource url to
     * 'resources/images/backend/thumbnail_goobi_person.svg' from the request or the JSF-Context if no request is provided
     *
     * @param size the desired avatar image size in pixels
     * @param request the HTTP request used to build resource URLs; may be null
     * @return Avatar URL
     */
    public String getAvatarUrl(int size, HttpServletRequest request) {
        return getAvatarType().getAvatar(this).getIconUrl(size, request);
    }

    /**
     * Used by the crowdsourcing module.
     *
     * @return {@link #getAvatarUrl(int size, HttpServletRequest request)} with size={@link #AVATAR_DEFAULT_SIZE} and request=null
     */
    public String getAvatarUrl() {
        return getAvatarUrl(AVATAR_DEFAULT_SIZE, null);
    }

    /**
     *
     * @param request the HTTP request used to build resource URLs
     * @return {@link #getAvatarUrl(int size, HttpServletRequest request)} with size={@link #AVATAR_DEFAULT_SIZE}
     */
    public String getAvatarUrl(HttpServletRequest request) {
        return getAvatarUrl(AVATAR_DEFAULT_SIZE, request);
    }

    /**
     * Used by the crowdsourcing module.
     *
     * @param size the desired avatar image size in pixels
     * @return {@link #getAvatarUrl(int size, HttpServletRequest request)} with request=null
     */
    public String getAvatarUrl(int size) {
        return getAvatarUrl(size, null);
    }

    /**
     * Generates salt and a password hash for the given password string.
     *
     * @param password the plain-text password to hash and store
     * @return true if the password was set successfully (i.e. the given password is not blank), false otherwise
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
     * @param email the email address to look up
     * @param password the plain-text password to verify
     * @return The user, if successful.
     * @throws io.goobi.viewer.exceptions.AuthenticationException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public User auth(String email, String password) throws AuthenticationException, DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only allow non-openID accounts
        if (user != null && user.getPasswordHash() != null && bcrypt.checkpw(password, user.getPasswordHash())) {
            user.setLastLogin(LocalDateTime.now());
            return user;
        }

        throw new AuthenticationException("User not found or passwort incorrect");
    }

    /**
     * hasPriviledgeForAllTemplates.
     *
     * @return true if this user has access to all CMS page templates (as superuser or via an unrestricted CMS admin license), false otherwise
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

        List<License> allLicenses = new ArrayList<>(getLicenses());
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllTemplates());
    }

    /**
     * hasPrivilegesForTemplate.
     *
     * @param template the CMS page template to check privileges for
     * @return true exactly if the user is not restricted to certain cmsTemplates or if the given templateId is among the allowed templates for the
     *         user of a usergroup she is in
     */
    public boolean hasPrivilegesForTemplate(CMSPageTemplate template) {
        //if there is no template, assume you have all privileges
        if (template == null) {
            return true;
        }
        // Abort if user not a CMS admin
        if (!isCmsAdmin()) {
            return false;
        }
        // Full admins get all values
        if (hasPriviledgeForAllTemplates()) {
            return true;
        }

        // Check user licenses
        for (License license : getLicenses()) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            if (license.getAllowedCmsTemplates().contains(template)) {
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
                    if (license.getAllowedCmsTemplates().contains(template)) {
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
     * getAllowedTemplates.
     *
     * @param allTemplates full list of available CMS page templates
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

        Set<CMSPageTemplate> allowedTemplates = HashSet.newHashSet(allTemplates.size());
        // Check user licenses
        for (License license : getLicenses()) {
            if (!LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName())) {
                continue;
            }
            // If no restriction is set, return all values
            if (license.isPrivCmsAllTemplates()) {
                return allTemplates;
            }
            if (!license.getAllowedCmsTemplates().isEmpty()) {
                allowedTemplates.addAll(license.getAllowedCmsTemplates());
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
                        allowedTemplates.addAll(license.getAllowedCmsTemplates());
                    }
                }
            }
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }
        // allowedTemplateIds.add("template_general_generic");
        if (allowedTemplates.isEmpty()) {
            return Collections.emptyList();
        }

        List<CMSPageTemplate> ret = new ArrayList<>(allTemplates.size());
        for (CMSPageTemplate template : allTemplates) {
            if (allowedTemplates.contains(template)) {
                ret.add(template);
            }
        }

        logger.trace("getAllowedTemplates END");
        return ret;
    }

    /**
     * hasPrivilegeForAllCategories.
     *
     * @return true if this user has access to all CMS categories (as superuser or via an unrestricted CMS admin license), false otherwise
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

        List<License> allLicenses = new ArrayList<>(getLicenses());
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllCategories());
    }

    /**
     * getAllowedCategories.
     *
     * @param allCategories full list of available CMS categories
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
        for (License license : getLicenses()) {
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
     * hasPrivilegeForAllSubthemeDiscriminatorValues.
     *
     * @return true if this user has access to all CMS subtheme discriminator values (as superuser or via an unrestricted CMS admin license), false otherwise
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

        List<License> allLicenses = new ArrayList<>(getLicenses());
        try {
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
        } catch (DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return allLicenses.stream()
                .anyMatch(license -> LicenseType.LICENSE_TYPE_CMS.equals(license.getLicenseType().getName()) && license.isPrivCmsAllSubthemes());
    }

    /**
     * getAllowedSubthemeDiscriminatorValues.
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
        for (License license : getLicenses()) {
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
                    if (license.isPrivCmsAllTemplates() || !license.getAllowedCmsTemplates().isEmpty()) {
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
     * Getter for the field <code>id</code>.
     *
     * @return the database identifier of this user
     */
    public Long getId() {
        return id;
    }

    /**
     * Setter for the field <code>id</code>.
     *
     * @param id the database identifier to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Getter for the field <code>passwordHash</code>.
     *
     * @return the hashed password of this user
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Setter for the field <code>passwordHash</code>.
     *
     * @param passwordHash the hashed password to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * Getter for the field <code>activationKey</code>.
     *
     * @return the account activation key sent by email
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * Setter for the field <code>activationKey</code>.
     *
     * @param activationKey the account activation key sent by email to set
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * Getter for the field <code>lastLogin</code>.
     *
     * @return the timestamp of the most recent login
     */
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * Setter for the field <code>lastLogin</code>.
     *
     * @param lastLogin the timestamp of the most recent login to set
     */
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * isActive.
     *
     * @return true if the user account is active; false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Setter for the field <code>active</code>.
     *
     * @param active true if the user account is active; false otherwise
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * isSuspended.
     *
     * @return true if the user account is suspended; false otherwise
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * Setter for the field <code>suspended</code>.
     *
     * @param suspended true if the user account is suspended; false otherwise
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * Getter for the field <code>nickName</code>.
     *
     * @return the display nickname of this user
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * Setter for the field <code>nickName</code>.
     *
     * @param nickName the display nickname of the user to set
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * Getter for the field <code>lastName</code>.
     *
     * @return the last name of this user
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * Setter for the field <code>lastName</code>.
     *
     * @param lastName the last name of the user to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * Getter for the field <code>firstName</code>.
     *
     * @return the first name of this user
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * Setter for the field <code>firstName</code>.
     *
     * @param firstName the first name of the user to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * Getter for the field <code>openIdAccounts</code>.
     *
     * @return the list of OpenID account identifiers linked to this user
     */
    public List<String> getOpenIdAccounts() {
        return openIdAccounts;
    }

    /**
     * Setter for the field <code>openIdAccounts</code>.
     *
     * @param openIdAccounts the list of OpenID account identifiers linked to this user to set
     */
    public void setOpenIdAccounts(List<String> openIdAccounts) {
        this.openIdAccounts = openIdAccounts;
    }

    /**
     * Setter for the field <code>email</code>.
     *
     * @param email the email address of the user to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * Getter for the field <code>email</code>.
     *
     * @return the email address of this user
     */
    public String getEmail() {
        return email;
    }

    /**
     * Getter for the field <code>comments</code>.
     *
     * @return administrative comments about this user account
     */
    public String getComments() {
        return comments;
    }

    /**
     * Setter for the field <code>comments</code>.
     *
     * @param comments administrative comments about the user account to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * Getter for the field <code>score</code>.
     *
     * @return the contribution score of this user
     */
    public long getScore() {
        return score;
    }

    /**
     * Setter for the field <code>score</code>.
     *
     * @param score the user's contribution score to set
     */
    public void setScore(long score) {
        this.score = score;
    }

    /**
     *
     * @return true if user email address equals the configured anonymous user address; false otherwise
     */
    public boolean isAnonymous() {
        return EMAIL_ADDRESS_ANONYMOUS.equals(email);
    }

    /**
     * raiseScore.
     *
     * @param amount points to add to the user score
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public void raiseScore(int amount) throws DAOException {
        score += amount;
        DataManager.getInstance().getDao().updateUser(this);
    }

    /**
     * getRank.
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

    
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    
    public void setUserProperties(Map<String, String> userProperties) {
        this.userProperties = userProperties;
    }

    /**
     * isSuperuser.
     *
     * @return true if this user has superuser privileges; false otherwise
     */
    public boolean isSuperuser() {
        return superuser;
    }

    /**
     * isCmsAdmin.
     *
     * @return true if user is superuser or has CMS-specific privileges
     */
    public boolean isCmsAdmin() {
        try {
            return isSuperuser() || isHasCmsPrivilege(IPrivilegeHolder.PRIV_CMS_PAGES);
        } catch (PresentationException e) {
            logger.error(e.getMessage());
        } catch (IndexUnreachableException | DAOException e) {
            logger.error(e.getMessage(), e);
        }

        return false;
    }

    /**
     * Setter for the field <code>superuser</code>.
     *
     * @param superuser true if the user should have superuser privileges; false otherwise
     */
    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    /**
     * isOpenIdUser.
     *
     * @return true if this user has at least one linked OpenID account, false otherwise
     */
    public boolean isOpenIdUser() {
        return openIdAccounts != null && !openIdAccounts.isEmpty();
    }

    /**
     * Getter for the field <code>copy</code>.
     *
     * @return the unsaved copy of this user instance
     */
    public User getCopy() {
        return copy;
    }

    /**
     * Setter for the field <code>copy</code>.
     *
     * @param copy the user instance representing an unsaved copy of this user to set
     */
    public void setCopy(User copy) {
        this.copy = copy;
    }

    /**
     * Getter for the field <code>transkribusSession</code>.
     *
     * @return the active Transkribus session for this user
     */
    public TranskribusSession getTranskribusSession() {
        return transkribusSession;
    }

    /**
     * Setter for the field <code>transkribusSession</code>.
     *
     * @param transkribusSession the active Transkribus session for this user to set
     */
    public void setTranskribusSession(TranskribusSession transkribusSession) {
        this.transkribusSession = transkribusSession;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Required by the ILicensee interface.
     */
    @Override
    public String getName() {
        if (StringUtils.isNotEmpty(getLastName())) {
            return getLastName() + ", " + getFirstName();
        }

        return getDisplayName();
    }

    @Override
    public AccessType getAccessType() {
        return AccessType.USER;
    }

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

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
        // Remove this user from the global set of logged in users
        Set<User> logins = (Set<User>) event.getSession().getServletContext().getAttribute(ATTRIBUTE_LOGINS);
        if (logins != null) {
            logins.remove(this);
            logger.trace("User removed from context: {}", getId());
        }
    }

    /**
     * backupFields.
     */
    public void backupFields() {
        //keep avatar update date of copy because a change in the avatar file is recorded in the copy
        if (copy != null) {
            this.setLocalAvatarUpdated(copy.getLocalAvatarUpdated());
        }
        copy = new User(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.email;
    }

    /**
     * setBCrypt.
     *
     * @param bcrypt the BCrypt instance to use for password hashing
     */
    protected void setBCrypt(BCrypt bcrypt) {
        this.bcrypt = bcrypt;
    }

    /**
     * Gets the {@link io.goobi.viewer.model.security.user.User#id} of a user from a URI.
     *
     * @param idAsURI URI containing the user ID in its path
     * @return a {@link java.lang.Long} object.
     * @should extract id correctly
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
     * getIdAsURI.
     *
     * @return a {@link java.net.URI} object.
     */
    public URI getIdAsURI() {
        return URI.create(URI_ID_TEMPLATE.replace("{id}", this.getId().toString()));
    }

    
    public void setAgreedToTermsOfUse(boolean agreedToTermsOfUse) {
        this.agreedToTermsOfUse = agreedToTermsOfUse;
    }

    
    public boolean isAgreedToTermsOfUse() {
        return agreedToTermsOfUse;
    }

    
    public UserAvatarOption getAvatarType() {
        return Optional.ofNullable(avatarType).orElse(UserAvatarOption.DEFAULT);
    }

    
    public void setAvatarType(UserAvatarOption avatarType) {
        this.avatarType = avatarType;
    }

    public boolean hasLocalAvatarImage() {
        try {
            return UserAvatarResource.getUserAvatarFile(getId()).isPresent();
        } catch (IOException e) {
            logger.error("Error reading local avatar file: {}", e.toString());
            return false;
        }
    }

    public void setAvatarFile(Part uploadedFile) throws IOException {
        String fileName = Paths.get(uploadedFile.getSubmittedFileName()).getFileName().toString(); // MSIE fix.
        Path destFile = UserAvatarResource.getAvatarFilePath(fileName, getId());
        deleteAvatarFile();
        UserAvatarResource.removeFromImageCache(destFile, ContentServerCacheManager.getInstance());
        try (InputStream initialStream = uploadedFile.getInputStream()) {
            if (!Files.isDirectory(destFile.getParent())) {
                Files.createDirectories(destFile.getParent());
            }
            java.nio.file.Files.copy(
                    uploadedFile.getInputStream(),
                    destFile,
                    StandardCopyOption.REPLACE_EXISTING);
            if (!Files.exists(destFile)) {
                throw new IOException("Uploaded file does not exist");
            } else if (!isValidImageFile(destFile)) {
                throw new IOException("Uploaded file is not a valid image file");
            } else {
                this.localAvatarUpdated = System.currentTimeMillis();
            }
        } catch (IOException e) {
            logger.error("Error uploaded avatar file: {}", e.toString());
            deleteAvatarFile();
            throw e;
        }
    }

    private static boolean isValidImageFile(Path file) throws IOException {
        String contentType1 = FileTools.probeContentType(PathConverter.toURI(file));
        // String contentType2 = FileTools.getMimeTypeFromFile(file);
        return contentType1.startsWith("image/");
    }

    public void deleteAvatarFile() throws IOException {
        UserAvatarResource.getUserAvatarFile(getId()).ifPresent(file -> {
            try {
                Files.delete(file);
            } catch (IOException e) {
                logger.error("Error deleting avatar file: {}", e.toString());

            }
        });
    }

    
    public Long getLocalAvatarUpdated() {
        return localAvatarUpdated;
    }

    
    public void setLocalAvatarUpdated(Long localAvatarUpdated) {
        this.localAvatarUpdated = localAvatarUpdated;
    }

    public String getBackendDisplayName() {
        if (StringUtils.isAllBlank(this.nickName, this.firstName, this.lastName)) {
            return this.email;
        }
        String name = "";
        if (StringUtils.isNotBlank(nickName)) {
            name += (nickName + " - ");
        }
        if (StringUtils.isNotBlank(firstName)) {
            name += (firstName + " ");
        }
        if (StringUtils.isNotBlank(lastName)) {
            name += (lastName + " ");
        }
        name += "(" + email + ")";
        return name;
    }

    @Override
    public int compareTo(User other) {
        if (StringUtils.isNoneBlank(this.email, other.email)) {
            return this.email.compareToIgnoreCase(other.email);
        } else if (StringUtils.isAllBlank(this.email, other.email)) {
            return 0;
        } else {
            return StringUtils.isBlank(this.email) ? -1 : 1;
        }
    }

    /**
     * main.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("halbgeviertstrich", BCrypt.gensalt()));
    }
}
