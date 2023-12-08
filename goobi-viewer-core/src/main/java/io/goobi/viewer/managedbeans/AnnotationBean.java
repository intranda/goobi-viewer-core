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

import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.dao.IDAO;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.messages.Messages;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.export.AnnotationSheetWriter;
import io.goobi.viewer.model.annotation.serialization.SqlAnnotationDeleter;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.questions.Question;
import io.goobi.viewer.model.misc.SelectionManager;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class AnnotationBean implements Serializable {

    private static final long serialVersionUID = 8377250065305331020L;

    private static final Logger logger = LogManager.getLogger(AnnotationBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 15;

    @Inject
    protected CrowdsourcingBean crowdsourcingBean;

    private TableDataProvider<CrowdsourcingAnnotation> lazyModelAnnotations;

    private SelectionManager<Long> exportSelection = new SelectionManager<>();

    private String ownerCampaignId = "";

    private String targetRecordPI = "";

    @PostConstruct
    public void init() {
        if (lazyModelAnnotations == null) {
            lazyModelAnnotations = new TableDataProvider<>(new TableDataSource<CrowdsourcingAnnotation>() {

                private Optional<Long> numCreatedPages = Optional.empty();

                @Override
                public List<CrowdsourcingAnnotation> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                        Map<String, String> filters) {
                    try {
                        String useSortField = sortField;
                        SortOrder useSortOrder = sortOrder;
                        if (StringUtils.isBlank(useSortField)) {
                            useSortField = "id";
                            useSortOrder = SortOrder.DESCENDING;
                        }
                        filters.putAll(getFilters());
                        List<CrowdsourcingAnnotation> ret =
                                DataManager.getInstance().getDao().getAnnotations(first, pageSize, useSortField, useSortOrder.asBoolean(), filters);
                        exportSelection = new SelectionManager<>(ret.stream().map(CrowdsourcingAnnotation::getId).collect(Collectors.toList()));
                        return ret;
                    } catch (DAOException e) {
                        logger.error("Could not initialize lazy model: {}", e.getMessage());
                    }

                    return Collections.emptyList();
                }

                /**
                 * @param filters
                 */
                public Map<String, String> getFilters() {
                    Map<String, String> filters = new HashMap<>();
                    if (StringUtils.isNotEmpty(getOwnerCampaignId())) {
                        filters.put("generatorId", getOwnerCampaignId());
                    }
                    if (StringUtils.isNotEmpty(getTargetRecordPI())) {
                        filters.put("targetPI", getTargetRecordPI());
                    }

                    return filters;
                }

                @Override
                public long getTotalNumberOfRecords(Map<String, String> filters) {
                    if (!numCreatedPages.isPresent()) {
                        try {
                            filters.putAll(getFilters());
                            numCreatedPages = Optional.ofNullable(DataManager.getInstance().getDao().getAnnotationCount(filters));
                        } catch (DAOException e) {
                            logger.error("Unable to retrieve total number of campaigns", e);
                        }
                    }
                    return numCreatedPages.orElse(0L);
                }

                @Override
                public void resetTotalNumberOfRecords() {
                    numCreatedPages = Optional.empty();
                }

            });
            lazyModelAnnotations.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
            lazyModelAnnotations.getFilter("targetPI_body");
        }
    }

    /**
     * <p>
     * Getter for the field <code>lazyModelAnnotations</code>.
     * </p>
     *
     * @return the lazyModelAnnotations
     */
    public TableDataProvider<CrowdsourcingAnnotation> getLazyModelAnnotations() {
        return lazyModelAnnotations;
    }

    /**
     * @return the ownerCampaignId
     */
    public String getOwnerCampaignId() {
        return ownerCampaignId;
    }

    /**
     * @param ownerCampaignId the ownerCampaignId to set
     */
    public void setOwnerCampaignId(String ownerCampaignId) {
        this.ownerCampaignId = ownerCampaignId;
    }

    /**
     * @return the targetRecordPI
     */
    public String getTargetRecordPI() {
        return targetRecordPI;
    }

    /**
     * @param targetRecordPI the targetRecordPI to set
     */
    public void setTargetRecordPI(String targetRecordPI) {
        this.targetRecordPI = targetRecordPI;
    }

    /**
     * @return the exportSelection
     */
    public SelectionManager<Long> getExportSelection() {
        return exportSelection;
    }

    /**
     * Deletes given annotation.
     *
     * @param annotation a {@link io.goobi.viewer.model.annotation.CrowdsourcingAnnotation} object.
     * @return empty string
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public String deleteAnnotationAction(CrowdsourcingAnnotation annotation) throws DAOException {
        if (annotation == null) {
            return "";
        }

        try {
            new SqlAnnotationDeleter().delete(annotation);
            Messages.info("admin__crowdsoucing_annotation_deleteSuccess");
            crowdsourcingBean.getLazyModelCampaigns().update();
        } catch (DAOException | IOException e) {
            logger.error(e.getMessage());
            Messages.error(e.getMessage());
        }

        return "";
    }

    public Optional<Campaign> getOwningCampaign(CrowdsourcingAnnotation anno) {
        try {
            IDAO dao = DataManager.getInstance().getDao();
            if (anno.getGeneratorId() != null) {
                Question question = dao.getQuestion(anno.getGeneratorId());
                if (question != null) {
                    return Optional.ofNullable(question.getOwner());
                }
            }
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
        return Optional.empty();
    }

    /**
     * Setter for {@link SelectionManager#setSelectAll(boolean) exportSelection#setSelectAll(boolean)} is placed here to avoid jsf confusing it with
     * setting a value of the map
     *
     * @param select
     */
    public void setSelectAll(boolean select) {
        this.exportSelection.setSelectAll(select);
    }

    /**
     * Getter for {@link SelectionManager#isSelectAll() exportSelection#isSelectAll()} is placed here to avoid jsf confusing it with getting a value
     * of the map
     *
     * @return always false to deselect the select all button when loading the page
     */
    public boolean isSelectAll() {
        return false;
        //this.exportSelection.isSelectAll();
    }

    public void downloadAllAnnotations() throws IOException, DAOException {
        downloadAnnotations(DataManager.getInstance().getDao().getAllAnnotations(null, false));
    }

    /**
     * Create an excel sheet and write it to download stream
     *
     * @throws IOException
     */
    public void downloadSelectedAnnotations() throws IOException {
        List<CrowdsourcingAnnotation> selectedAnnos = this.exportSelection.getAllSelected()
                .stream()
                .map(this::getAnnotationById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
        logger.debug("Selected {} annotations for excel download", selectedAnnos.size());
        downloadAnnotations(selectedAnnos);
    }

    /**
     * 
     * @param annotations
     * @throws IOException
     */
    public void downloadAnnotations(List<CrowdsourcingAnnotation> annotations) throws IOException {
        try {
            String fileName = "annotations.xlsx";

            FacesContext fc = FacesContext.getCurrentInstance();
            ExternalContext ec = fc.getExternalContext();
            // Some JSF component library or some Filter might have set some headers in the buffer beforehand.
            // We want to get rid of them, else it may collide.
            ec.responseReset();
            ec.setResponseContentType("application/msexcel");
            ec.setResponseHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
            OutputStream os = ec.getResponseOutputStream();
            AnnotationSheetWriter writer = new AnnotationSheetWriter();
            writer.createExcelSheet(os, annotations);
            fc.responseComplete();
            // Important! Otherwise JSF will attempt to render the response which obviously
            // will fail since it's already written with a file and closed.
        } finally {
            //
        }
    }

    public Optional<CrowdsourcingAnnotation> getAnnotationById(Long id) {
        return this.lazyModelAnnotations.getPaginatorList().stream().filter(anno -> id.equals(anno.getId())).findFirst();
    }

}
