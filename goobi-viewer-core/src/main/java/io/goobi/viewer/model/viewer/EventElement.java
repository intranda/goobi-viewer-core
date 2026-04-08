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
package io.goobi.viewer.model.viewer;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.solr.common.SolrDocument;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.DateTools;
import io.goobi.viewer.controller.StringConstants;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.managedbeans.ActiveDocumentBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.metadata.Metadata;
import io.goobi.viewer.solr.SolrConstants;
import io.goobi.viewer.solr.SolrTools;

/**
 * Event.
 */
public class EventElement implements Comparable<EventElement>, Serializable {

    private static final long serialVersionUID = 5369209153499598371L;

    private static final Logger logger = LogManager.getLogger(EventElement.class);

    private String type;
    private List<String> dateStringsStart = new ArrayList<>();
    private String dateEndString;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private String displayDate;
    private List<Metadata> metadata;
    private List<Metadata> sidebarMetadata;
    private List<Metadata> searchHitMetadata;

    /**
     * Creates a new EventElement instance.
     *
     * @param doc Event Solr document
     * @param locale locale for metadata translations
     * @param forSearchHit true to populate search hit metadata; false for full metadata
     * @throws io.goobi.viewer.exceptions.IndexUnreachableException if any.
     * @throws io.goobi.viewer.exceptions.PresentationException if any.
     * @should fill in missing dateStart from displayDate
     * @should fill in missing dateEnd from dateStart
     * @should populate search hit metadata correctly
     * @should populate non search metadata correctly
     */
    public EventElement(SolrDocument doc, Locale locale, boolean forSearchHit) throws IndexUnreachableException, PresentationException {
        type = (String) doc.getFieldValue(SolrConstants.EVENTTYPE);
        // logger.trace("new EventElement: {}", (type == null ? "(no type)" : type)); //NOSONAR Debug

        Collection<Object> eventDateValues = doc.getFieldValues(SolrConstants.EVENTDATE);
        if (eventDateValues != null && !eventDateValues.isEmpty()) {
            displayDate = (String) eventDateValues.iterator().next();
        }
        dateEndString = (String) doc.getFieldValue(SolrConstants.EVENTDATEEND);
        if (StringUtils.isNotEmpty(dateEndString) && dateEnd == null) {
            List<LocalDateTime> dates = DateTools.parseMultipleDatesFromString(dateEndString);
            if (!dates.isEmpty()) {
                dateEnd = dates.get(0);
            }
        }
        Collection<Object> colDateStartStrings = doc.getFieldValues(SolrConstants.EVENTDATESTART);
        if (dateStart == null) {
            if (colDateStartStrings != null) {
                for (Object o : colDateStartStrings) {
                    String s = (String) o;
                    if (s != null) {
                        dateStringsStart.add(s);
                    }
                }
            }
            for (String dateString : dateStringsStart) {
                List<LocalDateTime> dates = DateTools.parseMultipleDatesFromString(dateString);
                if (!dates.isEmpty() && dateStart == null) {
                    dateStart = dates.get(0);
                    if (dates.size() > 1 && dateEnd == null) {
                        dateEnd = dates.get(1);
                    }
                }
            }
        }
        checkDates();

        if (forSearchHit) {
            // Search metadata
            searchHitMetadata = DataManager.getInstance().getConfiguration().getSearchHitMetadataForTemplate(type);
            // logger.trace("event search hit metadata: {}", searchHitMetadata.size()); //NOSONAR Debug
            populateMetadata(searchHitMetadata, doc, locale);
        } else {
            // Main metadata
            metadata = DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(0, type);
            populateMetadata(metadata, doc, locale);

            // Sidebar metadata
            sidebarMetadata = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(type);
            if (sidebarMetadata.isEmpty()) {
                // Use default if no elements are defined for the current event type
                sidebarMetadata = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(StringConstants.DEFAULT_NAME);
            }
            populateMetadata(sidebarMetadata, doc, locale);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int compareTo(EventElement o) {
        if (o.getDateStart() != null && o.getDateEnd() != null && getDateStart() != null && getDateEnd() != null) {
            if (o.getDateStart().isAfter(getDateEnd())) {
                return -1;
            }
            if (o.getDateEnd().isBefore(getDateStart())) {
                return 1;
            }
        } else if (getDateStart() == null) {
            return 1;
        } else if (o.getDateStart() == null) {
            return -1;
        }

        return 0;
    }

    /**
     *
     * @param metadata list of metadata fields to populate
     * @param doc Solr document containing the event data
     * @param locale locale for translations/formatting
     * @throws IndexUnreachableException
     * @throws PresentationException
     */
    private static void populateMetadata(List<Metadata> metadata, SolrDocument doc, Locale locale)
            throws IndexUnreachableException, PresentationException {
        if (doc == null) {
            throw new IllegalArgumentException("doc may not be null");
        }
        if (metadata == null) {
            return;
        }

        // Get metadata list for the event type
        String iddoc = (String) doc.getFieldValue(SolrConstants.IDDOC);
        for (Metadata md : metadata) {
            StructElement se = new StructElement();
            se.setMetadataFields(SolrTools.getFieldValueMap(doc));
            md.populate(se, iddoc, md.getSortFields(), locale);
            //            if (md.getValues() != null && !md.getValues().isEmpty()) {
            //                logger.trace("{}: {}", md.getLabel(), SolrTools.getFieldValueMap(doc).toString()); //NOSONAR Debug
            //            }
        }
    }

    /**
     * Checks the presence of date objects and fills in the gaps, if necessary.
     */
    private void checkDates() {
        if (dateStart == null && displayDate != null) {
            List<LocalDateTime> dates = DateTools.parseMultipleDatesFromString(displayDate);
            if (!dates.isEmpty()) {
                dateStart = dates.get(0);
            }
        }
        if (dateEnd == null && dateStart != null) {
            dateEnd = dateStart;
        }
        // logger.debug("dateStart: " + dateStart.toString()); //NOSONAR Debug
        // logger.debug("dateEnd: " + dateEnd.toString()); //NOSONAR Debug
    }

    /**
     * Getter for the field <code>displayDate</code>.
     *
     * @return a {@link java.lang.String} object.
     */
    public String getDisplayDate() {
        if (StringUtils.isNotEmpty(displayDate)) {
            return displayDate;
        }
        StringBuilder sb = new StringBuilder();
        if (!dateStringsStart.isEmpty()) {
            sb.append(dateStringsStart.get(0));
        }
        if (StringUtils.isNotEmpty(dateEndString) && !dateEndString.equals(dateStringsStart.get(0))) {
            sb.append(" - ").append(dateEndString);
        }

        return sb.toString();
    }

    /**
     * getLabel.
     *
     * @return a {@link java.lang.String} object.
     * @should include type
     * @should not include date
     */
    public String getLabel() {
        String type = getType();
        //        String date = getDisplayDate();
        //        if (StringUtils.isNotEmpty(date)) {
        //            return new StringBuilder(type).append(" (").append(date).append(')').toString();
        //        }
        return type;
    }

    /**
     * Getter for the field <code>type</code>.
     *
     * @return the event type string (e.g. from MODS/LIDO metadata)
     */
    public String getType() {
        return type;
    }

    /**
     * Setter for the field <code>type</code>.
     *
     * @param type the event type string (e.g. from MODS/LIDO metadata)
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Getter for the field <code>dateStart</code>.
     *
     * @return the start date and time of this event
     */
    public LocalDateTime getDateStart() {
        return dateStart;
    }

    /**
     * Setter for the field <code>dateStart</code>.
     *
     * @param dateStart the start date and time of this event
     */
    public void setDateStart(LocalDateTime dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * Getter for the field <code>dateEnd</code>.
     *
     * @return the end date and time of this event
     */
    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    /**
     * Setter for the field <code>dateEnd</code>.
     *
     * @param dateEnd the end date and time of this event
     */
    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * Getter for the field <code>metadata</code>.
     *
     * @return the list of metadata entries for this event, filtered by current display language
     */
    public List<Metadata> getMetadata() {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            return Metadata.filterMetadata(metadata, adb.getSelectedRecordLanguage(), null);
        }

        return metadata;
    }

    /**
     * hasMetadata.
     *
     * @return true if this event element has at least one non-blank metadata entry, false otherwise
     */
    public boolean isHasMetadata() {
        if (metadata != null) {
            return metadata.stream().anyMatch(md -> !md.isBlank());
        }

        return false;
    }

    /**
     * hasSidebarMetadata.
     *
     * @return true if this event element has at least one non-blank sidebar metadata entry, false otherwise
     */
    public boolean isHasSidebarMetadata() {
        if (sidebarMetadata != null) {
            return sidebarMetadata.stream().anyMatch(md -> !md.isBlank());
        }

        return false;
    }

    /**
     * Getter for the field <code>sidebarMetadata</code>.
     *
     * @return the list of metadata entries displayed in the sidebar for this event
     */
    public List<Metadata> getSidebarMetadata() {
        return sidebarMetadata;
    }

    
    public List<Metadata> getSearchHitMetadata() {
        return searchHitMetadata;
    }

    /**
     * 
     * @param locale locale to filter metadata fields by
     * @return searchHitMetadata minus any fields that don't match the given locale
     */
    public List<Metadata> getSearchHitListForLocale(Locale locale) {
        return Metadata.filterMetadata(searchHitMetadata, locale != null ? locale.getLanguage() : null, null);
    }

    /**
     * 
     * @return searchHitMetadata minus any fields that don't match the current locale
     */
    public List<Metadata> getSearchHitMetadataForCurrentLocale() {
        return getSearchHitListForLocale(BeanUtils.getLocale());
    }
}
