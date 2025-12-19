package io.goobi.viewer.model.security;

import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.security.License.AccessType;
import io.goobi.viewer.model.security.clients.ClientApplication;
import io.goobi.viewer.model.security.user.AbstractLicensee;
import io.goobi.viewer.model.security.user.IpRange;
import io.goobi.viewer.model.security.user.User;
import io.goobi.viewer.model.security.user.UserGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Licensee wrapper.
 */
@Entity
@Table(name = "license_rights_holders")
public class LicenseRightsHolder {

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(LicenseRightsHolder.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "license_rights_holder_id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "license_id", nullable = false)
    private License owner;

    @Column(name = "licensee_type")
    @Enumerated(EnumType.STRING)
    private AccessType type;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "user_group_id")
    private UserGroup userGroup;

    @ManyToOne
    @JoinColumn(name = "ip_range_id")
    private IpRange ipRange;

    @ManyToOne
    @JoinColumn(name = "client_id")
    private ClientApplication client;

    public LicenseRightsHolder() {

    }

    public LicenseRightsHolder(License owner) {
        this.owner = owner;
    }

    public boolean isDisabled() {
        return getType() == null || (getUser() == null && getUserGroup() == null && getIpRange() == null && getClient() == null);
    }

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
     * @return the type
     */
    public AccessType getType() {
        return type;
    }

    /**
     * @return the owner
     */
    public License getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(License owner) {
        this.owner = owner;
    }

    /**
     * @param type the type to set
     */
    public void setType(AccessType type) {
        logger.trace("setType: {}", type);
        this.type = type;
    }

    /**
     * @return the user
     */
    public User getUser() {
        return user;
    }

    /**
     * @param user the user to set
     * @should set userGroup and ipRange to null if user not null
     * @should not set userGroup and ipRange to null if user null
     */
    public void setUser(User user) {
        this.user = user;
        if (user != null) {
            this.userGroup = null;
            this.ipRange = null;
            this.client = null;
        }
    }

    /**
     * @return the userGroup
     */
    public UserGroup getUserGroup() {
        return userGroup;
    }

    /**
     * @param userGroup the userGroup to set
     * @should set user and ipRange to null if userGroup not null
     * @should not set user and ipRange to null if userGroup null
     */
    public void setUserGroup(UserGroup userGroup) {
        this.userGroup = userGroup;
        if (userGroup != null) {
            this.user = null;
            this.ipRange = null;
            this.client = null;
        }
    }

    /**
     * @return the ipRange
     */
    public IpRange getIpRange() {
        return ipRange;
    }

    /**
     * @param ipRange the ipRange to set
     * @should set user and userGroup to null if ipRange not null
     * @should not set user and userGroup to null if ipRange null
     */
    public void setIpRange(IpRange ipRange) {
        this.ipRange = ipRange;
        if (ipRange != null) {
            this.user = null;
            this.userGroup = null;
            this.client = null;
        }
    }

    /**
     * @return the client
     */
    public ClientApplication getClient() {
        return client;
    }

    /**
     * @param client the client to set
     */
    public void setClient(ClientApplication client) {
        this.client = client;
    }

    public Long getClientId() {
        return Optional.ofNullable(client).map(ClientApplication::getId).orElse(null);
    }

    /**
     * @param clientId the clientId to set
     * @throws DAOException
     */
    public void setClientId(Long clientId) throws DAOException {
        if (clientId != null) {
            this.client = DataManager.getInstance().getDao().getClientApplication(clientId);
        }
    }
}
