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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.commons.lang3.StringUtils;

import io.goobi.viewer.controller.DateTools;

@Named("utils")
@ApplicationScoped
public class UtilityBean implements Serializable {

    private static final long serialVersionUID = -4109004802230106329L;

    public Map createMap(List list) {
        Map map = new LinkedHashMap<>();
        if (list != null && list.size() > 1) {
            for (int i = 0; i < list.size() - 1; i += 2) {
                int keyIndex = i;
                int valueIndex = i + 1;
                Object key = list.get(keyIndex);
                if (list.get(valueIndex) instanceof List) {
                    Map value = createMap((List) list.get(valueIndex));
                    map.put(key, value);
                } else {
                    map.put(key, list.get(valueIndex));
                }
            }
        }
        
        return map;
    }

    public LocalDate getAsDate(String string) {
        if (StringUtils.isNotBlank(string)) {
            return DateTools.parseDateFromString(string).toLocalDate();
        }
        
        return null;
    }

    public LocalDateTime getAsDateTime(String string) {
        if (StringUtils.isNotBlank(string)) {
            return DateTools.parseDateFromString(string);
        }
        
        return null;
    }
}
