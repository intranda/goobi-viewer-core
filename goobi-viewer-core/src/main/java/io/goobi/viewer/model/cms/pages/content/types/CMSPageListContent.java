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
package io.goobi.viewer.model.cms.pages.content.types;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.RandomComparator;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.pages.CMSPage;
import io.goobi.viewer.model.cms.pages.CMSTemplateManager;
import io.goobi.viewer.model.cms.pages.content.CMSCategoryHolder;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContent;
import io.goobi.viewer.model.jsf.CheckboxSelectable;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

@Entity
@Table(name = "cms_content_pagelist")
@DiscriminatorValue("pagelist")
public class CMSPageListContent extends CMSContent implements CMSCategoryHolder {

    private static final String COMPONENT_NAME = "pagelist";

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_content_pagelist_categories", joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();

    @Column(name = "group_by_category")
    private boolean groupByCategory = false;

    @Transient
    private List<CheckboxSelectable<CMSCategory>> selectableCategories = null;

    @Transient
    private List<CMSPage> nestedPages = null;
    @Transient
    private int nestedPagesCount = 0;
    @Transient
    @Inject
    private CMSTemplateManager templateManager;

    public CMSPageListContent() {
        super();
        this.categories = new ArrayList<>();
    }

    private CMSPageListContent(CMSPageListContent orig) {
        super(orig);
        this.categories = orig.categories;
    }

    @Override
    public String getBackendComponentName() {
        return COMPONENT_NAME;
    }

    public List<CMSCategory> getCategories() {
        return categories;
    }

    public List<CheckboxSelectable<CMSCategory>> getSelectableCategories() throws DAOException {
        if (this.selectableCategories == null) {
            createSelectableCategories();
        }
        return this.selectableCategories;

    }

    private void createSelectableCategories() throws DAOException {
        this.selectableCategories = new ArrayList<>(DataManager.getInstance()
                .getDao()
                .getAllCategories()
                .stream()
                .map(cat -> new CheckboxSelectable<>(this.categories, cat, CMSCategory::getName))
                .toList());
    }

    @Override
    public CMSContent copy() {
        return new CMSPageListContent(this);
    }

    @Override
    public List<File> exportHtmlFragment(String outputFolderPath, String namingScheme) throws IOException, ViewerConfigurationException {
        return Collections.emptyList();
    }

    @Override
    public String handlePageLoad(boolean resetResults, CMSComponent component) throws PresentationException {
        return null;
    }

    @Override
    public boolean addCategory(CMSCategory category) {
        if (!this.categories.contains(category)) {
            this.selectableCategories = null; //reset selectable categories
            return this.categories.add(category);
        }
        return false;
    }

    @Override
    public boolean removeCategory(CMSCategory category) {
        if (this.categories.contains(category)) {
            this.selectableCategories = null; //reset selectable categories
            return this.categories.remove(category);
        }
        return false;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     * 
     * @param random
     * @param paged
     * @param templateManager
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @deprecated use {@link #getNestedPages(Boolean, CMSTemplateManager)} instead
     */
    @Deprecated(since = "24.02")
    public List<CMSPage> getNestedPages(Boolean random, Boolean paged, CMSTemplateManager templateManager) throws DAOException {
        return getNestedPages(random, templateManager);
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     * 
     * @param random
     * @param templateManager
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPages(Boolean random, CMSTemplateManager templateManager) throws DAOException {
        if (nestedPages == null) {
            nestedPages = loadNestedPages(random, templateManager);
        }
        return nestedPages;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @param random
     * @param paged
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @param templateManager
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     * @deprecated use {@link #getNestedPagesByCategory(boolean, CMSCategory, CMSTemplateManager)} instead
     */
    @Deprecated(since = "24.02")
    public List<CMSPage> getNestedPagesByCategory(boolean random, boolean paged, CMSCategory category, CMSTemplateManager templateManager)
            throws DAOException {
        return getNestedPagesByCategory(random, category, templateManager);
    }

    /**
     * <p>
     * Getter for the field <code>nestedPages</code>.
     * </p>
     *
     * @param random
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     * @param templateManager
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSPage> getNestedPagesByCategory(boolean random, CMSCategory category, CMSTemplateManager templateManager)
            throws DAOException {
        if (nestedPages == null) {
            nestedPages = loadNestedPages(random, templateManager);
        }
        return nestedPages.stream()
                .filter(child -> this.getCategories().isEmpty()
                        || !CollectionUtils.intersection(this.getCategories(), child.getCategories()).isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * <p>
     * resetData.
     * </p>
     */
    public void resetData() {
        nestedPages = null;
    }

    /**
     * 
     * @param random
     * @param paged
     * @param templateManager
     * @return List<CMSPage>
     * @throws DAOException
     */
    private List<CMSPage> loadNestedPages(boolean random, CMSTemplateManager templateManager) throws DAOException {
        AtomicInteger totalPages = new AtomicInteger(0);
        Stream<CMSPage> nestedPagesStream = DataManager.getInstance()
                .getDao()
                .getAllCMSPages()
                .stream()
                .filter(CMSPage::isPublished)
                .filter(child -> getCategories().isEmpty() || !CollectionUtils.intersection(getCategories(), child.getCategories()).isEmpty())
                .map(CMSPage::new)
                .peek(child -> child.initialiseCMSComponents(templateManager))
                .peek(child -> totalPages.incrementAndGet());
        if (random) {
            nestedPagesStream = nestedPagesStream.sorted(new RandomComparator<>());
        } else {
            nestedPagesStream = nestedPagesStream.sorted(new CMSPage.PageComparator());
        }
        List<CMSPage> ret = nestedPagesStream.collect(Collectors.toList());
        setNestedPagesCount(totalPages.intValue());
        return ret;
    }

    /**
     * <p>
     * Getter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @return a int.
     */
    public int getNestedPagesCount() {
        return nestedPagesCount;
    }

    /**
     * <p>
     * Setter for the field <code>nestedPagesCount</code>.
     * </p>
     *
     * @param nestedPages a int.
     */
    public void setNestedPagesCount(int nestedPages) {
        this.nestedPagesCount = nestedPages;
    }

    /**
     * <p>
     * getSortedCategories.
     * </p>
     * 
     * @param pageNo
     * @param random
     * @param paged
     * @param templateManager
     * @return a {@link java.util.List} object.
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<CMSCategory> getSortedCategories(int pageNo, boolean random, boolean paged, CMSTemplateManager templateManager) throws DAOException {
        if (!this.categories.isEmpty()) {
            SortedMap<Long, CMSCategory> sortMap = new TreeMap<>();
            for (CMSCategory category : getCategories()) {
                long order = getNestedPagesByCategory(random, paged, category, templateManager).stream()
                        .filter(page -> page.getPageSorting() != null)
                        .mapToLong(CMSPage::getPageSorting)
                        .sorted()
                        .findFirst()
                        .orElse(Long.MAX_VALUE);
                while (sortMap.containsKey(order)) {
                    order++;
                }
                sortMap.put(order, category);
            }
            return new ArrayList<>(sortMap.values());
        }

        return Collections.emptyList();
    }

    public void setGroupByCategory(boolean groupByCategory) {
        this.groupByCategory = groupByCategory;
    }

    public boolean isGroupByCategory() {
        return groupByCategory;
    }

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
