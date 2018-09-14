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
package de.intranda.digiverso.presentation.model.security.user;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

import org.apache.commons.lang.StringUtils;
import org.eclipse.persistence.annotations.Index;
import org.eclipse.persistence.annotations.PrivateOwned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

import de.intranda.digiverso.presentation.controller.BCrypt;
import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.exceptions.AuthenticationException;
import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.exceptions.PresentationException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.security.ILicensee;
import de.intranda.digiverso.presentation.model.security.IPrivilegeHolder;
import de.intranda.digiverso.presentation.model.security.License;
import de.intranda.digiverso.presentation.model.security.LicenseType;
import de.intranda.digiverso.presentation.model.security.Role;
import de.intranda.digiverso.presentation.model.transkribus.TranskribusSession;
import jgravatar.Gravatar;
import jgravatar.GravatarDefaultImage;
import jgravatar.GravatarRating;

@Entity
@Table(name = "users")
@XStreamAlias("user")
public class User implements ILicensee, HttpSessionBindingListener {

    /** Logger for this class. */
    private static final Logger logger = LoggerFactory.getLogger(User.class);

    public static final String ATTRIBUTE_LOGINS = "logins";

    public static final int AVATAR_DEFAULT_SIZE = 96;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Index(name = "index_users_email")
    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    /** Activation key for the user account. Value must be reset to null when the account is activated. */
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
    @XStreamOmitField
    private Set<String> recordsForWhichUserMaySetRepresentativeImage;

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    @XStreamOmitField
    private Set<String> recordsForWhichUserMayEditOverviewPage;

    /** Save previous checks to avoid expensive Solr queries. */
    @Transient
    @XStreamOmitField
    private Set<String> recordsForWhichUserMayDeleteOcrPage;

    @Transient
    @XStreamOmitField
    private User copy;

    @Transient
    @XStreamOmitField
    private TranskribusSession transkribusSession;

