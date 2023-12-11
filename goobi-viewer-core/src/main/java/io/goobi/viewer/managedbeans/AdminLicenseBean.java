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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.inject.Named;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.NetTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.messages.ViewerResourceBundle;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.pages.CMSPageTemplate;
import io.goobi.viewer.model.search.SearchHelper;
import io.goobi.viewer.model.security.DownloadTicket;
import io.goobi.viewer.model.security.License;
import io.goobi.viewer.model.security.LicenseType;
import io.goobi.viewer.model.security.Role;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;
import jakarta.mail.MessagingException;

/**
 * Administration backend functions.
 */
@Named
@SessionScoped
public class AdminLicenseBean implements Serializable {

    private static final long serialVersionUID = 4036951960661161323L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(AdminLicenseBean.class);

    private static final String MSG_ADMIN_LICENSE_SAVE_FAILURE = "license_licenseSaveFailure";
    private static final String MSG_ADMIN_LICENSE_SAVE_SUCCESS = "license_licenseSaveSuccess";
    private static final String URL_PRETTY_ADMINLICENSES = "pretty:adminLicenses";

    private static final String EXCEPTION_TICKET_MAY_NOT_BE_NULL = "ticket may not be null";

    static final int DEFAULT_ROWS_PER_PAGE = 15;

    private TableDataProvider<DownloadTicket> lazyModelDownloadTickets;

    private Role currentRole = null;
    private LicenseType currentLicenseType = null;
    private License currentLicense = null;

    /**
     * <p>
     * Constructor for AdminBean.
     * </p>
     */
    public AdminLicenseBean() {
        // the emptiness inside
    }

