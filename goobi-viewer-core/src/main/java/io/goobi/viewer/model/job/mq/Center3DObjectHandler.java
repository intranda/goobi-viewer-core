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
package io.goobi.viewer.model.job.mq;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.api.rest.AbstractApiUrlManager;
import io.goobi.viewer.api.rest.v1.ApiUrls;
import io.goobi.viewer.controller.DataFileTools;
import io.goobi.viewer.controller.DataManager;
import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.exceptions.IndexUnreachableException;
import io.goobi.viewer.exceptions.PresentationException;
import io.goobi.viewer.model.job.TaskType;
import io.goobi.viewer.model.media.voyager.GltfBoundingBoxReader;
import io.goobi.viewer.model.media.voyager.VoyagerSceneBuilder;

/**
 * Message handler that auto-generates a centered Voyager SVX scene file for a GLTF or GLB 3D object.
 *
 * <p>The bounding-box centre of the model is computed from the GLTF POSITION accessor min/max values
 * and stored as a node translation in the generated {@code *.svx.json} file.  This ensures that the
 * Voyager viewer rotates around the object's actual centre rather than the coordinate-system origin.
 *
 * <p>If an {@code svx.json} file already exists for the model (e.g. from manual editing in
 * voyager-story) it is left untouched so that manual scene adjustments are preserved.
 */
public class Center3DObjectHandler implements MessageHandler<MessageStatus> {

    static final String PARAMETER_PI = "pi";
    static final String PARAMETER_FILENAME = "filename";
    static final String PARAMETER_FORCE = "force";

    private static final Logger logger = LogManager.getLogger(Center3DObjectHandler.class);

    /** Lazily resolved in production; may be set explicitly for testing. */
    private final AbstractApiUrlManager urls;

    /** Production constructor – resolves dependencies from {@link DataManager} at call time. */
    public Center3DObjectHandler() {
        this.urls = null;
    }

    /** Package-private constructor for unit tests. */
    Center3DObjectHandler(AbstractApiUrlManager urls) {
        this.urls = urls;
    }

    @Override
    public MessageStatus call(ViewerMessage message, MessageQueueManager queueManager) {
        String pi = message.getProperties().get(PARAMETER_PI);
        String filename = message.getProperties().get(PARAMETER_FILENAME);

        if (pi == null || filename == null) {
            logger.error("Missing required parameter 'pi' or 'filename' in {} message", getMessageHandlerName());
            return MessageStatus.ERROR;
        }

        boolean force = Boolean.parseBoolean(message.getProperties().getOrDefault(PARAMETER_FORCE, "false"));

        try {
            Path mediaDirectory = DataFileTools.getMediaFolder(pi);
            return processCentering(pi, filename, mediaDirectory, force);
        } catch (PresentationException | IndexUnreachableException e) {
            logger.error("Error resolving media folder for {}: {}", pi, e.toString());
            return MessageStatus.ERROR;
        }
    }

    /**
     * Core logic: computes the bounding-box centre of the model and writes a centered
     * {@code *.svx.json} next to it.  Separated from {@link #call} for testability.
     *
     * @param pi persistent identifier of the record
     * @param filename 3D model filename (must be {@code .gltf} or {@code .glb})
     * @param mediaDirectory directory that contains the model file
     * @param force if {@code true}, an existing SVX file is overwritten; if {@code false},
     *              the task is skipped when an SVX file already exists
     * @return {@link MessageStatus#FINISH} on success or deliberate skip,
     *         {@link MessageStatus#ERROR} on failure
     */
    MessageStatus processCentering(String pi, String filename, Path mediaDirectory, boolean force) {
        String ext = FilenameUtils.getExtension(filename).toLowerCase();
        if (!"gltf".equals(ext) && !"glb".equals(ext)) {
            logger.debug("Skipping centering for non-GLTF file: {}", filename);
            return MessageStatus.FINISH;
        }

        Path modelFile = mediaDirectory.resolve(filename);
        if (!Files.isRegularFile(modelFile)) {
            logger.error("3D model file not found: {}", modelFile);
            return MessageStatus.ERROR;
        }

        String baseFilename = FilenameUtils.getBaseName(filename);
        Path svxFile = mediaDirectory.resolve(baseFilename + ".svx.json");

        if (Files.exists(svxFile) && !force) {
            logger.debug("SVX scene file already exists for {}/{} – skipping auto-centering", pi, filename);
            return MessageStatus.FINISH;
        }

        try {
            double[][] bounds = GltfBoundingBoxReader.computeBounds(modelFile);
            double[] center = {
                    (bounds[0][0] + bounds[1][0]) / 2.0,
                    (bounds[0][1] + bounds[1][1]) / 2.0,
                    (bounds[0][2] + bounds[1][2]) / 2.0
            };
            logger.debug("Bounding-box centre for {}/{}: [{}, {}, {}]", pi, filename, center[0], center[1], center[2]);

            AbstractApiUrlManager urlManager = resolveUrlManager();
            URI modelUri = urlManager.path(ApiUrls.RECORDS_FILES_3D).params(pi, filename).buildURI();

            String scene = new VoyagerSceneBuilder(baseFilename)
                    .addModel(modelUri, modelFile)
                    .setTranslation(-center[0], -center[1], -center[2])
                    .setBounds(bounds[0], bounds[1])
                    .build();

            boolean overwriting = Files.exists(svxFile);
            StandardOpenOption createMode = overwriting ? StandardOpenOption.TRUNCATE_EXISTING : StandardOpenOption.CREATE_NEW;
            Files.writeString(svxFile, scene, StandardCharsets.UTF_8, StandardOpenOption.WRITE, createMode);
            logger.info("{} centered SVX scene file: {}", overwriting ? "Overwrote" : "Created", svxFile);

        } catch (IOException e) {
            logger.error("Error creating SVX scene file for {}/{}: {}", pi, filename, e.toString());
            return MessageStatus.ERROR;
        }

        return MessageStatus.FINISH;
    }

    private AbstractApiUrlManager resolveUrlManager() {
        if (this.urls != null) {
            return this.urls;
        }
        return DataManager.getInstance().getRestApiManager().getDataApiManager().orElse(new ApiUrls());
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.CENTER_3D_OBJECT.name();
    }

    /**
     * Creates a {@link ViewerMessage} that triggers centering for the given record's 3D model.
     * Skips the task if an SVX file already exists ({@code force=false}).
     *
     * @param pi persistent identifier of the record
     * @param filename 3D model filename ({@code .gltf} or {@code .glb})
     * @return ready-to-dispatch {@link ViewerMessage}
     */
    public static ViewerMessage createMessage(String pi, String filename) {
        return createMessage(pi, filename, false);
    }

    /**
     * Creates a {@link ViewerMessage} that triggers centering for the given record's 3D model.
     *
     * @param pi persistent identifier of the record
     * @param filename 3D model filename ({@code .gltf} or {@code .glb})
     * @param force if {@code true}, any existing SVX scene file will be overwritten
     * @return ready-to-dispatch {@link ViewerMessage}
     */
    public static ViewerMessage createMessage(String pi, String filename, boolean force) {
        ViewerMessage msg = new ViewerMessage(TaskType.CENTER_3D_OBJECT.name());
        msg.setProperties(Map.of(PARAMETER_PI, pi, PARAMETER_FILENAME, filename, PARAMETER_FORCE, Boolean.toString(force)));
        return msg;
    }
}
