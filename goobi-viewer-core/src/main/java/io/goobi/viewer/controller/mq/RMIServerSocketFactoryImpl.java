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
package io.goobi.viewer.controller.mq;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.rmi.server.RMIServerSocketFactory;

import javax.net.ServerSocketFactory;

/**
 * taken from: https://vafer.org/blog/20061010091658/
 */

public class RMIServerSocketFactoryImpl implements RMIServerSocketFactory {

    private final InetAddress localAddress;

    public RMIServerSocketFactoryImpl(final InetAddress pAddress) {
        localAddress = pAddress;
    }

    @Override
    public ServerSocket createServerSocket(final int pPort) throws IOException {
        return ServerSocketFactory.getDefault()
                .createServerSocket(pPort, 0, localAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        return obj.getClass().equals(getClass());
    }

    @Override
    public int hashCode() {
        return RMIServerSocketFactoryImpl.class.hashCode();
    }
}
