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
package de.intranda.digiverso.presentation.model.cms;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.Helper;
import de.intranda.digiverso.presentation.messages.Messages;
import de.intranda.digiverso.presentation.model.misc.GeoLocation;
import de.intranda.digiverso.presentation.model.misc.GeoLocationInfo;
import de.intranda.digiverso.presentation.model.misc.NumberIterator;
import de.intranda.digiverso.presentation.servlets.rest.cms.CMSContentResource;

@Entity
@Table(name = "cms_sidebar_elements")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "widget_type")
public class CMSSidebarElement {

    /**
     * 
     */
    private static final String JSON_PROPERTYNAME_GEOLOCATIONS = "locations";
    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarElement.class);
    protected static final int HASH_MULTIPLIER = 11;
    private static final NumberIterator ID_COUNTER = new NumberIterator();

    private static Pattern patternHtmlTag = Pattern.compile("<.*?>");
    private static Pattern patternHtmlAttribute = Pattern.compile("[ ].*?[=][\"].*?[\"]");
    private static Pattern patternCssClass = Pattern.compile("[0-9a-z-_]*");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_sidebar_element_id")
    private Long id;

    /** Reference to the owning <code>CMSPage</code>. */
    @ManyToOne
    @JoinColumn(name = "owner_page_id", nullable = false)
    private CMSPage ownerPage;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "value")
    private String value;

    @Column(name = "sort_order")
    private int order;

    @Column(name = "inner_html", columnDefinition = "LONGTEXT")
    private String html = null;

    @Column(name = "css_class")
    private String cssClass = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_mode", nullable = false)
    private WidgetMode widgetMode = WidgetMode.STANDARD;

    @Column(name = "linked_pages", nullable = true)
    private String linkedPagesString = "";
    @Transient
    private PageList linkedPages = null;

    @Column(name = "geo_locations", columnDefinition = "LONGTEXT")
    private String geoLocationsString = null;
    @Transient
    private GeoLocationInfo geoLocations = null;

    @Column(name = "widget_type", nullable = false)
    private String widgetType = this.getClass().getSimpleName();

    @Column(name = "widget_title")
    private String widgetTitle = null;
    
    @Transient
    private final int sortingId = ID_COUNTER.next();

    public enum WidgetMode {
        STANDARD,
        FOLDOUT;
    }

    public CMSSidebarElement() {
        // the emptiness inside
    }

    public int compareTo(Object o) {
        CMSSidebarElement other = (CMSSidebarElement) o;
        if (other.order == order) {
            return 0;
        }
        if (other.order < order) {
            return 1;
        }

        return -1;
    }

    /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
    @Override
    public int hashCode() {
        int code = 21;
        code += HASH_MULTIPLIER * getType().hashCode();
        if (StringUtils.isNotBlank(getHtml())) {
            code += HASH_MULTIPLIER * getHtml().hashCode();
        }
        if (StringUtils.isNotBlank(getCssClass())) {
            code += HASH_MULTIPLIER * getCssClass().hashCode();
        }
        if (getLinkedPages() != null) {
            code += HASH_MULTIPLIER * getLinkedPages().hashCode();
        }
        return code;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(CMSSidebarElement.class) && bothNullOrEqual(getType(), ((CMSSidebarElement) o).getType()) && bothNullOrEqual(
                getHtml(), ((CMSSidebarElement) o).getHtml()) && bothNullOrEqual(getCssClass(), ((CMSSidebarElement) o).getCssClass())
                && bothNullOrEqual(getLinkedPages(), ((CMSSidebarElement) o).getLinkedPages());
    }

    protected static boolean bothNullOrEqual(Object o1, Object o2) {
        if (o1 == null) {
            return o2 == null;
        }
        return o1.equals(o2);
    }

    public String getHtml() {
        return this.html;
    }

    public void setHtml(String html) {
        //        this.html = html;
        this.html = correctHtml(html);
    }

    /**
     * @param html2
     * @return
     */
    private static String correctHtml(String string) {
        for (String key : CMSSidebarManager.getInstance().getHtmlReplacements().keySet()) {
            String replacement = CMSSidebarManager.getInstance().getHtmlReplacements().get(key);
            string = string.replaceAll(key, replacement);
        }
        return string;
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
     * @return the ownerPage
     */
    public CMSPage getOwnerPage() {
        return ownerPage;
    }

    /**
     * @param ownerPage the ownerPage to set
     */
    public void setOwnerPage(CMSPage ownerPage) {
        this.ownerPage = ownerPage;
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
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    public String getContent() {
        return CMSContentResource.getSidebarElementUrl(this);
    }

    public boolean hasHtml() {
        return getHtml() != null;
    }

    public WidgetMode getWidgetMode() {
        if (widgetMode == null) {
            widgetMode = WidgetMode.STANDARD;
        }
        //	logger.trace("Get widget mode {}", widgetMode);
        return widgetMode;
    }

    public void setWidgetMode(WidgetMode widgetMode) {
        //	logger.trace("Setting widget mode of {} to {}", type,  widgetMode);
        if (widgetMode == null) {
            widgetMode = WidgetMode.STANDARD;
        }
        this.widgetMode = widgetMode;
    }

    /**
     * @return the cssClass
     */
    public String getCssClass() {
        return cssClass;
    }

    /**
     * @param cssClass the cssClass to set
     */
    public void setCssClass(String className) {
        if (!validateCssClass(className)) {
            String msg = Helper.getTranslation("cms_validationWarningCssClassInvalid", null);
            Messages.error(msg.replace("{0}", this.getType()));
        } else {
            this.cssClass = className;
        }
    }

    /**
     * @param className
     * @return
     */
    private static boolean validateCssClass(String className) {
        return patternCssClass.matcher(className).matches();
    }

    /**
     * Tests whether the html contains only the allowed html-tags
     *
     * @return
     */
    public boolean isValid() {
        if (hasHtml()) {
            Matcher m = patternHtmlTag.matcher(html);
//            Set<String> allowedTags = CMSSidebarManager.getInstance().getAllowedHtmlTags();
            Set<String> disallowedTags = CMSSidebarManager.getInstance().getDisallowedHtmlTags();
            while (m.find()) {
                String tag = m.group();
                if (tag.startsWith("<!--")) {
                    continue;
                }
                tag = cleanupHtmlTag(tag);
                logger.trace("Check tag '{}' for validity", tag);
                if (disallowedTags != null && disallowedTags.contains(tag)) {
                    logger.debug("Tag '{}' is not allowed in sidebar widget HTML.", tag);
                    return false;
                }

            }
        }
        return true;
    }

    /**
     * Normalizes the given HTML tag so that it can be matched against <code>CMSSidebarManager.getAllowedHtmlTags()</code>.
     *
     * @param tag
     * @return
     * @should remove attributes correctly
     * @should remove closing tag correctly
     */
    protected static String cleanupHtmlTag(String tag) {
        // Remove attributes
        Matcher m2 = patternHtmlAttribute.matcher(tag);
        while (m2.find()) {
            String attribute = m2.group();
            tag = tag.replace(attribute, "");
        }
        tag = tag.replace("</", "<").replace("/>", ">").replace(" ", "");

        return tag;
    }

    public SidebarElementType.Category getCategory() {

        if (this instanceof CMSSidebarElementWithQuery) {
            return SidebarElementType.Category.fieldQuery;
        } else if (this instanceof CMSSidebarElementWithSearch) {
            return SidebarElementType.Category.search;
        } else if (this.getLinkedPages() != null) {
            return SidebarElementType.Category.pageLinks;
        } else if (this.getGeoLocations() != null) {
            return SidebarElementType.Category.geoLocations;
        }
        return this.getHtml() != null ? SidebarElementType.Category.custom : SidebarElementType.Category.standard;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getCanonicalName()).append('\n');
        sb.append(getType());
        sb.append(" (").append(getId()).append(") ");
        return sb.toString();
    }

    /**
     * @return the sortingId
     */
    public int getSortingId() {
        return sortingId;
    }

    /**
     * @return the linkedPages
     */
    public PageList getLinkedPages() {
        //        this.linkedPages = new PageList(this.linkedPagesString);
        return linkedPages;
    }

    /**
     * @param linkedPages the linkedPages to set
     */
    public void setLinkedPages(PageList linkedPages) {
        this.linkedPages = linkedPages;
    }

    /**
     * @return the linkedPagesList
     */
    public String getLinkedPagesString() {
        //        this.linkedPagesString = linkedPages.toString();
        return linkedPagesString;
    }

    /**
     * @param linkedPagesList the linkedPagesList to set
     */
    public void setLinkedPagesString(String linkedPagesList) {
        this.linkedPagesString = linkedPagesList;
    }

    public void serialize() {
        if (this.linkedPages != null) {
            this.linkedPagesString = linkedPages.toString();
        } else {
            this.linkedPagesString = null;
        }
        if (geoLocations != null) {
            this.geoLocationsString = createGeoLocationsString(geoLocations);
        }

    }

    public void deSerialize() {
        if (StringUtils.isNotEmpty(this.linkedPagesString)) {
            this.linkedPages = new PageList(this.linkedPagesString);
        } else {
            this.linkedPages = null;
        }
        if (StringUtils.isNotBlank(this.geoLocationsString)) {
            this.geoLocations = createGeoLocationsFromString(this.geoLocationsString);
        }
    }

    /**
     * 
     */
    public void initGeolocations(GeoLocationInfo info) {
        if(info.getLocationList().isEmpty()) {
            info.getLocationList().add(new GeoLocation());
        }
        this.geoLocations = info;
        this.geoLocationsString = createGeoLocationsString(this.geoLocations);
    }

    public GeoLocationInfo getGeoLocations() {
        return this.geoLocations;
    }

    public void addGeoLocation() {
        this.geoLocations.getLocationList().add(new GeoLocation());
    }

    public void removeGeoLocation() {
        if(geoLocations != null) {            
            this.geoLocations.getLocationList().remove(this.geoLocations.getLocationList().size()-1);
            if(this.geoLocations.getLocationList().isEmpty()) {
                this.geoLocations.getLocationList().add(new GeoLocation());
            }
        }
    }

    /**
     * @param geoLocationsString2
     * @return
     */
    private GeoLocationInfo createGeoLocationsFromString(String string) {

        try {
            JSONObject json = new JSONObject(string);
            GeoLocationInfo info = new GeoLocationInfo(json);
//            if(locations != null) {                
//                for (int i = 0; i < locations.length(); i++) {
//                    JSONObject obj = locations.getJSONObject(i);
//                    list.add(new GeoLocation(obj));
//                }
//            }
            return info;
        } catch (ParseException e) {
            logger.error("Failed to create geolocation list from string \n" + string, e);
        }
        return new GeoLocationInfo();
//        if(list.isEmpty()) {
//            list.add(new GeoLocation());
//        }
    }

    /**
     * @param geoLocations2
     * @return
     */
    private String createGeoLocationsString(GeoLocationInfo info) {

        JSONObject json = info.getAsJson();

//        JSONArray locations = new JSONArray();
//        list.stream()
//        .filter(loc -> !loc.isEmpty())
//        .map(loc -> loc.getAsJson())
//        .forEach(loc -> locations.put(loc));
//        
//        JSONObject json = new JSONObject();
//        json.put(JSON_PROPERTYNAME_GEOLOCATIONS, locations);

        return json.toString();
    }

    public String getGeoLocationsString() {
        return this.geoLocationsString;
    }
    /**
     * @return the widgetTitle
     */
    public String getWidgetTitle() {
        return widgetTitle;
    }
    
    /**
     * @param widgetTitle the widgetTitle to set
     */
    public void setWidgetTitle(String widgetTitle) {
        this.widgetTitle = widgetTitle;
    }
    
    public boolean isHasWidgetTitle() {
        return StringUtils.isNotBlank(getWidgetTitle());
    }
    
    public boolean isHasLinkedPages() {
        return this.linkedPages != null && !this.linkedPages.isEmpty();
    }

}
