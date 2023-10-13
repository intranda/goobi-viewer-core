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
package io.goobi.viewer.model.annotation.serialization;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocument;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

import de.intranda.api.annotation.wa.Motivation;
import de.intranda.api.annotation.wa.WebAnnotation;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.model.annotation.AnnotationConverter;
import io.goobi.viewer.model.annotation.CrowdsourcingAnnotation;
import io.goobi.viewer.model.annotation.PersistentAnnotation;
import io.goobi.viewer.model.annotation.comments.Comment;
import io.goobi.viewer.model.crowdsourcing.campaigns.Campaign;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordPageStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CampaignRecordStatistic;
import io.goobi.viewer.model.crowdsourcing.campaigns.CrowdsourcingStatus;
import io.goobi.viewer.modules.interfaces.IndexAugmenter;

/**
 * @author florian
 *
 */
public class AnnotationIndexAugmenter implements IndexAugmenter {

    private final Logger logger = LogManager.getLogger(AnnotationIndexAugmenter.class);

    /** Constant <code>SUFFIX_ANNOTATIONS="_annotations"</code> */
    public static final String SUFFIX_ANNOTATIONS = "_ugc";

    private final List<PersistentAnnotation> annotations;
    private final AbstractApiUrlManager urls = new ApiUrls(DataManager.getInstance().getConfiguration().getRestApiUrl());
    private final AnnotationConverter converter = new AnnotationConverter(urls);

    public AnnotationIndexAugmenter(Collection<PersistentAnnotation> annotations) {
        this.annotations = new ArrayList<>(annotations);
    }

    public AnnotationIndexAugmenter() {
        this.annotations = null; //load annotations in augemntReIndex... methods
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.modules.interfaces.IndexAugmenter#augmentReIndexRecord(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public void augmentReIndexRecord(String pi, String dataRepository, String namingScheme) throws Exception {

        Collection<PersistentAnnotation> annotations = this.annotations != null ? this.annotations : loadAllAnnotations(pi);

        if (!annotations.isEmpty()) {
            logger.debug("Found {} annotations for this record.", annotations.size());
            writeToHotfolder(namingScheme, annotations);
        }
    }

    /* (non-Javadoc)
     * @see io.goobi.viewer.modules.interfaces.IndexAugmenter#augmentReIndexPage(java.lang.String, int, org.apache.solr.common.SolrDocument, java.lang.String, java.lang.String)
     */
    @Override
    public boolean augmentReIndexPage(String pi, int page, SolrDocument doc, String dataRepository, String namingScheme) throws Exception {

        Collection<PersistentAnnotation> annotations = this.annotations != null ? this.annotations : loadAllAnnotations(pi, page);

        if (!annotations.isEmpty()) {
            logger.debug("Found {} annotations for this record.", annotations.size());
            writeToHotfolder(namingScheme, annotations);
        }
        return true;
    }

    private void writeToHotfolder(String namingScheme, Collection<PersistentAnnotation> annotations) {
        File annotationDir = new File(DataManager.getInstance().getConfiguration().getHotfolder(), namingScheme + SUFFIX_ANNOTATIONS);
        for (PersistentAnnotation annotation : annotations) {
            try {
                WebAnnotation webAnno = converter.getAsWebAnnotation(annotation);
                String json = webAnno.toString();
                String jsonFileName = annotation.getTargetPI() + "_" + annotation.getId() + ".json";
                FileUtils.writeStringToFile(new File(annotationDir, jsonFileName), json, Charset.forName(StringTools.DEFAULT_ENCODING));
            } catch (JsonParseException e) {
                logger.error(e.getMessage(), e);
            } catch (JsonMappingException e) {
                logger.error(e.getMessage(), e);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    private Collection<PersistentAnnotation> loadAllAnnotations(String pi) throws DAOException {
        List<PersistentAnnotation> annotations = new ArrayList<>();
        annotations.addAll(loadAllCampaignAnnotations(pi));
        annotations.addAll(loadAllCommentAnnotations(pi));
        return annotations;
    }

    private Collection<PersistentAnnotation> loadAllAnnotations(String pi, int page) throws DAOException {
        List<PersistentAnnotation> annotations = new ArrayList<>();
        annotations.addAll(loadAllCampaignAnnotations(pi, page));
        annotations.addAll(loadAllCommentAnnotations(pi, page));
        return annotations;
    }

    private Collection<PersistentAnnotation> loadAllCampaignAnnotations(String pi) throws DAOException {
        List<CampaignRecordStatistic> statistics =
                DataManager.getInstance().getDao().getCampaignStatisticsForRecord(pi, CrowdsourcingStatus.FINISHED);
        List<PersistentAnnotation> annotations = new ArrayList<>();
        for (CampaignRecordStatistic statistic : statistics) {
            Campaign campaign = statistic.getOwner();
            annotations.addAll(DataManager.getInstance().getDao().getAnnotationsForCampaignAndWork(campaign, pi));
        }

        List<CampaignRecordPageStatistic> pageStatistics =
                DataManager.getInstance().getDao().getCampaignPageStatisticsForRecord(pi, CrowdsourcingStatus.FINISHED);
        for (CampaignRecordPageStatistic statistic : pageStatistics) {
            Campaign campaign = statistic.getOwner().getOwner();
            Integer page = statistic.getPage();
            annotations.addAll(DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, pi, page));
        }

        return annotations;
    }

    private Collection<CrowdsourcingAnnotation> loadAllCampaignAnnotations(String pi, int page) throws DAOException {
        List<CampaignRecordPageStatistic> pageStatistics =
                DataManager.getInstance().getDao().getCampaignPageStatisticsForRecord(pi, CrowdsourcingStatus.FINISHED);
        CampaignRecordPageStatistic statistic = pageStatistics.stream()
                .filter(s -> s.getPage() == page)
                .findAny()
                .orElseThrow(() -> new IllegalStateException("No page number " + page + " found"));
        Campaign campaign = statistic.getOwner().getOwner();
        List<CrowdsourcingAnnotation> annotations = DataManager.getInstance().getDao().getAnnotationsForCampaignAndTarget(campaign, pi, page);
        return annotations;
    }

    private Collection<Comment> loadAllCommentAnnotations(String pi, int page) throws DAOException {
        List<Comment> comments = DataManager.getInstance().getDao().getCommentsForPage(pi, page);
        return comments;
    }

    private Collection<CrowdsourcingAnnotation> loadAllCommentAnnotations(String pi) throws DAOException {
        List<CrowdsourcingAnnotation> annos = DataManager.getInstance().getDao().getAnnotationsForTarget(pi, null, Motivation.COMMENTING);
        return annos;
    }

    @Override
    public int hashCode() {
        return this.annotations == null ? 0 : this.annotations.hashCode();
    }

    /**
     * Two instances are equal if they contain the same annotations (disregarding order)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(AnnotationIndexAugmenter.class)) {
            return CollectionUtils.isEqualCollection(this.annotations, ((AnnotationIndexAugmenter) obj).annotations);
        } else {
            return false;
        }
    }

}
