package io.goobi.viewer.managedbeans;

import java.io.Serializable;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


@Named
@ApplicationScoped
public class QuartzBean  implements Serializable{

    private static final long serialVersionUID = -8294562786947936886L;

    private static final Logger log = LogManager.getLogger(QuartzBean.class);




}
