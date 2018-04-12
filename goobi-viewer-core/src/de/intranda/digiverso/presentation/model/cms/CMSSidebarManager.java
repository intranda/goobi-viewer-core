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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.controller.DataManager;
import de.intranda.digiverso.presentation.model.cms.CMSSidebarElement.WidgetMode;

public final class CMSSidebarManager {

    private static final Logger logger = LoggerFactory.getLogger(CMSSidebarManager.class);

    @Deprecated
    private static final String[] ALLOWED_HTML_TAGS = { "<p>", "<ul>", "<ol>", "<li>", "<a>", "<h3>", "<h4>", "<h5>", "<h6>", "<i>", "<strong>",
        "<address>", "<abbr>", "<dl>", "<dt>", "<dd>", "<img>", "<span>", "<table>", "<tr>", "<th>", "<td>", "<thead>", "<tbody>", "<br>",
        "<br />", "<style>", "<div>" };
    private static final String[] DISALLOWED_HTML_TAGS = { "<script>" };

    private static final String[] HTML_REPLACEMENTS = { "<br\\s?>:=:<br />" };

    @Deprecated
    private Set<String> allowedHtmlTags;
    private Set<String> disallowedHtmlTags;
    @Deprecated
    private String allowedHtmlTagsForDisplay;
    private String disallowedHtmlTagsForDisplay;
    private Map<String, String> htmlReplacements;

    /**
     * This solution takes advantage of the Java memory model's guarantees about class initialization to ensure thread safety. Each class can only be
     * loaded once, and it will only be loaded when it is needed. That means that the first time getInstance is called, InstanceHolder will be loaded
     * and instance will be created, and since this is controlled by ClassLoaders, no additional synchronization is necessary.
     */
    private static class InstanceHolder {
        private static CMSSidebarManager instance = new CMSSidebarManager();
    }

    public static CMSSidebarManager getInstance() {
        return InstanceHolder.instance;
    }

    private CMSSidebarManager() {
        init();
    }

    private void init() {
        allowedHtmlTags = new HashSet<>(Arrays.asList(ALLOWED_HTML_TAGS));
        disallowedHtmlTags = new HashSet<>(Arrays.asList(DISALLOWED_HTML_TAGS));

        htmlReplacements = new HashMap<>();
        for (String string : HTML_REPLACEMENTS) {
            try {
                String[] strings = string.split(":=:");
                htmlReplacements.put(strings[0], strings[1]);
            } catch (NullPointerException | IndexOutOfBoundsException e) {
                logger.error("Html replacement \"" + string + "\" has an invalid format.");
            }
        }

        StringBuilder sb = new StringBuilder("");
        for (String tag : ALLOWED_HTML_TAGS) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(tag);
        }
        allowedHtmlTagsForDisplay = sb.toString();
        
        disallowedHtmlTagsForDisplay = StringUtils.join(disallowedHtmlTags, ", ");
    }

    public static List<CMSSidebarElement> getAvailableSidebarElements() {
        List<CMSSidebarElement> ret = new ArrayList<>();
        for (SidebarElementType element : SidebarElementType.values()) {
            CMSSidebarElement e = element.createSidebarElement();
            ret.add(e);
        }

        return ret;
    }

    /**
     * @return the sorted list of
     */
    public static List<CMSSidebarElement> getDefaultSidebarElements() {
        List<CMSSidebarElement> result = new ArrayList<>();
        List<SidebarElementType> elements = Arrays.asList(SidebarElementType.values());
        int order = 0;
        for (SidebarElementType e : elements) {
            CMSSidebarElement element = e.createSidebarElement();
            element.setOrder(order++);
            element.setWidgetMode(DataManager.getInstance()
                .getConfiguration()
                .isFoldout(e.getLabel()) ? WidgetMode.FOLDOUT : WidgetMode.STANDARD);
            result.add(element);
        }
        return result;
    }

    /**
     * Returns the HTML tags in ALLOWED_HTML_TAGS as a <code>HashSet</code> for fast <code>contains()</code> calls.
     *
     * @return
     */
    @Deprecated
    public Set<String> getAllowedHtmlTags() {
        return allowedHtmlTags;
    }
    
    /**
     * Returns the HTML tags in DISALLOWED_HTML_TAGS as a <code>HashSet</code> for fast <code>contains()</code> calls.
     *
     * @return
     */
    public Set<String> getDisallowedHtmlTags() {
        return disallowedHtmlTags;
    }

    /**
     *
     * @return
     */
    public Map<String, String> getHtmlReplacements() {
        return htmlReplacements;
    }

    /**
     *
     * @return
     */
    @Deprecated
    public String getAllowedHtmlTagsForDisplay() {
        return allowedHtmlTagsForDisplay;
    }

    /**
     * @return the disallowedHtmlTagsForDisplay
     */
    public String getDisallowedHtmlTagsForDisplay() {
        return disallowedHtmlTagsForDisplay;
    }
    
}
