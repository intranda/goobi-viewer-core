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
package de.intranda.digiverso.presentation.model.iiif.presentation.builder;

import java.util.Comparator;

import de.intranda.digiverso.presentation.model.viewer.StructElement;

/**
 * Sorts StructElements by anchors first, then works, then everything else
 * within anchors and works, elements are sorted by PI, all other elements are sorted by LOGID
 * 
 * @author Florian Alpers
 *
 */
public class StructElementComparator implements Comparator<StructElement> {

    @Override
    public int compare(StructElement ds1, StructElement ds2) {
        if(ds1 == null) {
            return 1;
        } else if(ds2 == null) {
            return -1;
        }
        
        if(ds1.isAnchor() && ds2.isAnchor()) {
            return ds1.getPi().compareTo(ds2.getPi());
        } else if(ds1.isAnchor()) {
            return -1;
        } else if(ds2.isAnchor()) {
            return 1;
        }
        
        if(ds1.isWork() && ds2.isWork()) {
            return ds1.getPi().compareTo(ds2.getPi());
        } else if(ds1.isWork()) {
            return -1;
        } else if(ds2.isWork()) {
            return 1;
        }
        
        return ds1.getLogid().compareTo(ds2.getLogid());
    }

}
