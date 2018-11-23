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
package de.intranda.digiverso.presentation.model.viewer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.controller.DateTools;
import de.intranda.digiverso.presentation.controller.SolrConstants;
import de.intranda.digiverso.presentation.controller.SolrSearchIndex;
import de.intranda.digiverso.presentation.exceptions.IndexUnreachableException;
import de.intranda.digiverso.presentation.managedbeans.ActiveDocumentBean;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;
import de.intranda.digiverso.presentation.model.metadata.Metadata;

/**
 * Event.
 */
public class EventElement implements Comparable<EventElement>, Serializable {

    private static final long serialVersionUID = 5369209153499598371L;

    private static final Logger logger = LoggerFactory.getLogger(EventElement.class);

    private String type;
    private List<String> dateStringsStart = new ArrayList<>();
    private String dateEndString;
    private Date dateStart;
    private Date dateEnd;
    private String displayDate;
    private List<Metadata> metadata;
    private List<Metadata> sidebarMetadata;

    /**
     * 
     * @param doc
     * @param locale
     * @throws IndexUnreachableException
     * @should fill in missing dateStart from displayDate
     * @should fill in missing dateEnd from dateStart
     */
    public EventElement(SolrDocument doc, Locale locale) throws IndexUnreachableException {
        type = (String) doc.getFieldValue(SolrConstants.EVENTTYPE);
        logger.debug("new EventElement: {}", (type == null ? "(no type)" : type));

        Collection<Object> eventDateValues = doc.getFieldValues(SolrConstants.EVENTDATE);
        if (eventDateValues != null) {
            for (Object field : eventDateValues) {
                displayDate = (String) field;
                break;
            }
        }
        dateEndString = (String) doc.getFieldValue(SolrConstants.EVENTDATEEND);
        if (StringUtils.isNotEmpty(dateEndString) && dateEnd == null) {
            List<Date> dates = DateTools.parseMultipleDatesFromString(dateEndString);
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
                List<Date> dates = DateTools.parseMultipleDatesFromString(dateString);
                if (!dates.isEmpty() && dateStart == null) {
                    dateStart = dates.get(0);
                    if (dates.size() > 1 && dateEnd == null) {
                        dateEnd = dates.get(1);
                    }
                }
            }
        }
        checkDates();
        metadata = DataManager.getInstance().getConfiguration().getMainMetadataForTemplate(type);
        populateMetadata(metadata, type, doc, locale);
        sidebarMetadata = DataManager.getInstance().getConfiguration().getSidebarMetadataForTemplate(type);
        populateMetadata(sidebarMetadata, type, doc, locale);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(EventElement o) {
        if (o.getDateStart() != null && o.getDateEnd() != null && getDateStart() != null && getDateEnd() != null) {
            if (o.getDateStart().after(getDateEnd())) {
                return -1;
            }
            if (o.getDateEnd().before(getDateStart())) {
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
     * @param metadata
     * @param type
     * @param doc
     * @param locale
     * @param recordLanguage
     * @throws IndexUnreachableException
     */
    private static void populateMetadata(List<Metadata> metadata, String type, SolrDocument doc, Locale locale) throws IndexUnreachableException {
        // Get metadata list for the event type
        if (metadata != null) {
            logger.trace("Metadata for event '{}'", type);
            for (Metadata md : metadata) {
                md.populate(SolrSearchIndex.getFieldValueMap(doc), locale);
                if (md.getValues() != null && !md.getValues().isEmpty()) {
                    logger.trace("{}: {}", md.getLabel(), SolrSearchIndex.getFieldValueMap(doc).toString());
                }
            }
        }
    }

    /**
     * Checks the presence of date objects and fills in the gaps, if necessary.
     */
    private void checkDates() {
        if (dateStart == null && displayDate != null) {
            List<Date> dates = DateTools.parseMultipleDatesFromString(displayDate);
            if (!dates.isEmpty()) {
                dateStart = dates.get(0);
            }
        }
        if (dateEnd == null && dateStart != null) {
            dateEnd = dateStart;
        }
        // logger.debug("dateStart: " + dateStart.toString());
        // logger.debug("dateEnd: " + dateEnd.toString());
    }

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

    public String getLabel() {
        String type = getType();
        String date = getDisplayDate();
        if (StringUtils.isNotEmpty(date)) {
            return new StringBuilder(type).append(" (").append(date).append(')').toString();
        }
        return type;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the dateStart
     */
    public Date getDateStart() {
        return dateStart;
    }

    /**
     * @param dateStart the dateStart to set
     */
    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    /**
     * @return the dateEnd
     */
    public Date getDateEnd() {
        return dateEnd;
    }

    /**
     * @param dateEnd the dateEnd to set
     */
    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    /**
     * @return the metadata
     */
    public List<Metadata> getMetadata() {
        ActiveDocumentBean adb = BeanUtils.getActiveDocumentBean();
        if (adb != null) {
            List<Metadata> ret = Metadata.filterMetadataByLanguage(metadata, adb.getSelectedRecordLanguage());
            return ret;
        }

        return metadata;
    }

    public boolean hasMetadata() {
        if (metadata != null) {
            return metadata.stream().anyMatch(md -> !md.isBlank());
        }

        return false;
    }

    public boolean hasSidebarMetadata() {
        if (sidebarMetadata != null) {
            return sidebarMetadata.stream().anyMatch(md -> !md.isBlank());
        }

        return false;
    }

    /**
     * @return the sidebarMetadata
     */
    public List<Metadata> getSidebarMetadata() {
        return sidebarMetadata;
    }
}
