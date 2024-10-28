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
package io.goobi.viewer.managedbeans;

import java.io.Serializable;
import java.util.Optional;

import jakarta.enterprise.context.SessionScoped;
import javax.inject.Named;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.exceptions.DAOException;
import io.goobi.viewer.managedbeans.utils.BeanUtils;
import io.goobi.viewer.model.administration.legal.TermsOfUse;

/**
 *
 * Keeps the global termsOfUse object for the current session
 *
 * @author florian
 */
@Named("termsOfUseBean")
@SessionScoped
public class TermsOfUseBean implements Serializable {

    private static final long serialVersionUID = 5425114972697440546L;
    private static final Logger logger = LogManager.getLogger(TermsOfUseBean.class);

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

    /**
     * <p>
     * isTermsOfUseActive.
     * </p>
     *
     * @return a boolean
     */
    public boolean isTermsOfUseActive() {
        return termsOfUse.map(TermsOfUse::isActive).orElse(false);
    }

    /**
     * <p>
     * getTitle.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getTitle() {
        return this.termsOfUse.map(t -> t.getTitleIfExists(BeanUtils.getLocale().getLanguage())
                .orElse(t.getTitleIfExists(BeanUtils.getDefaultLocale().getLanguage())
                        .orElse("")))
                .orElse("");
    }

    /**
     * <p>
     * getDescription.
     * </p>
     *
     * @return a {@link java.lang.String} object
     */
    public String getDescription() {
        return this.termsOfUse.map(t -> t.getDescriptionIfExists(BeanUtils.getLocale().getLanguage())
                .orElse(t.getDescriptionIfExists(BeanUtils.getDefaultLocale().getLanguage())
                        .orElse("")))
                .orElse("");
    }

}
