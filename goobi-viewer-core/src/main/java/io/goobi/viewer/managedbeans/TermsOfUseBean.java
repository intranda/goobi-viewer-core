package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Optional;

import javax.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.administration.legal.TermsOfUse;

/**
 * 
 * Keeps the global termsOfUse object for the current session
 * 
 * @author florian
 *
 */
@Named("termsOfUseBean")
@SessionScoped
public class TermsOfUseBean implements Serializable {

    private static final long serialVersionUID = 5425114972697440546L;
    private static final Logger logger = LoggerFactory.getLogger(TermsOfUseEditBean.class);

    private final Optional<TermsOfUse> termsOfUse = getTermsOfUseIfActiveAndAccessible();

    private static Optional<TermsOfUse> getTermsOfUseIfActiveAndAccessible() {
        try {
            TermsOfUse tou = DataManager.getInstance().getDao().getTermsOfUse();
            return Optional.ofNullable(tou).filter(t -> tou.getId() != null);
        } catch (DAOException e) {
            logger.error("Error getting terms of use object: '{}'", e.toString());
            return Optional.empty();
        }
    }

    public boolean isTermsOfUseActive() {
        return termsOfUse.map(TermsOfUse::isActive).orElse(false);
    }

    public String getTitle() {
        return this.termsOfUse.map(t -> t.getTitleIfExists(BeanUtils.getLocale().getLanguage())
                    .orElse(t.getTitleIfExists(BeanUtils.getDefaultLocale().getLanguage())
                        .orElse("")))
                .orElse("");
    }

    public String getDescription() {
        return this.termsOfUse.map(t -> t.getDescriptionIfExists(BeanUtils.getLocale().getLanguage())
                    .orElse(t.getDescriptionIfExists(BeanUtils.getDefaultLocale().getLanguage())
                        .orElse("")))
                .orElse("");
    }

}
