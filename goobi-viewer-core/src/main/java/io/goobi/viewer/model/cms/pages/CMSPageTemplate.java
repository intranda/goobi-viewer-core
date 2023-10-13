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
package io.goobi.viewer.model.cms.pages;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.persistence.annotations.CascadeOnDelete;
import org.eclipse.persistence.annotations.PrivateOwned;

import de.intranda.metadata.multilanguage.IMetadataValue;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.PrettyUrlTools;
import io.goobi.viewer.dao.converter.TranslatedTextConverter;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.cms.CMSCategory;
import io.goobi.viewer.model.cms.Selectable;
import io.goobi.viewer.model.cms.itemfunctionality.SearchFunctionality;
import io.goobi.viewer.model.cms.pages.content.CMSComponent;
import io.goobi.viewer.model.cms.pages.content.CMSContentItem;
import io.goobi.viewer.model.cms.pages.content.PersistentCMSComponent;
import io.goobi.viewer.model.cms.pages.content.TranslatableCMSContent;
import io.goobi.viewer.model.cms.pages.content.types.CMSSearchContent;
import io.goobi.viewer.model.cms.widgets.CustomSidebarWidget;
import io.goobi.viewer.model.cms.widgets.WidgetDisplayElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElement;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementAutomatic;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementCustom;
import io.goobi.viewer.model.cms.widgets.embed.CMSSidebarElementDefault;
import io.goobi.viewer.model.maps.GeoMap;
import io.goobi.viewer.model.translations.IPolyglott;
import io.goobi.viewer.model.translations.TranslatedText;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * <p>
 * Template to create a {@link CMSPage}. Contains some general information about the template as well as a list of {@link CMSComponent components} and
 * {@link CMSSidebarElement sidebar widgets} to be included in the page
 * </p>
 */
@Entity
@Table(name = "cms_page_templates")
public class CMSPageTemplate implements Comparable<CMSPageTemplate>, IPolyglott, Serializable {

    private static final long serialVersionUID = 9175944585243255552L;

    /** Logger for this class. */
    private static final Logger logger = LogManager.getLogger(CMSPageTemplate.class);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cms_page_template_id")
    private Long id;

    @Column(name = "date_created", nullable = false)
    private LocalDateTime dateCreated;

    @Column(name = "date_updated")
    private LocalDateTime dateUpdated;

    /**
     * Set to true to disallow users to change {@link CMSComponent}s contained in a page created from this template. The content of those components
     * may still be edited
     */
    @Column(name = "lock_components")
    private boolean lockComponents = false;

    @Column(name = "publication_status", nullable = false)
    @Enumerated(EnumType.STRING)
    private PublicationStatus publicationStatus = PublicationStatus.PRIVATE;

    @Column(name = "use_default_sidebar", nullable = false)
    private boolean useDefaultSidebar = false;

    @Column(name = "subtheme_discriminator", nullable = true)
    private String subThemeDiscriminatorValue = "";

