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

import jakarta.annotation.PreDestroy;
import jakarta.inject.Named;

import org.omnifaces.cdi.ViewScoped;

import io.goobi.viewer.controller.Procedure;

/**
 * View scoped bean to detect leaving a view and perform some action. The action may take no arguments and procudes not outcome
 * 
 * @author florian
 *
 */
@Named
@ViewScoped
public class ViewWatcher implements Serializable {

    private static final long serialVersionUID = 1488705096792424026L;

    /**
     * The {@link Procedure} to invoke when leaving the view
     */
    private transient Optional<Procedure> onLeavePage = Optional.empty();

    /**
     * Pass a {@link Procedure} which is to be invoced when leaving the page, more specificall on @PreDestroy of this bean
     * 
     * @param onLeavePage
     */
    public void onLeavePage(Procedure onLeavePage) {
        this.onLeavePage = Optional.ofNullable(onLeavePage);
    }

    /**
     * Method invoked when leaving the page. Executed the {@link Procedure} passed to {@link #onLeavePage}
     */
    @PreDestroy
    public void onLeavePage() {
        this.onLeavePage.ifPresent(Procedure::execute);
    }

}
