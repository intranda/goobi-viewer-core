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
package io.goobi.viewer.model.annotation;

import java.net.URI;

import de.intranda.api.annotation.IAnnotation;
import de.intranda.api.annotation.wa.collection.AnnotationCollection;
import de.intranda.api.annotation.wa.collection.AnnotationPage;
import de.intranda.metadata.multilanguage.IMetadataValue;

/**
 * @author florian
 *
 */
public class AnnotationCollectionBuilder {
    
    private int itemsPerPage = 10;
    
    public AnnotationCollectionBuilder setItemsPerPage(int itemsPerPage) {
        this.itemsPerPage = itemsPerPage;
        return this;
    }
    
    public AnnotationCollection build(int totalItemCount, URI baseURI, IMetadataValue label) {
        
        AnnotationCollection collection = new AnnotationCollection<IAnnotation>(baseURI);
        
        collection.setTotalItems(totalItemCount);
        collection.setLabel(label);
        collection.setFirst(getFirstPage(baseURI.toString()));
        if(totalItemCount > 0) {            
            collection.setLast(getLastPage(baseURI.toString(), totalItemCount));
        }

        return collection
    }

    /**
     * @param baseURI
     * @return
     */
    private AnnotationPage getLastPage(String baseURI, int totalItemCount) {
  
        if(!baseURI.endsWith("/")) {
            baseURI = baseURI + "/";
        }
        URI id = URI.create(baseURI + getLastPageNo(totalItemCount)); 
        AnnotationPage page = new AnnotationPage<>(id);
        return page;
    }

    /**
     * @param totalItemCount
     * @return
     */
    private int getLastPageNo(int totalItemCount) {
        return (totalItemCount/itemsPerPage) + 1;
    }

    /**
     * @param baseURI
     * @return
     */
    private AnnotationPage getFirstPage(String baseURI) {
        if(!baseURI.endsWith("/")) {
            baseURI = baseURI + "/";
        }
        URI id = URI.create(baseURI + "1"); 
        AnnotationPage page = new AnnotationPage<>(id);
        return page;
    }

}
