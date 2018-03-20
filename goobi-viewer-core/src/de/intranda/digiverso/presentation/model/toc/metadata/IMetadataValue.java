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
import java.util.Locale;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * @author Florian Alpers
 *
 */
public interface IMetadataValue {
    
    /**
     * Set the value for a specific locale
     * @param value
     * @param locale
     */
    public void setValue(String value, Locale locale);
    /**
     * Set the value for a specific locale
     * @param value
     * @param locale
     */
    public void setValue(String value, String locale);
    /**
     * Set the value for the default locale {@code _DEFAULT} or the first available locale if no default locale exists
     * @param value
     * @param locale
     */
    public void setValue(String value);
    
    /**
     * Get the value for a specific locale
     * @param value
     * @param locale
     */
    public Optional<String> getValue(Locale language);
    /**
     * Get the value for a specific locale
     * @param value
     * @param locale
     */
    public Optional<String> getValue(String language);
    /**
     * Get the value for the default locale {@code _DEFAULT}
     * @param value
     * @param locale
     */
    public Optional<String> getValue();
    
    /**
     * @return  A collection of all languages for which values exist. If no language specific values exist, only {@code _DEFAULT} is returned
     */
    public Collection<String> getLanguages();
 
    /**
     * Prepend the given string to all values
     *
     * @param prefix
     */
    public void addPrefix(String prefix);
    
    /**
     * Append the given string to all values
     *
     * @param prefix
     */
    public void addSuffix(String suffix);
    
    /**
     * Sets each value to the result of the given {@link UnaryOpeator} with the original value as input parameter
     * 
     * @param function
     */
    public void mapEach(UnaryOperator<String> function);
    
    /**
     * @return true if no values are stored in this object
     */
    public boolean isEmpty();

    /**
     * @param locale
     * @return  true if no entry is set for the given locale
     */
    public boolean isEmpty(Locale locale);
    
    /**
     * @param locale
     * @return  true if no entry is set for the given locale
     */
    public boolean isEmpty(String locale);
}
