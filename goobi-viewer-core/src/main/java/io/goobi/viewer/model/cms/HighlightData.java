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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.managedbeans.CmsMediaBean;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.media.CMSMediaHolder;
import io.goobi.viewer.model.cms.media.CMSMediaItem;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "cms_highlights")
public class HighlightData implements Serializable {

    private static final long serialVersionUID = 2632497568590266830L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "highlight_id")
    private Long id;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "name", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText name = new TranslatedText();

    @Column(name = "record_identifier")
    private String recordIdentifier;

    @Column(name = "target_url")
    private String targetUrl;

    @Column(name = "date_start")
    @JsonIgnore
    private LocalDateTime dateStart;

    @Column(name = "date_end")
    @JsonIgnore
    private LocalDateTime dateEnd;

    @JoinColumn(name = "media_item_id")
    private CMSMediaItem mediaItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_mode")
    private ImageMode imageMode = ImageMode.RECORD_REPRESENTATIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type")
    private TargetType targetType = TargetType.RECORD;

    public enum TargetType {
        RECORD,
        URL;
    }

    public enum ImageMode {
        NO_IMAGE,
        UPLOADED_IMAGE,
        RECORD_REPRESENTATIVE
    }

    public HighlightData() {
        //empty
    }

    HighlightData(HighlightData source) {
        this.id = source.id;
        this.dateStart = source.dateStart;
        this.dateEnd = source.dateEnd;
        this.enabled = source.enabled;
        this.imageMode = source.imageMode;
        this.mediaItem = source.mediaItem;
        this.name = new TranslatedText(source.getName());
        this.recordIdentifier = source.recordIdentifier;
        this.targetType = source.targetType;
        this.targetUrl = source.targetUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public TranslatedText getName() {
        return name;
    }

    public void setName(TranslatedText name) {
        this.name = name;
    }

    public String getRecordIdentifier() {
        return recordIdentifier;
    }

    public void setRecordIdentifier(String recordIdentifier) {
        this.recordIdentifier = recordIdentifier;
    }

    public LocalDate getDateStart() {
        return Optional.ofNullable(dateStart).map(LocalDateTime::toLocalDate).orElse(null);
    }

    public LocalDateTime getTimeStart() {
        return dateStart;
    }

    public void setDateStart(LocalDate dateStart) {
        this.dateStart = Optional.ofNullable(dateStart).map(LocalDate::atStartOfDay).orElse(null);
    }

    public LocalDate getDateEnd() {
        return Optional.ofNullable(dateEnd).map(LocalDateTime::toLocalDate).map(date -> date.minusDays(1)).orElse(null);
    }

    public LocalDateTime getTimeEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDate dateEnd) {
        this.dateEnd = Optional.ofNullable(dateEnd).map(date -> date.plusDays(1)).map(LocalDate::atStartOfDay).orElse(null);
    }

    public CMSMediaItem getMediaItem() {
        return mediaItem;
    }

    public void setMediaItem(CMSMediaItem mediaItem) {
        this.mediaItem = mediaItem;
    }

    public ImageMode getImageMode() {
        return imageMode;
    }

    public void setImageMode(ImageMode imageMode) {
        this.imageMode = imageMode;
    }

    public TargetType getTargetType() {
        return targetType;
    }

    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
        if (TargetType.URL == this.targetType && this.imageMode == ImageMode.RECORD_REPRESENTATIVE) {
            this.imageMode = ImageMode.UPLOADED_IMAGE;
        }
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    @Override
    public int hashCode() {
        return recordIdentifier == null ? 0 : recordIdentifier.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj != null && obj.getClass().equals(this.getClass())) {
            HighlightData other = (HighlightData) obj;
            return Objects.equals(this.id, other.id)
                    && Objects.equals(this.recordIdentifier, other.recordIdentifier)
                    && Objects.equals(this.dateStart, other.dateStart)
                    && Objects.equals(this.dateEnd, other.dateEnd)
                    && Objects.equals(this.enabled, other.enabled)
                    && Objects.equals(this.imageMode, other.imageMode)
                    && Objects.equals(this.mediaItem, other.mediaItem)
                    && Objects.equals(this.name, other.name);
        }

        return false;
    }

    @Override
    public String toString() {
        return getName().getTextOrDefault() + " (ID: " + getId() + ")";
    }

}
