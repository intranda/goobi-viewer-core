package io.goobi.viewer.model.cms.widgets;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.model.cms.widgets.type.CustomWidgetType;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;

/**
 * Class to persist user generated CMS-Sidebar widgets in the database
 * 
 * @author florian
 *
 */
@Entity
@Table(name = "custom_sidebar_widgets")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "widget_type")
@DiscriminatorValue("CustomSidebarWidget")
public class CustomSidebarWidget implements IPolyglott {

    private static final Logger logger = LoggerFactory.getLogger(CustomSidebarWidget.class);

    /** Unique database ID. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "widget_id")
    protected Long id;

    @Column(name = "widget_title", columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText title = new TranslatedText(IPolyglott.getLocalesStatic());

    @Column(name = "widget_description", columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText description = new TranslatedText(IPolyglott.getLocalesStatic());

    @Column(name = "style_class", nullable = true)
    private String styleClass = "";
    
    @Column(name = "collapsed")
    private boolean collapsed = false;
    
    @Transient
    private Locale locale = IPolyglott.getCurrentLocale();

    public CustomSidebarWidget() {
    }

    public CustomSidebarWidget(CustomSidebarWidget source) {
        this.id = source.id;
        this.title = new TranslatedText(source.title);
        this.description = new TranslatedText(source.description);
    }

    @Override
    public boolean isComplete(Locale locale) {
        return title.isComplete(locale);
    }

    @Override
    public boolean isValid(Locale locale) {
        return title.isComplete(locale);
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return title.isEmpty(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.locale;
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.locale = locale;
        this.description.setSelectedLocale(locale);
        this.title.setSelectedLocale(locale);
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
     * @return the title
     */
    public TranslatedText getTitle() {
        return title;
    }

    /**
     * @return the description
     */
    public TranslatedText getDescription() {
        return description;
    }

    public CustomWidgetType getType() {
        return null;
    }

    
    public String getStyleClass() {
        return styleClass;
    }

    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
    }
    
    /**
     * @return the collapsed
     */
    public boolean isCollapsed() {
        return collapsed;
    }

    /**
     * @param collapsed the collapsed to set
     */
    public void setCollapsed(boolean collapsed) {
        this.collapsed = collapsed;
    }

    public static CustomSidebarWidget clone(CustomSidebarWidget o) {
        if (o == null) {
            return null;
        } else {
            try {
                return o.getClass().getConstructor(o.getClass()).newInstance(o);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                    | SecurityException e) {
                logger.error("Cannot instatiate CustomSidebarWidget of class {}. Reason: {}", o.getClass(), e.toString());
                return null;
            }
        }
    }

}
