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
package de.intranda.digiverso.presentation.model.toc.metadata;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Florian Alpers
 *
 */
public class SimpleMetadataValue implements IMetadataValue{

    String value = null;
    
    /**
     * 
     */
    public SimpleMetadataValue() {
    }
    
    public SimpleMetadataValue(String value) {
        this.value = value;
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.IMetadataValue#getValue(java.util.Locale)
     */
    @Override
    public Optional<String> getValue(Locale language) {
        return getValue();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.IMetadataValue#setValue(java.lang.String, java.util.Locale)
     */
    @Override
    public void setValue(String value, Locale locale) {
        setValue(value);
    }
    
    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#setValue(java.lang.String, java.lang.String)
     */
    @Override
    public void setValue(String value, String locale) {
        setValue(value);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#setValue(java.lang.String)
     */
    @Override
    public void setValue(String value) {
        this.value = value == null ? "" : value;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getValue(java.lang.String)
     */
    @Override
    public Optional<String> getValue(String language) {
        return getValue();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getValue()
     */
    @Override
    public Optional<String> getValue() {
        return Optional.ofNullable(value);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return value;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if(obj.getClass().equals(SimpleMetadataValue.class)) {
            return ((SimpleMetadataValue)obj).getValue().equals(this.getValue());
        }else {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#getLanguages()
     */
    @Override
    public Collection<String> getLanguages() {
        return Collections.singleton(MultiLanguageMetadataValue.DEFAULT_LANGUAGE);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#addPrefix(java.lang.String)
     */
    @Override
    public void addPrefix(String prefix) {
        this.value = prefix + this.value;
        
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#addSuffix(java.lang.String)
     */
    @Override
    public void addSuffix(String suffix) {
        this.value = this.value + suffix;
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#mapEach(java.util.function.UnaryOperator)
     */
    @Override
    public void mapEach(UnaryOperator<String> function) {
       this.value = function.apply(this.value);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty()
     */
    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(value);
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty(java.util.Locale)
     */
    @Override
    public boolean isEmpty(Locale locale) {
        return isEmpty();
    }

    /* (non-Javadoc)
     * @see de.intranda.digiverso.presentation.model.toc.metadata.IMetadataValue#isEmpty(java.lang.String)
     */
    @Override
    public boolean isEmpty(String locale) {
        return isEmpty();

    }
}
