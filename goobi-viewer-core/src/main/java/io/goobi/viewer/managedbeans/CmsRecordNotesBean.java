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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.NodeTraversor;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider;
import io.goobi.viewer.managedbeans.tabledata.TableDataProvider.SortOrder;
import io.goobi.viewer.managedbeans.tabledata.TableDataSource;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.recordnotes.CMSMultiRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSRecordNote;
import io.goobi.viewer.model.cms.recordnotes.CMSSingleRecordNote;
import io.goobi.viewer.servlets.IdentifierResolver;
import io.goobi.viewer.solr.SolrConstants;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 *
 * Bean used for listing and retrieving {@link CMSRecordNote}s.
 *
 * @author Florian Alpers
 */
@Named
@SessionScoped
public class CmsRecordNotesBean implements Serializable {

    private static final long serialVersionUID = 1436349423447175132L;

    private static final Logger logger = LogManager.getLogger(CmsRecordNotesBean.class);

    private static final int DEFAULT_ROWS_PER_PAGE = 10;

    public static final String PI_TITLE_FILTER = "PI_OR_TITLE";

    /** Note text variables, also URL-encoded because rich text editors may store them that way inside link targets. */
    private static final Pattern NOTE_VARIABLE = Pattern.compile("\\{(PAGE_URL|IMAGE_URL)\\}|%7B(PAGE_URL|IMAGE_URL)%7D", Pattern.CASE_INSENSITIVE);

    @Inject
    private ImageDeliveryBean images;

    @Inject
    private NavigationHelper navigationHelper;
    @Inject
    private ActiveDocumentBean activeDocumentBean;

    private TableDataProvider<CMSRecordNote> dataProvider;

    public CmsRecordNotesBean() {

    }

    /**
     * @param images Bean used for image delivery
     */
    public CmsRecordNotesBean(ImageDeliveryBean images) {
        this.images = images;
    }

    @PostConstruct
    public void init() {
        if (dataProvider == null) {
            initDataProvider();
        }
    }

    
    public TableDataProvider<CMSRecordNote> getDataProvider() {
        return dataProvider;
    }

