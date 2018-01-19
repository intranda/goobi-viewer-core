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
package de.intranda.digiverso.presentation.managedbeans.tabledata;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * @author Florian Alpers
 *
 */
public class PersistentTableDataProvider<T> extends TableDataProvider<T> {
    
    private Optional<List<T>> currentList = Optional.empty();
    
    /**
     * @param source
     */
    public PersistentTableDataProvider(TableDataSource<T> source) {
        super(source);
    }


    @Override
    public List<T> getPaginatorList() throws TableDataSourceException {
        return this.currentList.orElseGet(() -> {
            this.currentList = loadList();
            return this.currentList.orElse(Collections.emptyList());
        });
    }
    
    protected void resetCurrentList() {
        this.currentList = Optional.empty();
    }
}
