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

import static io.goobi.viewer.api.rest.v1.ApiUrls.CMS_MEDIA;
import static io.goobi.viewer.api.rest.v1.ApiUrls.CMS_MEDIA_BY_CATEGORY;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException;
import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.AbstractApiUrlManager.ApiPath;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.exceptions.ViewerConfigurationException;
import io.goobi.viewer.managedbeans.CmsBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.media.CMSMediaLister;
import io.goobi.viewer.model.cms.media.MediaItem;
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
@Table(name = "cms_content_imagelist")
@DiscriminatorValue("imagelist")
public class CMSImageListContent extends CMSContent implements CMSCategoryHolder {

    private static final String COMPONENT_NAME = "imagelist";
    private static final int DEFAULT_IMAGES_PER_VIEW = 9;
    private static final int DEFAULT_IMPORTANT_IMAGES_PER_VIEW = 0;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_content_imagelist_categories", joinColumns = @JoinColumn(name = "content_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();

    @Column(name = "images_per_view")
    private int imagesPerView;
    @Column(name = "important_images_per_view")
    private int importantImagesPerView;

    @Transient
    private List<CheckboxSelectable<CMSCategory>> selectableCategories = null;

    public CMSImageListContent() {
        super();
        this.categories = new ArrayList<>();
        imagesPerView = DEFAULT_IMAGES_PER_VIEW;
        importantImagesPerView = DEFAULT_IMPORTANT_IMAGES_PER_VIEW;
    }

    private CMSImageListContent(CMSImageListContent orig) {
        super(orig);
        this.categories = new ArrayList<>(orig.categories);
        this.imagesPerView = orig.imagesPerView;
        this.importantImagesPerView = orig.importantImagesPerView;
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

    public int getImagesPerView() {
        return imagesPerView;
    }

    public void setImagesPerView(int imagesPerView) {
        this.imagesPerView = imagesPerView;
    }

    public int getImportantImagesPerView() {
        return importantImagesPerView;
    }

    public void setImportantImagesPerView(int importantImagesPerView) {
        this.importantImagesPerView = importantImagesPerView;
    }

    public List<MediaItem> getMediaItems() throws DAOException {
        return getMediaItems(false);
    }

    public List<MediaItem> getMediaItems(boolean random) throws DAOException {
        return new CMSMediaLister(DataManager.getInstance().getDao())
                .getMediaItems(
                        this.categories.stream().map(CMSCategory::getName).toList(),
                        this.imagesPerView,
                        this.importantImagesPerView,
                        Boolean.TRUE.equals(random), BeanUtils.getRequest())
                .getMediaItems();
    }

    /**
     * <p>
     * getTileGridUrl.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     * @throws de.unigoettingen.sub.commons.contentlib.exceptions.IllegalRequestException if any.
     */
    public String getTileGridUrl() throws IllegalRequestException {

        String tags = this.getCategories().stream().map(CMSCategory::getName).collect(Collectors.joining("..."));

        return DataManager.getInstance()
                .getRestApiManager()
                .getDataApiManager()
                .map(urls -> buildTilegridUrl(urls, tags, this.getImagesPerView(), this.getImportantImagesPerView()))
                .orElse(getLegacyTileGridUrl());
    }

    /**
     * 
     * @param urls
     * @param tags
     * @param imagesPerView
     * @param priorityImagesPerView
     * @return Generated URL
     */
    private static String buildTilegridUrl(AbstractApiUrlManager urls, final String tags, int imagesPerView, int priorityImagesPerView) {
        ApiPath path = urls.path(CMS_MEDIA, CMS_MEDIA_BY_CATEGORY)
                .params(StringUtils.isBlank(tags) ? "-" : tags)
                .query("max", imagesPerView)
                .query("prioritySlots", priorityImagesPerView)
                .query("random", "true");
        return path.build();
    }

    /**
     * @return Generated URL
     */
    private String getLegacyTileGridUrl() {
        StringBuilder sb = new StringBuilder(BeanUtils.getServletPathWithHostAsUrlFromJsfContext());
        sb.append("/rest/tilegrid/")
                .append(CmsBean.getCurrentLocale().getLanguage())
                .append("/")
                .append(this.getImagesPerView())
                .append("/")
                .append(this.getImportantImagesPerView())
                .append("/")
                .append(this.getCategories().stream().map(CMSCategory::getName).collect(Collectors.joining("$")))
                .append("/");
        return sb.toString();
    }

    @Override
    public CMSContent copy() {
        return new CMSImageListContent(this);
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

    @Override
    public String getData(Integer w, Integer h) {
        return "";
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (this.getOwningComponent() != null) {
            sb.append("Parent ID = ")
                    .append(this.getOwningComponent().getId())
                    .append("|")
                    .append("Order = ")
                    .append(getOwningComponent().getOrder());
        }
        sb.append("|").append("Categories = ").append(this.categories);
        return sb.toString();
    }

}
