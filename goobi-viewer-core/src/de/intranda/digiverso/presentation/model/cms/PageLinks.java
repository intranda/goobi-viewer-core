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
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.intranda.digiverso.presentation.exceptions.DAOException;
import de.intranda.digiverso.presentation.managedbeans.utils.BeanUtils;

/**
 * @author Florian Alpers
 *
 */
public class PageLinks extends ArrayList<CMSPage> {

    private static final Logger logger = LoggerFactory.getLogger(PageLinks.class);

    private static final long serialVersionUID = -4570389904428516931L;

    public PageLinks() {
        super();
    }

    public PageLinks(String stringRep) {
        super();
        if (StringUtils.isNotBlank(stringRep)) {
            String[] ids = stringRep.split(";");
            for (String id : ids) {
                if (StringUtils.isNotBlank(id) && id.matches("\\d+")) {
                    try {
                        CMSPage page = BeanUtils.getCmsBean().getPage(Long.parseLong(id));
                        this.add(page);
                    } catch (NumberFormatException | DAOException e) {
                        logger.error(e.toString(), e);
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        
        List<String> ids = new ArrayList<String>();
        for (CMSPage page : this) {
            Long id = page.getId();
            String idString = Long.toString(id);
            ids.add(idString);
        }
        return StringUtils.join(ids, ";");
//        String string = this.stream().map(page -> page.getId()).map(id -> Long.toString(id)).collect(Collectors.joining(";"));
//        return string;
    }

}
