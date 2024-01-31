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
package io.goobi.viewer.model.administration.configeditor;

import java.io.Serializable;
import java.time.LocalDateTime;

import io.goobi.viewer.controller.DateTools;

public class BackupRecord implements Serializable {

    private static final long serialVersionUID = -7935008140086500081L;

    /** Date/time part of file name */
    private String name;
    /** Index in list */
    private int number;

    /**
     * 
     * @param name
     * @param i
     */
    public BackupRecord(String name, int i) {
        this.name = name;
        this.number = i;
    }

    /**
     * 
     * @return <code>name</code> as a {@link LocalDateTime}
     */
    public LocalDateTime getDate() {
        return LocalDateTime.parse(name, DateTools.FORMATTERFILENAME);
    }

    /**
     * 
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * 
     * @return the number
     */
    public int getNumber() {
        return number;
    }

}