    @OneToMany(mappedBy = "ownerTemplate", fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
    @OrderBy("order")
    @PrivateOwned
    private List<CMSSidebarElement> sidebarElements = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "cms_template_cms_categories", joinColumns = @JoinColumn(name = "template_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    private List<CMSCategory> categories = new ArrayList<>();

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText title = new TranslatedText();

    @Column(name = "description", nullable = false, columnDefinition = "MEDIUMTEXT")
    @Convert(converter = TranslatedTextConverter.class)
    private TranslatedText description = new TranslatedText();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "owningTemplate")
    @PrivateOwned
    @CascadeOnDelete
    private List<PersistentCMSComponent> persistentComponents = new ArrayList<>();

    @Column(name = "lecacy_template")
    private boolean legacyTemplate = false;

    /**
     * A html class name to be applied to the DOM element containing the page html
     */
    @Column(name = "wrapper_element_class")
    private String wrapperElementClass = "";

    @Transient
    private String sidebarElementString = null;

    @Transient
    private List<Selectable<CMSCategory>> selectableCategories = null;

    @Transient
    private Locale selectedLocale = IPolyglott.getCurrentLocale();

    @Transient
    private List<CMSComponent> cmsComponents = new ArrayList<>();

    /**
     * <p>
     * Constructor for CMSPage.
     * </p>
     */
    public CMSPageTemplate() {
        this.dateCreated = LocalDateTime.now();
    }

    /**
     * creates a deep copy of the original CMSPage. Only copies persisted properties and performs initialization for them
     *
     * @param original a {@link io.goobi.viewer.model.cms.pages.CMSPageTemplate} object.
     */
    public CMSPageTemplate(CMSPageTemplate original) {
        if (original.id != null) {
            this.id = original.id;
        }
        this.title = original.title;
        this.description = original.description;
        this.dateCreated = original.dateCreated;
        this.dateUpdated = original.dateUpdated;
        this.publicationStatus = original.publicationStatus;
        this.useDefaultSidebar = original.useDefaultSidebar;
        this.subThemeDiscriminatorValue = original.subThemeDiscriminatorValue;
        this.categories = new ArrayList<>(original.categories);
        this.wrapperElementClass = original.wrapperElementClass;
        this.lockComponents = original.lockComponents;
        this.legacyTemplate = original.legacyTemplate;

        if (original.sidebarElements != null) {
            this.sidebarElements = new ArrayList<>(original.sidebarElements.size());
            for (CMSSidebarElement sidebarElement : original.sidebarElements) {
                CMSSidebarElement copy = CMSSidebarElement.copy(sidebarElement, this);
                this.sidebarElements.add(copy);
            }
        }

        for (PersistentCMSComponent component : original.getPersistentComponents()) {
            PersistentCMSComponent copy = new PersistentCMSComponent(component);
            copy.setOwningTemplate(this);
            this.persistentComponents.add(copy);
        }
    }

    public CMSPageTemplate(CMSPage original) {
        this.title = new TranslatedText(original.getTitleTranslations());
        this.dateCreated = LocalDateTime.now();
        this.dateUpdated = LocalDateTime.now();
        this.useDefaultSidebar = original.isUseDefaultSidebar();
        this.subThemeDiscriminatorValue = original.getSubThemeDiscriminatorValue();
        this.categories = new ArrayList<>(original.getCategories());
        this.wrapperElementClass = original.getWrapperElementClass();

        if (original.getSidebarElements() != null) {
            this.sidebarElements = new ArrayList<>(original.getSidebarElements().size());
            for (CMSSidebarElement sidebarElement : original.getSidebarElements()) {
                CMSSidebarElement copy = CMSSidebarElement.copy(sidebarElement, this);
                this.sidebarElements.add(copy);
            }
        }

        for (PersistentCMSComponent component : original.getPersistentComponents()) {
            PersistentCMSComponent copy = new PersistentCMSComponent(component);
            copy.setOwningTemplate(this);
            this.persistentComponents.add(copy);
        }
    }

    public void initialiseCMSComponents(CMSTemplateManager templateManager) {
        this.cmsComponents = new ArrayList<>();
        for (PersistentCMSComponent persistentComponent : persistentComponents) {
            CMSComponent comp = templateManager
                    .getComponent(persistentComponent.getTemplateFilename())
                    .map(c -> new CMSComponent(c, Optional.of(persistentComponent)))
                    .orElse(null);
            if (comp != null) {
                this.cmsComponents.add(comp);
            }
        }
        sortComponents();
    }

    private void sortComponents() {
        Collections.sort(this.cmsComponents);
        for (int i = 0; i < this.cmsComponents.size(); i++) {
            this.cmsComponents.get(i).setOrder(i + 1);
        }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (getClass() != obj.getClass()) {
            return false;
        }
        CMSPageTemplate other = (CMSPageTemplate) obj;
        if (id == null) {
            if (other.id != null) {
                return false;
            }
            return super.equals(obj);
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    /** {@inheritDoc} */
    @Override
    public int compareTo(CMSPageTemplate o) {
        if (o == null || o.getId() == null) {
            return -1;
        }
        if (id == null) {
            return 1;
        }

        return id.compareTo(o.getId());
    }

    /**
     * <p>
     * addSidebarElement.
     * </p>
     *
     * @param element a {@link io.goobi.viewer.model.cms.widgets.CMSSidebarElement} object.
     */
    public void addSidebarElement(CMSSidebarElement element) {
        if (element != null) {
            sidebarElements.add(element);
        }
    }

    /**
     * <p>
     * Getter for the field <code>id</code>.
     * </p>
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * <p>
     * Setter for the field <code>id</code>.
     * </p>
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * <p>
     * Getter for the field <code>dateCreated</code>.
     * </p>
     *
     * @return the dateCreated
     */
    public LocalDateTime getDateCreated() {
        return dateCreated;
    }

    /**
     * <p>
     * Setter for the field <code>dateCreated</code>.
     * </p>
     *
     * @param dateCreated the dateCreated to set
     */
    public void setDateCreated(LocalDateTime dateCreated) {
        this.dateCreated = dateCreated;
    }

    public LocalDateTime getDateUpdated() {
        return dateUpdated;
    }

    /**
     * <p>
     * Setter for the field <code>dateUpdated</code>.
     * </p>
     *
     * @param dateUpdated the dateUpdated to set
     */
    public void setDateUpdated(LocalDateTime dateUpdated) {
        this.dateUpdated = dateUpdated;
    }

    /**
     * <p>
     * isPublished.
     * </p>
     *
     * @return the published
     */
    public boolean isPublished() {
        return PublicationStatus.PUBLISHED.equals(this.publicationStatus);
    }

    /**
     * <p>
     * Setter for the field <code>published</code>.
     * </p>
     *
     * @param published the published to set
     */
    public void setPublished(boolean published) {
        this.publicationStatus = published ? PublicationStatus.PUBLISHED : PublicationStatus.PRIVATE;
    }

    /**
     * <p>
     * isUseDefaultSidebar.
     * </p>
     *
     * @return the useDefaultSidebar
     */
    public boolean isUseDefaultSidebar() {
        return useDefaultSidebar;
    }

    /**
     * <p>
     * Setter for the field <code>useDefaultSidebar</code>.
     * </p>
     *
     * @param useDefaultSidebar the useDefaultSidebar to set
     */
    public void setUseDefaultSidebar(boolean useDefaultSidebar) {
        this.useDefaultSidebar = useDefaultSidebar;
    }

    /**
     * <p>
     * Getter for the field <code>sidebarElements</code>.
     * </p>
     *
     * @return the sidebarElements
     */
    public List<CMSSidebarElement> getSidebarElements() {
        return sidebarElements;
    }

    /**
     * <p>
     * Setter for the field <code>sidebarElements</code>.
     * </p>
     *
     * @param sidebarElements the sidebarElements to set
     */
    public void setSidebarElements(List<CMSSidebarElement> sidebarElements) {
        this.sidebarElements = sidebarElements;

    }

    public void addToSidebar(List<WidgetDisplayElement> widgets) {
        for (WidgetDisplayElement displayWidget : widgets) {
            CMSSidebarElement element = null;
            switch (displayWidget.getGenerationType()) {
                case DEFAULT:
                    element = new CMSSidebarElementDefault(displayWidget.getContentType(), this);
                    break;
                case AUTOMATIC:
                    try {
                        GeoMap map = DataManager.getInstance().getDao().getGeoMap(displayWidget.getId());
                        element = new CMSSidebarElementAutomatic(map, this);
                    } catch (DAOException e) {
                        logger.error("Unable to add widget: Cannot load geomap id={}", displayWidget.getId());
                    }
                    break;
                case CUSTOM:
                    try {
                        CustomSidebarWidget widget = DataManager.getInstance().getDao().getCustomWidget(displayWidget.getId());
                        element = new CMSSidebarElementCustom(widget, this);
                    } catch (DAOException e) {
                        logger.error("Unable to add widget: Cannot load geomap id={}", displayWidget.getId());
                    }
                    break;
            }
            if (element != null) {
                this.sidebarElements.add(element);
            }
        }
    }

    public void moveUpSidebarElement(CMSSidebarElement element) {
        int currentIndex = this.sidebarElements.indexOf(element);
        if (currentIndex > 0) {
            int newIndex = currentIndex - 1;
            this.sidebarElements.remove(currentIndex);
            this.sidebarElements.add(newIndex, element);
        }
    }

    public void moveDownSidebarElement(CMSSidebarElement element) {
        int currentIndex = this.sidebarElements.indexOf(element);
        if (currentIndex > -1 && currentIndex < this.sidebarElements.size() - 1) {
            int newIndex = currentIndex + 1;
            this.sidebarElements.remove(currentIndex);
            this.sidebarElements.add(newIndex, element);
        }
    }

    public void removeSidebarElement(CMSSidebarElement element) {
        this.sidebarElements.remove(element);
    }

    public boolean containsSidebarElement(WidgetDisplayElement widget) {
        switch (widget.getGenerationType()) {
            case DEFAULT:
                return this.sidebarElements.stream().anyMatch(ele -> ele.getContentType().equals(widget.getContentType()));
            case AUTOMATIC:
                return this.sidebarElements.stream()
                        .anyMatch(ele -> ele.getContentType().equals(widget.getContentType())
                                && Objects.equals(((CMSSidebarElementAutomatic) ele).getMap().getId(), widget.getId()));
            case CUSTOM:
                return this.sidebarElements.stream()
                        .anyMatch(ele -> ele.getContentType().equals(widget.getContentType())
                                && Objects.equals(((CMSSidebarElementCustom) ele).getWidget().getId(), widget.getId()));
            default:
                return false;
        }

    }

    /**
     * <p>
     * Getter for the field <code>categories</code>.
     * </p>
     *
     * @return the classifications
     */
    public List<CMSCategory> getCategories() {
        return categories;
    }

    /**
     * <p>
     * Setter for the field <code>categories</code>.
     * </p>
     *
     * @param categories a {@link java.util.List} object.
     */
    public void setCategories(List<CMSCategory> categories) {
        this.categories = categories;
    }

    /**
     * <p>
     * addCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     */
    public void addCategory(CMSCategory category) {
        if (category != null && !categories.contains(category)) {
            categories.add(category);
        }
    }

    /**
     * <p>
     * removeCategory.
     * </p>
     *
     * @param category a {@link io.goobi.viewer.model.cms.CMSCategory} object.
     */
    public void removeCategory(CMSCategory category) {
        categories.remove(category);
    }

    /**
     * <p>
     * Getter for the field <code>sidebarElementString</code>.
     * </p>
     *
     * @return the sidebarElementString
     */
    public String getSidebarElementString() {
        return sidebarElementString;
    }

    /**
     * <p>
     * Setter for the field <code>sidebarElementString</code>.
     * </p>
     *
     * @param sidebarElementString the sidebarElementString to set
     */
    public void setSidebarElementString(String sidebarElementString) {
        logger.trace("setSidebarElementString: {}", sidebarElementString);
        this.sidebarElementString = sidebarElementString;
    }

    /**
     * <p>
     * isLanguageComplete.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a boolean.
     */
    public boolean isLanguageComplete(Locale locale) {
        if (!this.title.isComplete(locale)) {
            return false;
        }

        for (PersistentCMSComponent component : persistentComponents) {
            if (!component.isComplete(locale)) {
                return false;
            }
        }

        return true;
    }

    /**
     * <p>
     * getTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getTitle() {
        return this.title.getTextOrDefault();
    }

    /**
     * <p>
     * getTitle.
     * </p>
     *
     * @param locale a {@link java.util.Locale} object.
     * @return a {@link java.lang.String} object.
     */
    public String getTitle(Locale locale) {
        return this.title.getText(locale);
    }

    public IMetadataValue getTitleTranslations() {
        return this.title;
    }

    public TranslatedText getDescription() {
        return description;
    }

    public void setDescription(TranslatedText description) {
        this.description = description;
    }

    /**
     * <p>
     * Getter for the field <code>subThemeDiscriminatorValue</code>.
     * </p>
     *
     * @return the subThemeDiscriminatorValue
     */
    public String getSubThemeDiscriminatorValue() {
        return subThemeDiscriminatorValue;
    }

    /**
     * <p>
     * Setter for the field <code>subThemeDiscriminatorValue</code>.
     * </p>
     *
     * @param subThemeDiscriminatorValue the subThemeDiscriminatorValue to set
     */
    public void setSubThemeDiscriminatorValue(String subThemeDiscriminatorValue) {
        this.subThemeDiscriminatorValue = subThemeDiscriminatorValue == null ? "" : subThemeDiscriminatorValue;
    }

    /**
     * <p>
     * isHasSidebarElements.
     * </p>
     *
     * @return a boolean.
     */
    public boolean isHasSidebarElements() {
        if (!isUseDefaultSidebar()) {
            return getSidebarElements() != null && !getSidebarElements().isEmpty();
        }
        return true;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    /** {@inheritDoc} */
    @Override
    public String toString() {
        String t = this.getTitle();
        if (StringUtils.isBlank(t)) {
            return "ID: " + this.getId() + " (no title)";

        }
        return t;
    }

    /**
     * <p>
     * Getter for the field <code>wrapperElementClass</code>.
     * </p>
     *
     * @return the {@link #wrapperElementClass}
     */
    public String getWrapperElementClass() {
        return wrapperElementClass;
    }

    /**
     * <p>
     * Setter for the field <code>wrapperElementClass</code>.
     * </p>
     *
     * @param wrapperElementClass the {@link #wrapperElementClass} to set
     */
    public void setWrapperElementClass(String wrapperElementClass) {
        this.wrapperElementClass = wrapperElementClass;
    }

    /**
     * Retrieve all categories fresh from the DAO and write them to this depending on the state of the selectableCategories list. Saving the
     * categories from selectableCategories directly leads to ConcurrentModificationexception when persisting page
     */
    public void writeSelectableCategories() {
        if (selectableCategories == null) {
            return;
        }

        try {
            List<CMSCategory> allCats = DataManager.getInstance().getDao().getAllCategories();
            List<CMSCategory> tempCats = new ArrayList<>();
            for (CMSCategory cat : allCats) {
                if ((this.categories.contains(cat) && selectableCategories.stream().noneMatch(s -> s.getValue().equals(cat)))
                        || selectableCategories.stream().anyMatch(s -> s.getValue().equals(cat) && s.isSelected())) {
                    tempCats.add(cat);
                }
            }
            this.categories = tempCats;
        } catch (DAOException e) {
            logger.error(e.toString(), e);
        }
    }

    /**
     * <p>
     * Getter for the field <code>selectableCategories</code>.
     * </p>
     *
     * @return the selectableCategories
     * @throws io.goobi.viewer.exceptions.DAOException if any.
     */
    public List<Selectable<CMSCategory>> getSelectableCategories() throws DAOException {
        if (selectableCategories == null) {
            List<CMSCategory> allowedCategories = BeanUtils.getCmsBean().getAllowedCategories(BeanUtils.getUserBean().getUser());
            selectableCategories =
                    allowedCategories.stream().map(cat -> new Selectable<>(cat, this.categories.contains(cat))).collect(Collectors.toList());
        }
        return selectableCategories;
    }

    public void resetSelectableCategories() {
        this.selectableCategories = null;
    }

    public String getAdminBackendUrl() {
        String prettyId = "adminCmsEditPage";
        return PrettyUrlTools.getAbsolutePageUrl(prettyId, this.getId());
    }

    public List<PersistentCMSComponent> getPersistentComponents() {
        return persistentComponents;
    }

    public List<CMSComponent> getComponents() {
        if (this.cmsComponents.size() != this.persistentComponents.size()) {
            logger.error("CMSComponents not initialized. Call initialiseCMSComponents to do so");
        }
        return this.cmsComponents;
    }

    public CMSComponent getAsCMSComponent(PersistentCMSComponent p) {
        return this.cmsComponents.stream()
                .filter(c -> c.getPersistentComponent() == p)
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("Component " + p.getId() + " is not registered in page"));
    }

    public boolean removeComponent(PersistentCMSComponent component) {
        this.cmsComponents.remove(getAsCMSComponent(component));
        return this.persistentComponents.remove(component);
    }

    public boolean removeComponent(CMSComponent component) {
        this.persistentComponents.remove(component.getPersistentComponent());
        boolean success = this.cmsComponents.remove(component);
        if (success) {
            sortComponents();
        }
        return success;
    }

    public PersistentCMSComponent addComponent(CMSComponent template) {
        PersistentCMSComponent persistentComponent = new PersistentCMSComponent(template);
        persistentComponent.setOrder(getHighestComponentOrder() + 1);
        persistentComponent.setOwningTemplate(this);
        this.persistentComponents.add(persistentComponent);
        CMSComponent cmsComponent = new CMSComponent(template, Optional.of(persistentComponent));
        this.cmsComponents.add(cmsComponent);
        this.sortComponents();
        return persistentComponent;
    }

    private int getHighestComponentOrder() {
        return this.persistentComponents.stream()
                .mapToInt(PersistentCMSComponent::getOrder)
                .max()
                .orElse(0);
    }

    @Override
    public boolean isComplete(Locale locale) {
        Locale defaultLocale = IPolyglott.getDefaultLocale();
        return this.title.isComplete(locale, defaultLocale, true) &&
                this.cmsComponents.stream()
                        .flatMap(comp -> comp.getTranslatableContentItems().stream())
                        .allMatch(content -> ((TranslatableCMSContent) content.getContent()).getText()
                                .isComplete(locale, defaultLocale, content.isRequired()));
    }

    @Override
    public boolean isValid(Locale locale) {
        return this.title.isValid(locale) &&
                this.cmsComponents.stream()
                        .flatMap(comp -> comp.getTranslatableContentItems().stream())
                        .filter(CMSContentItem::isRequired)
                        .allMatch(content -> ((TranslatableCMSContent) content.getContent()).getText().isValid(locale));

    }

    public boolean hasSearchFunctionality() {
        return this.persistentComponents.stream()
                .flatMap(c -> c.getContentItems().stream())
                .anyMatch(CMSSearchContent.class::isInstance);
    }

    public Optional<SearchFunctionality> getSearch() {
        return this.persistentComponents.stream()
                .flatMap(c -> c.getContentItems().stream())
                .filter(CMSSearchContent.class::isInstance)
                .map(content -> ((CMSSearchContent) content).getSearch())
                .findAny();
    }

    /**
     * Set the order attribute of the {@link PersistentCMSComponent} belonging to the given {@link CMSComponent} to the given order value. Also, sets
     * the order value of all Components which previously had the given order to the order value of the given component
     * 
     * @param component
     * @param order
     * @return
     */
    public void setComponentOrder(CMSComponent component, int order) {
        PersistentCMSComponent persistentComponent = component.getPersistentComponent();
        Integer currentOrder = persistentComponent.getOrder();
        this.getComponents()
                .stream()
                .filter(c -> Integer.compare(c.getOrder(), order) == 0)
                .forEach(comp -> comp.setOrder(currentOrder));
        persistentComponent.setOrder(order);
        Collections.sort(this.cmsComponents);
    }

    public void incrementOrder(CMSComponent component) {
        this.setComponentOrder(component, component.getOrder() + 1);
    }

    public void decrementOrder(CMSComponent component) {
        this.setComponentOrder(component, component.getOrder() - 1);
    }

    public boolean isFirstComponent(CMSComponent component) {
        return this.cmsComponents.indexOf(component) == 0;
    }

    public boolean isLastComponent(CMSComponent component) {
        return this.cmsComponents.indexOf(component) == this.cmsComponents.size() - 1;
    }

    @Override
    public boolean isEmpty(Locale locale) {
        return !isValid(locale);
    }

    @Override
    public Locale getSelectedLocale() {
        return this.title.getSelectedLocale();
    }

    @Override
    public void setSelectedLocale(Locale locale) {
        this.title.setSelectedLocale(locale);
        this.description.setSelectedLocale(locale);
        this.persistentComponents.forEach(comp -> comp.setSelectedLocale(locale));
    }

    public boolean isLockComponents() {
        return lockComponents;
    }

    public void setLockComponents(boolean lockComponents) {
        this.lockComponents = lockComponents;
    }

    public boolean isLegacyTemplate() {
        return this.legacyTemplate;
    }

    public void setLegacyTemplate(boolean legacyTemplate) {
        this.legacyTemplate = legacyTemplate;
    }

    public PersistentCMSComponent addComponent(String filename, CMSTemplateManager templateManager)
            throws IllegalArgumentException, IllegalStateException {
        if (templateManager == null) {
            throw new IllegalArgumentException("template manager may not be null");
        }
        return addComponent(templateManager
                .getComponent(filename)
                .orElseThrow(() -> new IllegalArgumentException("No component configured with filename " + filename)));
    }

    public boolean isContainsPagedComponents() {
        return this.persistentComponents.stream().anyMatch(PersistentCMSComponent::isPaged);
    }

    public String getName() {
        return this.title.getTextOrDefault();
    }

    public void setTitleTranslations(TranslatedText title) {
        this.title = title;
    }

}
