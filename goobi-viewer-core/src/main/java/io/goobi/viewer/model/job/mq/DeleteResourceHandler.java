package io.goobi.viewer.model.job.mq;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.goobi.viewer.controller.mq.MessageHandler;
import io.goobi.viewer.controller.mq.MessageQueueManager;
import io.goobi.viewer.controller.mq.MessageStatus;
import io.goobi.viewer.controller.mq.ViewerMessage;
import io.goobi.viewer.model.job.TaskType;

public class DeleteResourceHandler implements MessageHandler<MessageStatus> {

    public static final String PARAMETER_RESOURCE_PATH = "path";
    
    private static final Logger logger = LogManager.getLogger(DeleteResourceHandler.class);

    
    @Override
    public MessageStatus call(ViewerMessage ticket, MessageQueueManager queueManager) {
        
        String pathString = ticket.getProperties().get(PARAMETER_RESOURCE_PATH);
        
        if(StringUtils.isNotBlank(pathString)) {
            Path path  = Path.of(pathString);
            if(Files.exists(path)) {
                try {
                    FileUtils.deleteDirectory(path.toFile());
                    return MessageStatus.FINISH;
                } catch (IOException e) {
                    logger.error("Error deleting path {}. Reason: {}", path, e.toString());
                    return MessageStatus.ERROR;            
                }
            } else {
                logger.error("Cannot delete resource at  {}. file location does not exist", path);
                return MessageStatus.ERROR;      
            }
        } else {
            logger.error("Error deleting path. Path is empty");
            return MessageStatus.ERROR;      
        }
        
    }

    @Override
    public String getMessageHandlerName() {
        return TaskType.DELETE_RESOURCE.name();
    }

}
