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
package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.HtmlParser;
import io.goobi.viewer.controller.StringTools;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.cms.media.CMSMediaMultiHolder;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_archive_configs")
public class CMSArchiveConfig implements CMSMediaMultiHolder, Serializable {

    private static final long serialVersionUID = -1267723820348788709L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(CMSArchiveConfig.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_archive_config_id")
    private Long id;

    @Column(name = "pi", nullable = false)
    private String pi;

    @Column(name = "date_updated", nullable = false)
    private LocalDateTime dateUpdated;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText title = new TranslatedText();

    @Column(name = "preview_text", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText previewText = new TranslatedText();

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText description = new TranslatedText();

    @JoinColumn(name = "tile_media_item_id")
    private CMSMediaItem tileImage;

    @JoinColumn(name = "header_media_item_id")
    private CMSMediaItem headerImage;

    public CMSArchiveConfig() {
    }

    /**
     * 
     * @param pi
     */
    public CMSArchiveConfig(String pi) {
        this.pi = pi;
    }

    /**
     * Cloning constructor. Must be updated after any changes to the class.
     * 
     * @param orig {@link CMSArchiveConfig} to clone
     */
    public CMSArchiveConfig(CMSArchiveConfig orig) {
        this.id = orig.id;
        this.pi = orig.pi;
        this.dateUpdated = orig.dateUpdated;
        this.title = new TranslatedText(orig.title);
        this.previewText = new TranslatedText(orig.previewText);
        this.description = new TranslatedText(orig.description);
        this.tileImage = orig.tileImage;
        this.headerImage = orig.headerImage;
    }

    /**
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * @return the pi
     */
    public String getPi() {
        return pi;
    }

    /**
     * @param pi the pi to set
     */
    public void setPi(String pi) {
        this.pi = pi;
    }

    /**
     * @return the dateUpdated
     */
    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(TranslatedText title) {
        this.title = title;
    }

    /**
     * @return the previewText
     */
    public TranslatedText getPreviewText() {
        return previewText;
    }

    /**
     * @param previewText the previewText to set
     */
    public void setPreviewText(TranslatedText previewText) {
        this.previewText = previewText;
    }

    /**
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(TranslatedText description) {
        this.description = description;
    }

    /**
     * @return the tileImage
     */
    public CMSMediaItem getTileImage() {
        return tileImage;
    }

    /**
     * @param tileImage the tileImage to set
     */
    public void setTileImage(CMSMediaItem tileImage) {
        this.tileImage = tileImage;
    }

    /**
     * @return the headerImage
     */
    public CMSMediaItem getHeaderImage() {
        return headerImage;
    }

    /**
     * @param headerImage the headerImage to set
     */
    public void setHeaderImage(CMSMediaItem headerImage) {
        this.headerImage = headerImage;
    }

    /**
     * @param maxLength maximum character length
     * @return truncated plaintext
     */
    public String getShortDescription(int maxLength) {
        if (description.isEmpty()) {
            return "";
        }
        String text = description.getTextOrDefault();
        String plain = HtmlParser.getPlaintext(text);
        return StringTools.truncateText(plain, maxLength);
    }

    @Override
    public void setMediaItem(CMSMediaItem item) {
        setMediaItem(0, item);
    }

    @Override
    public CMSMediaItem getMediaItem() {
        return getMediaItem(0);
    }

    @Override
    public String getMediaFilter() {
        return CmsMediaBean.getImageFilter();
    }

    @Override
    public String getMediaTypes() {
        return CmsMediaBean.getImageTypes();
    }

    @Override
    public boolean hasMediaItem() {
        return getMediaItem() != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper() {
        if (hasMediaItem()) {
            return new CategorizableTranslatedSelectable<>(tileImage, true,
                    tileImage.getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }

    @Override
    public void setMediaItem(int index, CMSMediaItem item) {
        switch (index) {
            case 0:
                setTileImage(item);
                break;
            case 1:
                setHeaderImage(item);
                break;
            default:
                logger.warn("Unknown item index: {}", index);
        }
    }

    @Override
    public CMSMediaItem getMediaItem(int index) {
        return switch (index) {
            case 0 -> getTileImage();
            case 1 -> getHeaderImage();
            default -> null;
        };
    }

    @Override
    public String getMediaFilter(int index) {
        return CmsMediaBean.getImageFilter();
    }

    @Override
    public String getMediaTypes(int index) {
        return CmsMediaBean.getImageTypes();
    }

    @Override
    public boolean hasMediaItem(int index) {
        return getMediaItem(index) != null;
    }

    @Override
    public CategorizableTranslatedSelectable<CMSMediaItem> getMediaItemWrapper(int index) {
        if (hasMediaItem(index)) {
            return new CategorizableTranslatedSelectable<>(getMediaItem(index), true,
                    getMediaItem(index).getFinishedLocales().stream().findFirst().orElse(BeanUtils.getLocale()), Collections.emptyList());
        }

        return null;
    }
}