    /**
     * Get the thumbnail url for the record related to the note.
     *
     * @param note Note whose related record's thumbnail is requested
     * @return Thumbnail URL
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     * @should return expected value for given input
     */
    public String getThumbnailUrl(CMSSingleRecordNote note) throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (StringUtils.isNotBlank(note.getRecordPi())) {
            return images.getThumbs().getThumbnailUrl(note.getRecordPi());
        }
        return "";
    }

    /**
     * Get the thumbnail url for the record related to the note for given width and height.
     *
     * @param note Note whose related record's thumbnail is requested
     * @param width Desired thumbnail width in pixels
     * @param height Desired thumbnail height in pixels
     * @return Thumbnail URL
     * @throws ViewerConfigurationException
     * @throws PresentationException
     * @throws IndexUnreachableException
     */
    public String getThumbnailUrl(CMSSingleRecordNote note, int width, int height)
            throws IndexUnreachableException, PresentationException, ViewerConfigurationException {
        if (note != null && StringUtils.isNotBlank(note.getRecordPi())) {
            // return images.getThumbs().getThumbnailUrl(note.getRecordPi(), width, height);
            if (note.getSolrDoc() == null) {
                note.setSolrDoc(
                        DataManager.getInstance().getSearchIndex().getFirstDoc("+" + SolrConstants.PI + ":\"" + note.getRecordPi() + '"', null));
            }
            if (note.getSolrDoc() != null) {
                return BeanUtils.getImageDeliveryBean().getThumbs().getThumbnailUrl(note.getSolrDoc(), width, height);
            }
        }

        return "";
    }

    public boolean deleteNote(CMSRecordNote note) throws DAOException {
        if (note != null && note.getId() != null) {
            return DataManager.getInstance().getDao().deleteRecordNote(note);
        }
        return false;
    }

    /**
     *
     * @param note Note whose related record URL is requested
     * @return Record URL
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    public String getRecordUrl(CMSSingleRecordNote note) throws PresentationException, IndexUnreachableException {
        if (note != null) {
            if (note.getSolrDoc() == null) {
                note.setSolrDoc(
                        DataManager.getInstance().getSearchIndex().getFirstDoc("+" + SolrConstants.PI + ":\"" + note.getRecordPi() + '"', null));
            }
            if (note.getSolrDoc() != null) {
                return navigationHelper.getApplicationUrl() + IdentifierResolver.constructUrl(note.getSolrDoc(), false).substring(1);
            }

            return navigationHelper.getMetadataUrl() + "/" + note.getRecordPi() + "/";
        }
        return "";
    }

    private void initDataProvider() {
        dataProvider = new TableDataProvider<>(new TableDataSource<CMSRecordNote>() {

            private Optional<Long> numCreatedPages = Optional.empty();

            @Override
            public List<CMSRecordNote> getEntries(int first, int pageSize, final String sortField, final SortOrder sortOrder,
                    Map<String, String> filters) {
                try {
                    String useSortField = sortField;
                    if (StringUtils.isBlank(useSortField)) {
                        useSortField = "id";
                    }

                    return DataManager.getInstance()
                            .getDao()
                            .getRecordNotes(first, pageSize, useSortField, sortOrder.asBoolean(), filters);
                } catch (DAOException e) {
                    logger.error("Could not initialize lazy model: {}", e.getMessage());
                }

                return Collections.emptyList();
            }

            @Override
            public long getTotalNumberOfRecords(Map<String, String> filters) {
                if (!numCreatedPages.isPresent()) {
                    try {
                        List<CMSRecordNote> notes = DataManager.getInstance()
                                .getDao()
                                .getRecordNotes(0, Integer.MAX_VALUE, null, false, filters);
                        numCreatedPages = Optional.of((long) notes.size());
                    } catch (DAOException e) {
                        logger.error("Unable to retrieve total number of cms pages", e);
                    }
                }
                return numCreatedPages.orElse(0L);
            }

            @Override
            public void resetTotalNumberOfRecords() {
                numCreatedPages = Optional.empty();
            }
        });
        dataProvider.setEntriesPerPage(DEFAULT_ROWS_PER_PAGE);
        dataProvider.getFilter(PI_TITLE_FILTER);
        //            lazyModelPages.addFilter("CMSCategory", "name");
    }

    public List<CMSRecordNote> getNotesForRecord(String pi) throws DAOException {
        List<CMSRecordNote> notes = new ArrayList<>();
        notes.addAll(DataManager.getInstance().getDao().getRecordNotesForPi(pi, true));
        notes.addAll(DataManager.getInstance()
                .getDao()
                .getAllMultiRecordNotes(true)
                .stream()
                .filter(note -> note.matchesRecord(pi))
                .collect(Collectors.toList()));
        return notes;
    }

    public String getSearchUrlForNote(CMSMultiRecordNote note) {
        String query = BeanUtils.escapeCriticalUrlChracters(note.getQueryForSearch());
        return PrettyUrlTools.getAbsolutePageUrl("newSearch5", "-", query, "1", "-", "-");
    }

    /**
     * Prepares a note text for display: "page" resolves its variables for the page shown in the record view, "client" keeps them for the browser
     * and any other context has no values for them.
     *
     * @param text note text as HTML
     * @param context variable context of the rendering view
     * @return the note text to display, or null if the note needs a variable value the context does not have
     * @should keep variables for the client and hide notes with variables elsewhere when values are missing
     */
    public String resolveNoteText(String text, String context) {
        if ("client".equals(context) || text == null || !NOTE_VARIABLE.matcher(text).find()) {
            return text;
        }
        String pageUrl = "";
        String imageUrl = "";
        if ("page".equals(context) && activeDocumentBean != null && activeDocumentBean.getViewManager() != null) {
            try {
                if (activeDocumentBean.getViewManager().getCurrentPage() != null) {
                    pageUrl = activeDocumentBean.getPageUrl();
                    imageUrl = activeDocumentBean.getViewManager().getCurrentImageUrl();
                }
            } catch (IndexUnreachableException | DAOException | ViewerConfigurationException e) {
                logger.warn("Could not resolve note variables for the current page: {}", e.getMessage());
            }
        }
        return resolveNoteVariables(text, pageUrl, imageUrl);
    }

    /**
     * Replaces {PAGE_URL} and {IMAGE_URL} in a note text. In a link target, values after the query mark are URL-encoded. Text without variables is
     * returned unchanged.
     *
     * @param text note text as HTML
     * @param pageUrl URL of the current page; may be blank
     * @param imageUrl URL of the current image; may be blank
     * @return the note text with its variables resolved, or null if one of its variables has no value
     * @should resolve variables in link targets and text
     * @should resolve url encoded variables in link targets
     * @should return null if a variable has no value
     * @should return text without variables unchanged
     */
    public String resolveNoteVariables(String text, String pageUrl, String imageUrl) {
        if (text == null || !NOTE_VARIABLE.matcher(text).find()) {
            return text;
        }
        Map<String, String> values = Map.of("PAGE_URL", StringUtils.trimToEmpty(pageUrl), "IMAGE_URL", StringUtils.trimToEmpty(imageUrl));
        Matcher matcher = NOTE_VARIABLE.matcher(text);
        while (matcher.find()) {
            if (values.get(variableName(matcher)).isEmpty()) {
                return null;
            }
        }
        Document doc = Jsoup.parseBodyFragment(text);
        doc.outputSettings().prettyPrint(false);
        for (Element link : doc.select("a[href]")) {
            link.attr("href", replaceNoteVariables(link.attr("href"), values, true));
        }
        NodeTraversor.traverse((node, depth) -> {
            if (node instanceof TextNode textNode && NOTE_VARIABLE.matcher(textNode.getWholeText()).find()) {
                textNode.text(replaceNoteVariables(textNode.getWholeText(), values, false));
            }
        }, doc.body());
        return doc.body().html();
    }

    private static String variableName(Matcher matcher) {
        return (matcher.group(1) != null ? matcher.group(1) : matcher.group(2)).toUpperCase();
    }

    /**
     * @return the value with its variables replaced; inside a link target, values after the query mark are URL-encoded
     */
    private static String replaceNoteVariables(String value, Map<String, String> values, boolean linkTarget) {
        int query = linkTarget ? value.indexOf('?') : -1;
        Matcher matcher = NOTE_VARIABLE.matcher(value);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String replacement = values.get(variableName(matcher));
            if (query >= 0 && matcher.start() > query) {
                replacement = encodeUriComponent(replacement);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * @return the value encoded like JavaScript's encodeURIComponent, so server and browser build identical links
     */
    private static String encodeUriComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("%21", "!")
                .replace("%27", "'")
                .replace("%28", "(")
                .replace("%29", ")")
                .replace("%7E", "~");
    }
}