    /**
     * <p>
     * init.
     * </p>
     */
    @PostConstruct
    public void init() {
        lazyModelDownloadTickets = new TableDataProvider<>(new TableDataSource<DownloadTicket>() {

            @Override
            public List<DownloadTicket> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder, Map<String, String> filters) {
                logger.trace("getEntries<DownloadTicket>, {}-{}", first, first + pageSize);
                try {
                    String useSortField = sortField;
                    SortOrder useSortOrder = sortOrder;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "id";
                    }
                    return DataManager.getInstance().getDao().getActiveDownloadTickets(first, pageSize, useSortField, useSortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage());
                }
                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                try {
                    return DataManager.getInstance().getDao().getActiveDownloadTicketCount(filters);
                } catch (DAOException e) {
                    logger.error(e.getMessage(), e);
                    return 0;
                }
            }

            @Override
            public void resetTotalNumberOfRecords() {
                // 
            }
        });
        lazyModelDownloadTickets.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        lazyModelDownloadTickets.getFilter("pi_email_title_requestMessage");
    }

    // LicenseType

    /**
     * Returns all existing license types. Required for admin tabs.
     *
     * @return all license types in the database
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllLicenseTypes() throws DAOException {
        return DataManager.getInstance().getDao().getAllLicenseTypes();
    }

    /**
     *
     * @return Two SelectItemGroups for core and regular license types
     * @throws DAOException
     * @should group license types in select item groups correctly
     */
    public List<SelectItem> getGroupedLicenseTypeSelectItems() throws DAOException {
        List<LicenseType> licenseTypes = getAllLicenseTypes();
        if (licenseTypes.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> list1 = new ArrayList<>();
        List<LicenseType> list2 = new ArrayList<>();
        for (LicenseType licenseType : licenseTypes) {
            if (licenseType.isCore()) {
                list1.add(licenseType);
            } else {
                list2.add(licenseType);
            }
        }
        List<SelectItem> ret = new ArrayList<>(licenseTypes.size());

        SelectItemGroup group1 = new SelectItemGroup(ViewerResourceBundle.getTranslation("admin__license_function", null));
        SelectItem[] array1 = new SelectItem[list1.size()];
        for (int i = 0; i < array1.length; ++i) {
            array1[i] = new SelectItem(list1.get(i), ViewerResourceBundle.getTranslation(list1.get(i).getName(), null));
        }
        group1.setSelectItems(array1);
        ret.add(group1);

        SelectItemGroup group2 = new SelectItemGroup(ViewerResourceBundle.getTranslation("admin__license", null));
        SelectItem[] array2 = new SelectItem[list2.size()];
        for (int i = 0; i < array2.length; ++i) {
            array2[i] = new SelectItem(list2.get(i), ViewerResourceBundle.getTranslation(list2.get(i).getName(), null));
        }
        group2.setSelectItems(array2);
        ret.add(group2);

        return ret;
    }

    /**
     *
     * @param core
     * @return all license types in the database where this.core=core
     * @throws DAOException
     */
    private List<LicenseType> getFilteredLicenseTypes(boolean core) throws DAOException {
        List<LicenseType> all = getAllLicenseTypes();
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> ret = new ArrayList<>(all.size());
        for (LicenseType lt : all) {
            if (lt.isCore() == core) {
                ret.add(lt);
            }
        }

        return ret;
    }

    /**
     * <p>
     * getAllCoreLicenseTypes.
     * </p>
     *
     * @return all license types in the database where core=true
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllCoreLicenseTypes() throws DAOException {
        return getFilteredLicenseTypes(true);
    }

    /**
     * <p>
     * getAllRecordLicenseTypes.
     * </p>
     *
     * @return all license types in the database where core=false
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getAllRecordLicenseTypes() throws DAOException {
        return getFilteredLicenseTypes(false);
    }

    /**
     * Returns all existing non-core license types minus <code>currentLicenseType</code>. Used for overriding license type selection.
     *
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<LicenseType> getOtherLicenseTypes() throws DAOException {
        List<LicenseType> all = DataManager.getInstance().getDao().getAllLicenseTypes();
        if (all.isEmpty() || all.get(0).equals(this.currentLicenseType)) {
            return Collections.emptyList();
        }

        List<LicenseType> ret = new ArrayList<>(all.size() - 1);
        for (LicenseType licenseType : all) {
            if (licenseType.equals(this.currentLicenseType) || licenseType.isCore()) {
                continue;
            }
            ret.add(licenseType);
        }

        return ret;
    }

    /**
     * <p>
     * saveLicenseTypeAction.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String saveCurrentLicenseTypeAction() throws DAOException {
        if (currentLicenseType == null) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return URL_PRETTY_ADMINLICENSES;
        }

        // Adopt changes made to the privileges
        if (!currentLicenseType.getPrivileges().equals(currentLicenseType.getPrivilegesCopy())) {
            logger.trace("Saving changes to privileges");
            currentLicenseType.setPrivileges(new HashSet<>(currentLicenseType.getPrivilegesCopy()));
        }

        if (!currentLicenseType.isRedirect()) {
            currentLicenseType.setRedirectUrl(null);
        }

        if (currentLicenseType.getId() != null) {
            if (DataManager.getInstance().getDao().updateLicenseType(currentLicenseType)) {
                logger.trace("License type '{}' updated successfully", currentLicenseType.getName());
                Messages.info(StringConstants.MSG_ADMIN_UPDATED_SUCCESSFULLY);
            } else {
                Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminLicenseEdit";
            }
        } else {
            if (DataManager.getInstance().getDao().addLicenseType(currentLicenseType)) {
                Messages.info(StringConstants.MSG_ADMIN_ADDED_SUCCESSFULLY);
            } else {
                Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
                return "pretty:adminLicenseNew";
            }
        }

        return URL_PRETTY_ADMINLICENSES;
    }

    /**
     * <p>
     * deleteLicenseTypeAction.
     * </p>
     *
     * @param licenseType a {@link io.goobi.viewer.model.security.LicenseType} object.
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteLicenseTypeAction(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            return "";
        }

        if (DataManager.getInstance().getDao().deleteLicenseType(licenseType)) {
            Messages.info(StringConstants.MSG_ADMIN_DELETED_SUCCESSFULLY);

        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        }

        return licenseType.isCore() ? "pretty:adminRoles" : "pretty:adminLicenseTypes";
    }

    /**
     * <p>
     * newCurrentLicenseTypeAction.
     * </p>
     * 
     * @param name
     */
    public void newCurrentLicenseTypeAction(String name) {
        logger.trace("newCurrentLicenseTypeAction({})", name);
        currentLicenseType = new LicenseType(name);
    }

    /**
     * <p>
     * resetCurrentRoleLicenseAction.
     * </p>
     */
    public void resetCurrentRoleLicenseAction() {
        currentLicenseType = new LicenseType();
        currentLicenseType.setCore(true);
    }

    // License

    public List<License> getAllLicenses() throws DAOException {
        return DataManager.getInstance().getDao().getAllLicenses();
    }

    /**
     *
     * @param licenseType
     * @return true if at least one license uses the given license type; false otherwise
     * @throws DAOException
     */
    public boolean isLicenseTypeInUse(LicenseType licenseType) throws DAOException {
        if (licenseType == null) {
            return false;
        }

        return DataManager.getInstance().getDao().getLicenseCount(licenseType) > 0;
    }

    /**
     *
     * @param licenseTypeName
     * @return Number of records with the given licenseTypeName that have the ACCESSCONDITION_CONCURRENTUSE field
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public long getConcurrentViewsLimitRecordCountForLicenseType(String licenseTypeName) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
                .getSearchIndex()
                .getHitCount("+" + SolrConstants.ACCESSCONDITION + ":\"" + licenseTypeName
                        + "\" +" + SolrConstants.ISWORK + ":true +" + SolrConstants.ACCESSCONDITION_CONCURRENTUSE + ":*");
    }

    /**
     *
     * @param licenseTypeName
     * @return Number of records with the given licenseTypeName that have the ACCESSCONDITION_PDF_PERCENTAGE_QUOTA field
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public long getPdfQuotaRecordCountForLicenseType(String licenseTypeName) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
                .getSearchIndex()
                .getHitCount("+" + SolrConstants.ACCESSCONDITION + ":\"" + licenseTypeName
                        + "\" +" + SolrConstants.ISWORK + ":true +" + SolrConstants.ACCESSCONDITION_PDF_PERCENTAGE_QUOTA + ":*");
    }

    /**
     *
     * @param licenseType
     * @return Number of licenses with of given {@link LicenseType}
     * @throws DAOException
     */
    public List<License> getLicenses(LicenseType licenseType) throws DAOException {
        return DataManager.getInstance().getDao().getLicenses(licenseType);
    }

    /**
     * <p>
     * Creates <code>currentLicense</code> to a new instance.
     * </p>
     */
    public void newCurrentLicenseAction() {
        logger.trace("newCurrentLicenseAction");
        setCurrentLicense(new License());
    }

    /**
     * Adds the current License to the licensee (User, UserGroup or IpRange). It is imperative that the licensee object is refreshed after updating so
     * that a new license object is an ID attached. Otherwise the list of licenses will throw an NPE!
     *
     * @return Navigation outcome
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @throws DAOException
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String saveCurrentLicenseAction() throws DAOException, IndexUnreachableException, PresentationException {
        logger.trace("saveCurrentLicenseAction");
        if (currentLicense == null) {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
            return "";
        }

        // Sync changes made to the privileges
        if (!currentLicense.getPrivileges().equals(currentLicense.getPrivilegesCopy())) {
            logger.trace("Saving changes to privileges");
            currentLicense.setPrivileges(new HashSet<>(currentLicense.getPrivilegesCopy()));
        }
        // Sync changes made to allowed subthemes
        if (currentLicense.getSelectableSubthemes() != null && !currentLicense.getSelectableSubthemes().isEmpty()) {
            currentLicense.getSubthemeDiscriminatorValues().clear();
            for (Selectable<String> selectable : currentLicense.getSelectableSubthemes()) {
                if (selectable.isSelected()) {
                    currentLicense.getSubthemeDiscriminatorValues().add(selectable.getValue());
                }
            }
        }
        // Sync changes made to allowed categories
        if (currentLicense.getSelectableCategories() != null && !currentLicense.getSelectableCategories().isEmpty()) {
            currentLicense.getAllowedCategories().clear();
            for (Selectable<CMSCategory> selectable : currentLicense.getSelectableCategories()) {
                if (selectable.isSelected()) {
                    currentLicense.getAllowedCategories().add(selectable.getValue());
                }
            }
        }
        // Sync changes made to allowed templates
        if (currentLicense.getSelectableTemplates() != null && !currentLicense.getSelectableTemplates().isEmpty()) {
            currentLicense.getAllowedCmsTemplates().clear();
            for (Selectable<CMSPageTemplate> selectable : currentLicense.getSelectableTemplates()) {
                if (selectable.isSelected()) {
                    currentLicense.getAllowedCmsTemplates().add(selectable.getValue());
                }
            }
        }

        boolean error = false;
        if (currentLicense.getUser() != null) {
            // User
            currentLicense.getUser().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateUser(currentLicense.getUser())) {
                Messages.info(MSG_ADMIN_LICENSE_SAVE_SUCCESS);
            } else {
                Messages.error(MSG_ADMIN_LICENSE_SAVE_FAILURE);
                error = true;
            }
        } else if (currentLicense.getUserGroup() != null) {
            // UserGroup
            currentLicense.getUserGroup().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateUserGroup(currentLicense.getUserGroup())) {
                Messages.info(MSG_ADMIN_LICENSE_SAVE_SUCCESS);
            } else {
                Messages.error(MSG_ADMIN_LICENSE_SAVE_FAILURE);
                error = true;
            }
        } else if (currentLicense.getIpRange() != null) {
            // IpRange
            logger.trace("ip range id:{} ", currentLicense.getIpRange().getId());
            currentLicense.getIpRange().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().updateIpRange(currentLicense.getIpRange())) {
                Messages.info(MSG_ADMIN_LICENSE_SAVE_SUCCESS);
            } else {
                Messages.error(MSG_ADMIN_LICENSE_SAVE_FAILURE);
                error = true;
            }
        } else if (currentLicense.getClient() != null) {
            // IpRange
            logger.trace("client id:{} ", currentLicense.getClientId());
            currentLicense.getClient().addLicense(currentLicense);
            if (DataManager.getInstance().getDao().saveClientApplication(currentLicense.getClient())) {
                Messages.info(MSG_ADMIN_LICENSE_SAVE_SUCCESS);
            } else {
                Messages.error(MSG_ADMIN_LICENSE_SAVE_FAILURE);
                error = true;
            }
        } else {
            Messages.error(MSG_ADMIN_LICENSE_SAVE_FAILURE);
            error = true;
        }

        if (error) {
            if (currentLicense.getId() != null) {
                return "pretty:adminRightsEdit";
            }
            return "pretty:adminRightsNew";
        }

        return "pretty:adminRights";
    }

    /**
     * <p>
     * deleteLicenseAction.
     * </p>
     *
     * @param license a {@link io.goobi.viewer.model.security.License} object.
     * @return a {@link java.lang.String} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteLicenseAction(License license) throws DAOException {
        if (license == null) {
            throw new IllegalArgumentException("license may not be null");
        }

        boolean success = false;
        logger.debug("removing license: {}", license.getLicenseType().getName());
        if (license.getUser() != null) {
            license.getUser().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUser(license.getUser());
        } else if (license.getUserGroup() != null) {
            license.getUserGroup().removeLicense(license);
            success = DataManager.getInstance().getDao().updateUserGroup(license.getUserGroup());
        } else if (license.getIpRange() != null) {
            license.getIpRange().removeLicense(license);
            success = DataManager.getInstance().getDao().updateIpRange(license.getIpRange());
        } else if (license.getClient() != null) {
            license.getClient().removeLicense(license);
            success = DataManager.getInstance().getDao().saveClientApplication(license.getClient());
        }

        if (success) {
            Messages.info("license_deleteSuccess");
        } else {
            Messages.error("license_deleteFailure");
        }

        return "pretty:adminRights";
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelDownloadTickets</code>.
     * </p>
     *
     * @return the lazyModelDownloadTickets
     */
    public TableDataProvider<DownloadTicket> getLazyModelDownloadTickets() {
        return lazyModelDownloadTickets;
    }

    /**
     * <p>
     * getPageDownloadTickets.
     * </p>
     *
     * @return a {@link java.util.List} object.
     */
    public List<DownloadTicket> getPageDownloadTickets() {
        return lazyModelDownloadTickets.getPaginatorList();
    }

    /**
     * 
     * @return List of existing download tickets that are in request status
     * @throws DAOException
     */
    public List<DownloadTicket> getDownloadTicketRequests() throws DAOException {
        return DataManager.getInstance().getDao().getDownloadTicketRequests();
    }

    /**
     * 
     * @param ticket
     * @throws DAOException
     */
    private static void saveTicket(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }

        // Persist changes
        if (DataManager.getInstance().getDao().updateDownloadTicket(ticket)) {
            logger.trace("Download ticket '{}' updated successfully", ticket.getId());
            Messages.info(StringConstants.MSG_ADMIN_UPDATED_SUCCESSFULLY);
        } else {
            Messages.error(StringConstants.MSG_ADMIN_SAVE_ERROR);
        }
    }

    /**
     * 
     * @param ticket
     * @param emailSubjectKey
     * @param emailBodyKey
     * @param emailBodyParams
     * @return Navigation outcome 
     */
    private static String notifyOwner(DownloadTicket ticket, String emailSubjectKey, String emailBodyKey, List<String> emailBodyParams) {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        if (emailSubjectKey == null) {
            throw new IllegalArgumentException("emailSubjectKey may not be null");
        }
        if (emailBodyKey == null) {
            throw new IllegalArgumentException("emailBodyKey may not be null");
        }

        String subject = ViewerResourceBundle.getTranslation(emailSubjectKey, BeanUtils.getLocale()).replace("{0}", ticket.getLabel());
        String body = ViewerResourceBundle.getTranslation(emailBodyKey, BeanUtils.getLocale());
        if (emailBodyParams != null) {
            for (int i = 0; i < emailBodyParams.size(); ++i) {
                body = body.replace("{" + i + "}", emailBodyParams.get(i) != null ? emailBodyParams.get(i) : "NOT FOUND");
            }
        }

        try {
            if (NetTools.postMail(Collections.singletonList(ticket.getEmail()), null, null, subject, body)) {
                Messages.info("admin__email_sent");
            } else {
                Messages.error(StringConstants.MSG_ERR_SEND_EMAIL);
            }
        } catch (UnsupportedEncodingException | MessagingException e) {
            logger.error(e.getMessage());
            Messages.error(StringConstants.MSG_ERR_SEND_EMAIL);
        }

        return "";
    }

    /**
     * 
     * @param ticket
     * @return Navigation outcome
     * @throws DAOException
     */
    public String activateDownloadTicketAction(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        logger.trace("activateDownloadTicketAction: {}", ticket.getId());

        // Set creation and expiration dates
        ticket.activate();

        saveTicket(ticket);

        // Notify owner
        return notifyOwner(ticket, StringConstants.MSG_DOWNLOAD_TICKET_EMAIL_SUBJECT, "download_ticket__email_body_activation",
                Arrays.asList(ticket.getPi(), ticket.getPassword(), DateTools.formatterDEDateTimeNoSeconds.format(ticket.getExpirationDate())));
    }

    /**
     * 
     * @param ticket
     * @return Navigation outcome
     * @throws DAOException
     */
    public String extendDownloadTicketAction(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        logger.trace("extendDownloadTicketAction: {}", ticket.getId());

        // Set new expiration date
        ticket.extend(DownloadTicket.VALIDITY_DAYS);

        saveTicket(ticket);

        return notifyOwner(ticket, StringConstants.MSG_DOWNLOAD_TICKET_EMAIL_SUBJECT, "download_ticket__email_body_extention",
                Arrays.asList(ticket.getPi(), DateTools.formatterDEDateTimeNoSeconds.format(ticket.getExpirationDate())));
    }

    /**
     * 
     * @param ticket
     * @return Navigation outcome
     * @throws DAOException
     */
    public String renewDownloadTicketAction(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        logger.trace("renewDownloadTicketAction: {}", ticket.getId());

        // Generate new password and reset expiration date
        ticket.reset();

        saveTicket(ticket);

        // Notify owner
        return notifyOwner(ticket, StringConstants.MSG_DOWNLOAD_TICKET_EMAIL_SUBJECT, "download_ticket__email_body_renewal",
                Arrays.asList(ticket.getPi(), ticket.getPassword(), DateTools.formatterDEDateTimeNoSeconds.format(ticket.getExpirationDate())));
    }

    /**
     * 
     * @param ticket
     * @return Navigation outcome
     * @throws DAOException
     */
    public String rejectDownloadTicketAction(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        logger.trace("rejectDownloadTicketAction: {}", ticket.getId());

        if (DataManager.getInstance().getDao().deleteDownloadTicket(ticket)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
            return "";
        }

        return notifyOwner(ticket, StringConstants.MSG_DOWNLOAD_TICKET_EMAIL_SUBJECT, "download_ticket__email_body_rejection",
                Arrays.asList(ticket.getPi(), "info@example.com"));
    }

    /**
     * 
     * @param ticket
     * @return Navigation outcome
     * @throws DAOException
     */
    public String deleteDownloadTicketAction(DownloadTicket ticket) throws DAOException {
        if (ticket == null) {
            throw new IllegalArgumentException(EXCEPTION_TICKET_MAY_NOT_BE_NULL);
        }
        logger.trace("deleteDownloadTicketAction: {}", ticket.getId());

        if (DataManager.getInstance().getDao().deleteDownloadTicket(ticket)) {
            Messages.info("deletedSuccessfully");
        } else {
            Messages.error(StringConstants.MSG_ADMIN_DELETE_FAILURE);
        }

        return "pretty:adminDownloadTickets";
    }

    /*********************************** Getter and Setter ***************************************/

    /**
     * <p>
     * Getter for the field <code>currentRole</code>.
     * </p>
     *
     * @return the currentRole
     */
    public Role getCurrentRole() {
        return currentRole;
    }

    /**
     * <p>
     * Setter for the field <code>currentRole</code>.
     * </p>
     *
     * @param currentRole the currentRole to set
     */
    public void setCurrentRole(Role currentRole) {
        this.currentRole = currentRole;
    }

    /**
     * <p>
     * Getter for the field <code>currentLicenseType</code>.
     * </p>
     *
     * @return the currentLicenseType
     */
    public LicenseType getCurrentLicenseType() {
        return currentLicenseType;
    }

    /**
     * <p>
     * Setter for the field <code>currentLicenseType</code>.
     * </p>
     *
     * @param currentLicenseType the currentLicenseType to set
     */
    public void setCurrentLicenseType(LicenseType currentLicenseType) {
        if (currentLicenseType != null) {
            logger.trace("setCurrentLicenseType: {}", currentLicenseType.getName());
            // Prepare privileges working copy (but only if the same license type is not already set)
            if (!currentLicenseType.equals(this.currentLicenseType)) {
                currentLicenseType.setPrivilegesCopy(new HashSet<>(currentLicenseType.getPrivileges()));
            }
        }
        this.currentLicenseType = currentLicenseType;
    }

    /**
     * Returns the user ID of <code>currentLicenseType</code>.
     *
     * @return <code>currentLicenseType.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentLicenseTypeId() {
        if (currentLicenseType != null && currentLicenseType.getId() != null) {
            return currentLicenseType.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentUserGroup</code> by loading it from the DB via the given ID.
     *
     * @param id
     * @throws DAOException
     */
    public void setCurrentLicenseTypeId(Long id) throws DAOException {
        setCurrentLicenseType(DataManager.getInstance().getDao().getLicenseType(id));
    }

    /**
     * <p>
     * Getter for the field <code>currentLicense</code>.
     * </p>
     *
     * @return the currentLicense
     */
    public License getCurrentLicense() {
        return currentLicense;
    }

    /**
     * <p>
     * Setter for the field <code>currentLicense</code>.
     * </p>
     *
     * @param currentLicense the currentLicense to set
     */
    public void setCurrentLicense(License currentLicense) {
        if (currentLicense != null) {
            logger.trace("setCurrentLicense: {}", currentLicense);
            // Prepare privileges working copy (but only if the same license is not already set)
            currentLicense.resetTempData();
            if (!currentLicense.equals(this.currentLicense)) {
                currentLicense.setPrivilegesCopy(new HashSet<>(currentLicense.getPrivileges()));
            }
        }
        this.currentLicense = currentLicense;
    }

    /**
     * Returns the user ID of <code>currentLicense</code>.
     *
     * @return <code>currentLicense.id</code> if loaded and has ID; null if not
     */
    public Long getCurrentLicenseId() {
        if (currentLicense != null && currentLicense.getId() != null) {
            return currentLicense.getId();
        }

        return null;
    }

    /**
     * Sets <code>currentLicense</code> by loading it from the DB via the given ID.
     *
     * @param id
     * @throws DAOException
     */
    public void setCurrentLicenseId(Long id) throws DAOException {
        if (ObjectUtils.notEqual(getCurrentLicenseId(), id)) {
            setCurrentLicense(DataManager.getInstance().getDao().getLicense(id));
        }
    }

    /**
     * Queries Solr for a list of all values of the set ACCESSCONDITION
     *
     * @return A list of all indexed ACCESSCONDITIONs
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     */
    public List<String> getPossibleAccessConditions() throws IndexUnreachableException, PresentationException {
        List<String> accessConditions = SearchHelper.getFacetValues(
                "+" + SolrConstants.ACCESSCONDITION + ":[* TO *] -" + SolrConstants.ACCESSCONDITION + ":" + SolrConstants.OPEN_ACCESS_VALUE,
                SolrConstants.ACCESSCONDITION, 1);
        Collections.sort(accessConditions);
        return accessConditions;
    }

    /**
     *
     * @return List of access condition values that have no corresponding license type in the database
     * @throws IndexUnreachableException
     * @throws PresentationException
     * @throws DAOException
     */
    public List<String> getNotConfiguredAccessConditions() throws PresentationException, DAOException {
        List<String> accessConditions;
        try {
            accessConditions = getPossibleAccessConditions();
        } catch (IndexUnreachableException e) {
            logger.error("Solr error: {}", SolrTools.extractExceptionMessageHtmlTitle(e.getMessage()));
            return Collections.emptyList();
        } catch (PresentationException e) {
            logger.error(e.getMessage());
            return Collections.emptyList();
        }

        if (accessConditions.isEmpty()) {
            return Collections.emptyList();
        }

        List<LicenseType> licenseTypes = getAllLicenseTypes();
        if (licenseTypes.isEmpty()) {
            return accessConditions;
        }

        List<String> ret = new ArrayList<>();
        for (String accessCondition : accessConditions) {
            boolean found = false;
            for (LicenseType licenseType : licenseTypes) {
                if (licenseType.getName().equals(accessCondition)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                ret.add(accessCondition);
            }
        }

        return ret;

    }

    /**
     *
     * @param accessCondition
     * @return Number of records containing the given access condition value
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public long getNumRecordsWithAccessCondition(String accessCondition) throws IndexUnreachableException, PresentationException {
        return DataManager.getInstance()
                .getSearchIndex()
                .getHitCount(SearchHelper.getQueryForAccessCondition(accessCondition, false));
    }

    /**
     *
     * @param accessCondition
     * @return Generated query for given accessCondition
     */
    public String getUrlQueryForAccessCondition(String accessCondition) {
        String query = SearchHelper.getQueryForAccessCondition(accessCondition, true);
        if (query == null) {
            return null;
        }
        query = BeanUtils.escapeCriticalUrlChracters(query);
        try {
            return URLEncoder.encode(query, StringTools.DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return query;
        }
    }

    /**
     *
     * @param privilege
     * @return Composite message key for the given privilege name
     */
    public String getMessageKeyForPrivilege(String privilege) {
        return "license_priv_" + privilege.toLowerCase();
    }
}
