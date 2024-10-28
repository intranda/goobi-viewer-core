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
package io.goobi.viewer;

import jakarta.servlet.annotation.WebListener;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.managedbeans.AdminBean;
import io.goobi.viewer.managedbeans.AdminConfigEditorBean;

/**
 * <p>
 * SessionListener class.
 * </p>
 */
@WebListener
public class SessionListener implements HttpSessionListener {

    private static final Logger logger = LogManager.getLogger(SessionListener.class);

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpSessionListener#sessionCreated(jakarta.servlet.http.HttpSessionEvent)
     */
    /** {@inheritDoc} */
    @Override
    public void sessionCreated(HttpSessionEvent event) {
        //        if (DataManager.getInstance().getSessionMap().put(event.getSession().getId(), new HashMap<>()) == null) {
        //            logger.trace("Session created: {}", event.getSession().getId()); //NOSONAR Debug
        //        }
    }

    /* (non-Javadoc)
     * @see jakarta.servlet.http.HttpSessionListener#sessionDestroyed(jakarta.servlet.http.HttpSessionEvent)
     */
    /** {@inheritDoc} */
    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        if (DataManager.getInstance().getSessionMap().remove(event.getSession().getId()) != null) {
            String sessionId = event.getSession().getId();
            // logger.trace("Session destroyed: {}", sessionId); //NOSONAR Debug
            DataManager.getInstance().getRecordLockManager().removeLocksForSessionId(sessionId, null);
            if (sessionId.equals(AdminBean.getTranslationGroupsEditorSession())) {
                AdminBean.setTranslationGroupsEditorSession(null);
            }
            // Release file edit locks
            AdminConfigEditorBean.clearLocksForSessionId(sessionId);
        }
    }
}
