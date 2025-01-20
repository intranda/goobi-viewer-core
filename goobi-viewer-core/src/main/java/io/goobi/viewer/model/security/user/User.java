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
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.user.icon.UserAvatarOption;
import io.goobi.viewer.model.transkribus.TranskribusSession;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;
import jakarta.servlet.http.Part;

/**
 * <p>
 * User class.
 * </p>
 */
@Entity
@Table(name = "users")
public class User extends AbstractLicensee implements HttpSessionBindingListener, Serializable, Comparable<User> {

    private static final long serialVersionUID = 549769987121664488L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(User.class);

    /** Constant <code>ATTRIBUTE_LOGINS="logins"</code> */
    public static final String ATTRIBUTE_LOGINS = "logins";

    /** Constant <code>AVATAR_DEFAULT_SIZE=140</code> */
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

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE })
    @PrivateOwned
    private List<License> licenses = new ArrayList<>();

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
     * @param nickname
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
        // TODO clone licenses?
        for (License license : blueprint.getLicenses()) {
            getLicenses().add(license);
        }
        // Clone OpenID identifiers
        for (String openIdAccount : blueprint.getOpenIdAccounts()) {
            getOpenIdAccounts().add(openIdAccount);
        }
        // Clone properties
        for (Entry<String, String> entry : blueprint.getUserProperties().entrySet()) {
            userProperties.put(entry.getKey(), entry.getValue());
        }
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
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }

        return NetTools.scrambleEmailAddress(email);
    }

    /**
     * 
     * @return HTML-escapted value of <code>getDisplayName()</code>
     */
    @Deprecated(since = "2023.11")
    public String getDisplayNameEscaped() {
        return StringEscapeUtils.escapeHtml4(getDisplayName());
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
     * @param requiredAccessConditions a {@link java.util.Set} object.
     * @param privilegeName a {@link java.lang.String} object.
     * @param pi a {@link java.lang.String} object.
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
        // logger.trace("canSatisfyAllAccessConditions({},{},{})", conditionList, privilegeName, pi); //NOSONAR Debug
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

        Map<String, AccessPermission> permissionMap = new HashMap<>(requiredAccessConditions.size());
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
     * @return boolean
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public boolean isHasCmsPrivilege(String privilege) throws PresentationException, IndexUnreachableException, DAOException {
        return isHasPrivilege(LicenseType.LICENSE_TYPE_CMS, privilege);
    }

    /**
     * <p>
     * isHasPrivilege.
     * </p>
     *
     * @param licenseType a {@link java.lang.String} object.
     * @param privilege a {@link java.lang.String} object.
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
     * @param alreadyCheckedPiMap
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
     * @param pi
     * @param licenseType
     * @param privilegeName
     * @param alreadyCheckedPiMap
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
     * @param size
     * @param request
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
     * @param request
     * @return {@link #getAvatarUrl(int size, HttpServletRequest request)} with size={@link #AVATAR_DEFAULT_SIZE}
     */
    public String getAvatarUrl(HttpServletRequest request) {
        return getAvatarUrl(AVATAR_DEFAULT_SIZE, request);
    }

    /**
     * Used by the crowdsourcing module.
     *
     * @param size a int.
     * @return {@link #getAvatarUrl(int size, HttpServletRequest request)} with request=null
     */
    public String getAvatarUrl(int size) {
        return getAvatarUrl(size, null);
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
            user.setLastLogin(LocalDateTime.now());
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
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
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
     * @param template
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
        for (License license : licenses) {
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

        Set<CMSPageTemplate> allowedTemplates = new HashSet<>(allTemplates.size());
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
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
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
            allLicenses.addAll(getUserGroupsWithMembership().stream().flatMap(g -> g.getLicenses().stream()).toList());
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
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    /**
     * <p>
     * Setter for the field <code>lastLogin</code>.
     * </p>
     *
     * @param lastLogin the lastLogin to set
     */
    public void setLastLogin(LocalDateTime lastLogin) {
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
     *
     * @return true if user email address equals the configured anonymous user address; false otherwise
     */
    public boolean isAnonymous() {
        return EMAIL_ADDRESS_ANONYMOUS.equals(email);
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
     * @return the userProperties
     */
    public Map<String, String> getUserProperties() {
        return userProperties;
    }

    /**
     * @param userProperties the userProperties to set
     */
    public void setUserProperties(Map<String, String> userProperties) {
        this.userProperties = userProperties;
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
        } catch (IndexUnreachableException | DAOException e) {
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
     * jakarta.servlet.http.HttpSessionBindingListener#valueBound(jakarta.servlet.http.
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
     * jakarta.servlet.http.HttpSessionBindingListener#valueUnbound(jakarta.servlet.http
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
            logger.trace("User removed from context: {}", getId());
        }
    }

    /**
     * <p>
     * backupFields.
     * </p>
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

    /**
     * @return the avatarType
     */
    public UserAvatarOption getAvatarType() {
        return Optional.ofNullable(avatarType).orElse(UserAvatarOption.DEFAULT);
    }

    /**
     * @param avatarType the avatarType to set
     */
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

    /**
     * @return the localAvatarUpdated
     */
    public Long getLocalAvatarUpdated() {
        return localAvatarUpdated;
    }

    /**
     * @param localAvatarUpdated the localAvatarUpdated to set
     */
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
     * <p>
     * main.
     * </p>
     *
     * @param args an array of {@link java.lang.String} objects.
     */
    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("halbgeviertstrich", BCrypt.gensalt()));
    }
}
