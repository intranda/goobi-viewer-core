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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import javax.faces.view.ViewScoped;
import javax.inject.Named;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.LicenseDescription;
import io.goobi.viewer.controller.language.Language;
import io.goobi.viewer.managedbeans.utils.BeanUtils;

/**
 * @author florian
 *
 */
@Named
@ViewScoped
public class CreateRecordBean implements Serializable {

    private static final long serialVersionUID = -8052248087187114268L;
    private static final Logger logger = LoggerFactory.getLogger(CreateRecordBean.class);
    
    private String title;
    private String description;
    private Language language;
    private LocalDate date;
    private String creator;
    private String collection;
    private String accessCondition;
    private String license;
    
    public CreateRecordBean() {
        String languageCode = BeanUtils.getNavigationHelper().getLocale().getLanguage();
        this.language = DataManager.getInstance().getLanguageHelper().getLanguage(languageCode);
        
        this.date = LocalDate.now();
    }
    
    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }
    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    /**
     * @return the language
     */
    public Language getLanguage() {
        return language;
    }
    /**
     * @param language the language to set
     */
    public void setLanguage(Language language) {
        this.language = language;
    }
    /**
     * @return the date
     */
    public LocalDate getDate() {
        return date;
    }
    /**
     * @param date the date to set
     */
    public void setDate(LocalDate date) {
        this.date = date;
    }
    /**
     * @return the creator
     */
    public String getCreator() {
        return creator;
    }
    /**
     * @param creator the creator to set
     */
    public void setCreator(String creator) {
        this.creator = creator;
    }
    /**
     * @return the collection
     */
    public String getCollection() {
        return collection;
    }
    /**
     * @param collection the collection to set
     */
    public void setCollection(String collection) {
        this.collection = collection;
    }
    /**
     * @return the accessCondition
     */
    public String getAccessCondition() {
        return accessCondition;
    }
    /**
     * @param accessCondition the accessCondition to set
     */
    public void setAccessCondition(String accessCondition) {
        this.accessCondition = accessCondition;
    }
    /*
     * @return the license
     */
    public String getLicense() {
        return license;
    }
    /**
     * @param license the license to set
     */
    public void setLicense(String license) {
        this.license = license;
    }
    
    public void saveRecord() {
        System.out.println("Language is " + language.getGermanName());
        System.out.println("Date is " + (date != null ? date.toString() : "null")); 
        System.out.println("License is " + license);
    }
    
    public List<Language> getPossibleLanguages() {
        List<Language> languages = DataManager.getInstance().getLanguageHelper().getMajorLanguages();
        Locale locale = BeanUtils.getLocale();
        languages.sort((l1, l2) -> l1.getName(locale).compareTo(l2.getName(locale)));
        return languages;
    }
    
    public List<LicenseDescription> getPossibleLicenses() {
        return DataManager.getInstance().getConfiguration().getLicenseDescriptions();
    }
    
    private Document generateDCRecordXml() {
        Document doc = new Document();
        Namespace namespaceDC = Namespace.getNamespace("dc", "http://purl.org/dc/elements/1.1/");
        org.jdom2.Namespace asd;
        Element record = new Element("record", namespaceDC);
        
        return doc;
    }

}
