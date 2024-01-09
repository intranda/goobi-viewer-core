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

import java.util.Date;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.goobi.viewer.AbstractSolrEnabledTest;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.model.job.JobStatus;

public class UploadJobTest extends AbstractSolrEnabledTest {

    /**
     * @see UploadJob#buildProcessCreationRequest()
     * @verifies create request object correctly
     */
    @Test
    public void buildProcessCreationRequest_shouldCreateRequestObjectCorrectly() throws Exception {
        UploadJob uj = new UploadJob();
        uj.setTemplateName("Sample_workflow");
        uj.setEmail("a@b.com");
        uj.setPi("PPN123");
        uj.setTitle("Lorem ipsum");
        uj.setDescription("Lorem ipsum dolor sit amet...");

        ProcessCreationRequest result = uj.buildProcessCreationRequest();
        Assertions.assertNotNull(result);
        Assertions.assertEquals("Sample_workflow", result.getTemplateName());
        Assertions.assertEquals("PPN123", result.getIdentifier());
        Assertions.assertEquals("viewer_PPN123", result.getProcesstitle());

        Assertions.assertNotNull(result.getMetadata());
        Assertions.assertEquals("Lorem ipsum", result.getMetadata().get("TitleDocMain"));
        Assertions.assertEquals("Lorem ipsum dolor sit amet...", result.getMetadata().get("Description"));

        Assertions.assertNotNull(result.getProperties());
        Assertions.assertEquals("a@b.com", result.getProperties().get("email"));
    }

    /**
     * @see UploadJob#updateStatus(ProcessStatusResponse)
     * @verifies do nothing if response null
     */
    @Test
    public void updateStatus_shouldDoNothingIfResponseNull() throws Exception {
        UploadJob uj = new UploadJob();
        Assertions.assertEquals(JobStatus.UNDEFINED, uj.getStatus());
        Assertions.assertFalse(uj.updateStatus(null));
        Assertions.assertEquals(JobStatus.UNDEFINED, uj.getStatus());
    }

    /**
     * @see UploadJob#updateStatus(ProcessStatusResponse)
     * @verifies set status to error if process nonexistent
     */
    @Test
    public void updateStatus_shouldSetStatusToErrorIfProcessNonexistent() throws Exception {
        UploadJob uj = new UploadJob();
        ProcessStatusResponse psr = new ProcessStatusResponse();
        psr.setId(0);
        psr.setCreationDate(null);
        Assertions.assertTrue(uj.updateStatus(psr));
        Assertions.assertEquals(JobStatus.ERROR, uj.getStatus());

    }

    /**
     * @see UploadJob#updateStatus(ProcessStatusResponse)
     * @verifies set status to error of process rejected
     */
    @Test
    public void updateStatus_shouldSetStatusToErrorOfProcessRejected() throws Exception {
        UploadJob uj = new UploadJob();
        ProcessStatusResponse psr = new ProcessStatusResponse();
        psr.setId(1);
        psr.setCreationDate(new Date());
        psr.getProperties()
                .add(new PropertyResponse().setTitle(DataManager.getInstance().getConfiguration().getContentUploadRejectionPropertyName())
                        .setValue("true"));
        psr.getProperties()
                .add(new PropertyResponse().setTitle(DataManager.getInstance().getConfiguration().getContentUploadRejectionReasonPropertyName())
                        .setValue("Not good enough"));
        Assertions.assertTrue(uj.updateStatus(psr));
        Assertions.assertEquals(JobStatus.ERROR, uj.getStatus());
        Assertions.assertEquals("Not good enough", uj.getMessage());
    }

    /**
     * @see UploadJob#updateStatus(ProcessStatusResponse)
     * @verifies set status to ready if record in index
     */
    @Test
    public void updateStatus_shouldSetStatusToReadyIfRecordInIndex() throws Exception {
        UploadJob uj = new UploadJob();
        uj.setPi(PI_KLEIUNIV);
        ProcessStatusResponse psr = new ProcessStatusResponse();
        psr.setId(1);
        psr.setCreationDate(new Date());
        Assertions.assertTrue(uj.updateStatus(psr));
        Assertions.assertEquals(JobStatus.READY, uj.getStatus());
    }

    //    /**
    //     * @see UploadJob#updateStatus(ProcessStatusResponse)
    //     * @verifies set status to ready if process completed
    //     */
    //    @Test
    //    public void updateStatus_shouldSetStatusToReadyIfProcessCompleted() throws Exception {
    //        UploadJob uj = new UploadJob();
    //        ProcessStatusResponse psr = new ProcessStatusResponse();
    //        psr.setId(1);
    //        psr.setCreationDate(new Date());
    //        psr.setProcessCompleted(true);
    //        uj.updateStatus(psr);
    //        Assertions.assertEquals(JobStatus.READY, uj.getStatus());
    //    }

    //    /**
    //     * @see UploadJob#updateStatus(ProcessStatusResponse)
    //     * @verifies set status to ready if export step done
    //     */
    //    @Test
    //    public void updateStatus_shouldSetStatusToReadyIfExportStepDone() throws Exception {
    //        UploadJob uj = new UploadJob();
    //        ProcessStatusResponse psr = new ProcessStatusResponse();
    //        psr.setId(1);
    //        psr.setCreationDate(new Date());
    //        psr.setProcessCompleted(false);
    //        StepResponse sr = new StepResponse();
    //        sr.setTitle("Export to viewer");
    //        sr.setStatus("Completed");
    //        psr.getStep().add(sr);
    //        uj.updateStatus(psr);
    //        Assertions.assertEquals(JobStatus.READY, uj.getStatus());
    //    }
}