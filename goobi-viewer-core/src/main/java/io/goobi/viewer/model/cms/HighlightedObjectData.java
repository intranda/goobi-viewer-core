package io.goobi.viewer.model.cms;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Locale;

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
@Table(name = "cms_sliders")
public class HighlightedObjectData implements Serializable {

    private static final long serialVersionUID = 2632497568590266830L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slider_id")
    private Long id;

    @Column(name = "enabled")
    private boolean enabled = true;

    @Column(name = "name", nullable = true, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText name = new TranslatedText();

    @Column(name = "record_identifier")
    private String recordIdentifier;

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
    private ImageMode imageMode;

    public enum ImageMode {
        NO_IMAGE,
        UPLOADED_IMAGE,
        RECORD_REPRESENTATIVE
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

    public LocalDateTime getDateStart() {
        return dateStart;
    }

    public void setDateStart(LocalDateTime dateStart) {
        this.dateStart = dateStart;
    }

    public LocalDateTime getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(LocalDateTime dateEnd) {
        this.dateEnd = dateEnd;
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

}
