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
package io.goobi.viewer.model.job.upload;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Response returned by the Goobi workflow REST API after a process-creation request, indicating
 * success or failure and providing the newly created process name and ID on success.
 */
@XmlRootElement
public class ProcessCreationResponse {

    private String result; // success, error

    private String errorText;

    private String processName;

    private int processId;

    
    public String getResult() {
        return result;
    }

    
    public void setResult(String result) {
        this.result = result;
    }

    
    public String getErrorText() {
        return errorText;
    }

    
    public void setErrorText(String errorText) {
        this.errorText = errorText;
    }

    
    public String getProcessName() {
        return processName;
    }

    
    public void setProcessName(String processName) {
        this.processName = processName;
    }

    
    public int getProcessId() {
        return processId;
    }

    
    public void setProcessId(int processId) {
        this.processId = processId;
    }
}