    /** Empty constructor for XStream. */
    public User() {
        // the emptiness inside
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
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
     * @return
     */
    public String getDisplayName() {
        String displayName = "";
        //        if (StringUtils.isNotEmpty(lastName) || StringUtils.isNotEmpty(firstName)) {
        //            if (StringUtils.isNotEmpty(firstName)) {
        //                displayName = firstName;
        //            }
        //            if (StringUtils.isNotEmpty(lastName)) {
        //                displayName += " " + lastName;
        //            }
        //        } else
        if (StringUtils.isNotEmpty(nickName)) {
            displayName = nickName;
        } else {
            displayName = email;
        }

        return displayName;
    }

    /**
     * If the display name is the e-mail address and the logged in user (!= this user) is not an superuser, obfuscate the address.
     *
     * @return
     */
    public String getDisplayNameObfuscated() {
        String displayName = getDisplayName();
        if (displayName.equals(email) && BeanUtils.getUserBean() != null && !BeanUtils.getUserBean().isAdmin()) {
            return new StringBuilder().append(Helper.getTranslation("user_anonymous", null)).append(" (").append(id).append(')').toString();
        }

        return displayName;
    }

    @Override
    public boolean hasLicense(String licenseName, String privilegeName, String pi) throws PresentationException, IndexUnreachableException {
        logger.trace("hasLicense({},{},{})", licenseName, privilegeName, pi);
        if (StringUtils.isEmpty(privilegeName)) {
            return true;
        }
        for (License license : getLicenses()) {
            //            logger.trace("license: {}, {}", license.getId(), license.getPrivileges().toString());
            //            logger.trace("license type: {}", license.getLicenseType().getName());
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
                        if (DataManager.getInstance().getSearchIndex().getFirstDoc(sbQuery.toString(),
                                Collections.singletonList(SolrConstants.IDDOC)) != null) {
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
     * @throws DAOException
     */
    @Deprecated
    public boolean hasRole(String roleName) throws DAOException {
        // return DataManager.getInstance().getConfiguration().getUserHasRole(roleName, getId());
        Role role = DataManager.getInstance().getDao().getRole(roleName);
        return !DataManager.getInstance().getDao().getUserRoles(null, this, role).isEmpty();
    }

    /**
     * Checks whether the user has the given privilege directly.
     *
     * @param privilegeName
     * @return
     * @throws DAOException
     */
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
     * @throws DAOException
     */
    public List<UserGroup> getUserGroupOwnerships() throws DAOException {
        return DataManager.getInstance().getDao().getUserGroups(this);
    }

    /**
     *
     * @return
     * @throws DAOException
     */
    public List<UserRole> getUserGroupMemberships() throws DAOException {
        return DataManager.getInstance().getDao().getUserRoles(null, this, null);
    }

    /**
     * Returns a list of UserGroups of which this user is a member.
     *
     * @throws DAOException
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

    public boolean isGroupMember(UserGroup group) throws DAOException {
        for (UserRole membership : group.getMemberships()) {
            if (membership.getUser().equals(this) && membership.getUserGroup().equals(group)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks whether the user can satisfy at least one of the given access conditions with a license that contains the given privilege name. If one
     * of the conditions is OPENACCESS, true is always returned. Superusers always get access.
     *
     * @param conditionList
     * @param privilegeName
     * @param pi
     * @return
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     * @should return true if user is superuser
     * @should return true if condition is open access
     * @should return true if user has license
     * @should return false if user has no license
     * @should return true if condition list empty
     */
    public boolean canSatisfyAllAccessConditions(Set<String> conditionList, String privilegeName, String pi)
            throws PresentationException, IndexUnreachableException, DAOException {
         logger.trace("canSatisfyAllAccessConditions({},{},{})", conditionList, privilegeName, pi);
        if (isSuperuser()) {
            logger.trace("User '{}' is superuser, access granted.", getDisplayName());
            return true;
        }

        if (conditionList.size() == 1 && conditionList.contains(SolrConstants.OPEN_ACCESS_VALUE)) {
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
        //It should be sufficient if the user can satisfy one required licence
          return permissionMap.isEmpty() ||  permissionMap.containsValue(true);
//        return !permissionMap.containsValue(false);
    }

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

    @Override
    public boolean removeLicense(License license) {
        if (license != null && licenses != null) {
            // license.setUser(null);
            return licenses.remove(license);
        }

        return false;
    }

    /**
     * Checks whether this user has the permission to set the representative image for the currently open record. TODO For some reason this method is
     * called 8x in a row.
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isMaySetRepresentativeImage() throws IndexUnreachableException, PresentationException, DAOException {
        //        logger.trace("isMaySetRepresentativeImage");
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_SET_REPRESENTATIVE_IMAGE, IPrivilegeHolder.PRIV_SET_REPRESENTATIVE_IMAGE,
                recordsForWhichUserMaySetRepresentativeImage);
    }

    /**
     * Checks whether this user has the permission to explicitly create an overview page for the currently open record.
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isMayEditOverviewPage() throws IndexUnreachableException, PresentationException, DAOException {
        //        logger.trace("isMayEditOverviewPage");
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_FORCE_OVERVIEW_PAGE, IPrivilegeHolder.PRIV_EDIT_OVERVIEW_PAGE,
                recordsForWhichUserMayEditOverviewPage);
    }

    /**
     * Checks whether this user has the permission to delete all ocr-content of one page in crowdsourcing.
     *
     * @return
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @throws DAOException
     */
    public boolean isMayDeleteCrowdsourcingFulltext() throws IndexUnreachableException, PresentationException, DAOException {
        return isHasPrivilegeForCurrentRecord(LicenseType.LICENSE_TYPE_DELETE_OCR_PAGE, IPrivilegeHolder.PRIV_DELETE_OCR_PAGE,
                recordsForWhichUserMayDeleteOcrPage);
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

    public String getAvatarUrl() {
        if (useGravatar) {
            return getGravatarUrl(AVATAR_DEFAULT_SIZE);
        }

        return BeanUtils.getNavigationHelper().getApplicationUrl() + "resources/crowdsourcing/img/profile-small.png";
    }

    public String getAvatarUrl(int size) {
        if (useGravatar) {
            return getGravatarUrl(size);
        }

        return BeanUtils.getNavigationHelper().getApplicationUrl() + "resources/crowdsourcing/img/profile-small.png";
    }

    public String getGravatarUrl() {
        return getGravatarUrl(AVATAR_DEFAULT_SIZE);
    }

    /**
     * Emptry setter so that HTML pages do not throw missing property errors.
     *
     * @param gravatarUrl
     */
    public void setGravatarUrl(String gravatarUrl) {
        // nothing
    }

    /**
     * Generates and returns a Gravatar url for the user's e-mail address.
     * 
     * @param size
     * @return Gravatar URL
     */
    public String getGravatarUrl(int size) {
        if (useGravatar && StringUtils.isNotEmpty(email)) {
            Gravatar gravatar = new Gravatar();
            gravatar.setSize(size);
            gravatar.setRating(GravatarRating.GENERAL_AUDIENCES);
            gravatar.setDefaultImage(GravatarDefaultImage.GRAVATAR_ICON);
            String url = gravatar.getUrl(email);
            if (url != null) {
                url = url.replace("http:", "");
            }
            return url;
        }

        return null;
    }

    /**
     * Generates salt and a password hash for the given password string.
     *
     * @param password
     * @return
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
     * @param email
     * @param password
     * @return The user, if successful.
     * @throws AuthenticationException if login data incorrect
     * @throws DAOException
     */
    public static User auth(String email, String password) throws AuthenticationException, DAOException {
        User user = DataManager.getInstance().getDao().getUserByEmail(email);
        // Only allow non-openID accounts
        if (user != null && user.getPasswordHash() != null && BCrypt.checkpw(password, user.getPasswordHash())) {
            user.setLastLogin(new Date());
            return user;
        }

        throw new AuthenticationException("User not found or passwort incorrect");
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the passwordHash
     */
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * @param passwordHash the passwordHash to set
     */
    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    /**
     * @return the activationKey
     */
    public String getActivationKey() {
        return activationKey;
    }

    /**
     * @param activationKey the activationKey to set
     */
    public void setActivationKey(String activationKey) {
        this.activationKey = activationKey;
    }

    /**
     * @return the lastLogin
     */
    public Date getLastLogin() {
        return lastLogin;
    }

    /**
     * @param lastLogin the lastLogin to set
     */
    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the suspended
     */
    public boolean isSuspended() {
        return suspended;
    }

    /**
     * @param suspended the suspended to set
     */
    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }

    /**
     * @return the nickName
     */
    public String getNickName() {
        return nickName;
    }

    /**
     * @param nickName the nickName to set
     */
    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    /**
     * @return the lastName
     */
    public String getLastName() {
        return lastName;
    }

    /**
     * @param lastName the lastName to set
     */
    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    /**
     * @return the firstName
     */
    public String getFirstName() {
        return firstName;
    }

    /**
     * @param firstName the firstName to set
     */
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    /**
     * @return the openIdAccounts
     */
    public List<String> getOpenIdAccounts() {
        return openIdAccounts;
    }

    /**
     * @param openIdAccounts the openIdAccounts to set
     */
    public void setOpenIdAccounts(List<String> openIdAccounts) {
        this.openIdAccounts = openIdAccounts;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the comments
     */
    public String getComments() {
        return comments;
    }

    /**
     * @param comments the comments to set
     */
    public void setComments(String comments) {
        this.comments = comments;
    }

    /**
     * @return the score
     */
    public long getScore() {
        return score;
    }

    /**
     * @param score the score to set
     */
    public void setScore(long score) {
        this.score = score;
    }

    /**
     * @return the useGravatar
     */
    public boolean isUseGravatar() {
        return useGravatar;
    }

    /**
     * @param useGravatar the useGravatar to set
     */
    public void setUseGravatar(boolean useGravatar) {
        this.useGravatar = useGravatar;
    }

    public void raiseScore(int amount) throws DAOException {
        score += amount;
        DataManager.getInstance().getDao().updateUser(this);
    }

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

    /**
     * @return the licenses
     */
    @Override
    public List<License> getLicenses() {
        return licenses;
    }

    /**
     * @param licenses the licenses to set
     */
    public void setLicenses(List<License> licenses) {
        this.licenses = licenses;
    }

    /**
     * @return the superuser
     */
    public boolean isSuperuser() {
        return superuser;
    }

    /**
     * @param superuser the superuser to set
     */
    public void setSuperuser(boolean superuser) {
        this.superuser = superuser;
    }

    public boolean isOpenIdUser() {
        return openIdAccounts != null && !openIdAccounts.isEmpty();
    }

    /**
     * @return the copy
     */
    public User getCopy() {
        return copy;
    }

    /**
     * @param copy the copy to set
     */
    public void setCopy(User copy) {
        this.copy = copy;
    }

    /**
     * @return the transkribusSession
     */
    public TranskribusSession getTranskribusSession() {
        return transkribusSession;
    }

    /**
     * @param transkribusSession the transkribusSession to set
     */
    public void setTranskribusSession(TranskribusSession transkribusSession) {
        this.transkribusSession = transkribusSession;
    }

    /**
     * Required by the ILicensee interface.
     */
    @Override
    public String getName() {
        if (StringUtils.isNotEmpty(getLastName())) {
            return getLastName() + ", " + getFirstName();
        }

        return getDisplayName();
    }

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSessionBindingListener#valueBound(javax.servlet.http.HttpSessionBindingEvent)
     */
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

    /* (non-Javadoc)
     * @see javax.servlet.http.HttpSessionBindingListener#valueUnbound(javax.servlet.http.HttpSessionBindingEvent)
     */
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

    public void backupFields() {
        copy = clone();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    public static void main(String[] args) {
        System.out.println(BCrypt.hashpw("halbgeviertstrich", BCrypt.gensalt()));
    }
}
