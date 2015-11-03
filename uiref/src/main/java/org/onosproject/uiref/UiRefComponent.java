package org.onosproject.uiref;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * UI Reference Component.
 */
@Component(immediate = true)
public class UiRefComponent {
    private final Logger log = LoggerFactory.getLogger(getClass());


    @Activate
    public void activate() {
//        appId = coreService.registerApplication("org.onosproject.uiref");
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

}
