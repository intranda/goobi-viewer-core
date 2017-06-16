/*************************************************************************
 *
 * Copyright intranda GmbH
 *
 * ************************* CONFIDENTIAL ********************************
 *
 * [2003] - [2016] intranda GmbH, Bertha-von-Suttner-Str. 9, 37085 Göttingen, Germany
 *
 * All Rights Reserved.
 *
 * NOTICE: All information contained herein is protected by copyright.
 * The source code contained herein is proprietary of intranda GmbH.
 * The dissemination, reproduction, distribution or modification of
 * this source code, without prior written permission from intranda GmbH,
 * is expressly forbidden and a violation of international copyright law.
 *
 *************************************************************************/
package de.intranda.digiverso.presentation.exceptions;

import java.io.Serializable;

public class ModuleMissingException extends Exception implements Serializable {

    private static final long serialVersionUID = 6421734792359424553L;

    /**
     * @param string {@link String}
     */
    public ModuleMissingException(String string) {
        super(string);
    }
}
