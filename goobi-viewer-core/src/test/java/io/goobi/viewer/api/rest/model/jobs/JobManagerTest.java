/**
 * This file is part of the Goobi viewer - a content presentation and management application for digitized objects.
 *
 * Visit these websites for more information.
 *          - http://www.intranda.com
 *          - http://digiverso.com
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.goobi.viewer.api.rest.model.jobs;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.goobi.viewer.api.rest.model.jobs.Job.JobStatus;
import io.goobi.viewer.api.rest.model.jobs.Job.JobType;

/**
 * @author florian
 *
 */
public class JobManagerTest {

    JobManager manager;
    
    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        manager = new JobManager(Duration.of(7, ChronoUnit.DAYS));
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception {
    }
    
    @Test
    public void testAddJob() throws InterruptedException {
        Job job = new Job(JobType.NOTIFY_SEARCH_UPDATE, (request, me) -> {});
        manager.addJob(job);
        assertEquals(JobStatus.CREATED, manager.getJob(job.id).status);
        manager.triggerJobInThread(job.id, null);
        Thread.sleep(10);
        assertEquals(JobStatus.COMPLETE, manager.getJob(job.id).status);
    }
    
    @Test void testListJobs() {
        Job job1 = new Job(JobType.NOTIFY_SEARCH_UPDATE, (request, me) -> {});
        Job job2 = new Job(JobType.NOTIFY_SEARCH_UPDATE, (request, me) -> {});
        manager.addJob(job1);
        manager.addJob(job2);
        
        assertEquals(2, manager.getJobs(JobType.NOTIFY_SEARCH_UPDATE).size());
    }

}
