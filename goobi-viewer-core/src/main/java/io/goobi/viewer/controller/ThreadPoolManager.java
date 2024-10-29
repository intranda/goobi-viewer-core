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
package io.goobi.viewer.controller;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.poi.ss.formula.functions.T;

/**
 * @deprecated apparently shut down but never used
 */
@Deprecated(since = "24.10")
public class ThreadPoolManager {

    private final ExecutorService executorService;
    private final Object lock = new Object();

    public ThreadPoolManager(int size) {
        this.executorService = Executors.newFixedThreadPool(size);
    }

    public Future<T> execute(Callable<T> task) {
        synchronized (lock) {
            return this.executorService.submit(task);
        }
    }

    public Future<?> execute(Runnable task) {
        synchronized (lock) {
            return this.executorService.submit(task);
        }
    }

    public void shutdown() {
        synchronized (lock) {
            this.executorService.shutdownNow();
        }
    }

    public ExecutorService getExecutorService() {
        synchronized (lock) {
            return executorService;
        }
    }

}
