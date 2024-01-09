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
package io.goobi.viewer.api.rest.model.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.api.rest.model.tasks.Task;
import io.goobi.viewer.api.rest.model.tasks.Task.TaskStatus;
import io.goobi.viewer.api.rest.model.tasks.TaskManager;
import io.goobi.viewer.api.rest.model.tasks.TaskParameter;
import io.goobi.viewer.model.job.TaskType;

/**
 * @author florian
 *
 */
public class JobManagerTest {

    TaskManager manager;

    /**
     * @throws java.lang.Exception
     */
    @BeforeEach
    public void setUp() throws Exception {
        manager = new TaskManager(Duration.of(7, ChronoUnit.DAYS));
    }

    @Test
    public void testAddJob() throws InterruptedException {
        Task job = new Task(new TaskParameter(TaskType.NOTIFY_SEARCH_UPDATE), (request, me) -> {
        });
        manager.addTask(job);
        assertEquals(TaskStatus.CREATED, manager.getTask(job.getId()).getStatus());
        Future future = manager.triggerTaskInThread(job.getId(), null);
        try {
            future.get(1, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException e) {
            fail(e.toString());
        }
        assertEquals(TaskStatus.COMPLETE, manager.getTask(job.getId()).getStatus());
    }

    @Test
    public void testListJobs() {
        Task job1 = new Task(new TaskParameter(TaskType.NOTIFY_SEARCH_UPDATE), (request, me) -> {
        });
        Task job2 = new Task(new TaskParameter(TaskType.NOTIFY_SEARCH_UPDATE), (request, me) -> {
        });
        manager.addTask(job1);
        manager.addTask(job2);

        assertEquals(2, manager.getTasks(TaskType.NOTIFY_SEARCH_UPDATE).size());
    }

}
