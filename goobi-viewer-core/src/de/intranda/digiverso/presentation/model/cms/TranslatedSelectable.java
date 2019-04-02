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

import java.util.Locale;

/**
 * A {@link Selectable} which may also hold a locale to indicate the currently visible language/translation
 * 
 * @author florian
 *
 */
public class TranslatedSelectable<T> extends Selectable<T> {

	private Locale locale;
	
	/**
	 * @param value
	 * @param selected
	 */
	public TranslatedSelectable(T value, boolean selected, Locale defaultLocale) {
		super(value, selected);
		this.locale = defaultLocale;
		
	}
	
	/**
	 * @return the locale
	 */
	public Locale getLocale() {
		return locale;
	}
	
	/**
	 * @param locale the locale to set
	 */
	public void setLocale(Locale locale) {
		this.locale = locale;
	}
	
	public String getLanguage() {
		return locale.getLanguage();
	}
	
	public void setLanguage(String language) {
		this.locale = Locale.forLanguageTag(language);
	}
	
	

	/* (non-Javadoc)
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Selectable<T> other) {
		return super.compareTo(other);
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if(obj == null) {
			return false;
		} else if( obj == this) {
			return true;
		} else if(obj.getClass() == this.getClass()) {
			TranslatedSelectable other = (TranslatedSelectable)obj;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}

}
