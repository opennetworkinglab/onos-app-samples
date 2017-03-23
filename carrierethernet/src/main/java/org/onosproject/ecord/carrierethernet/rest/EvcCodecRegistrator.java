package org.onosproject.ecord.carrierethernet.rest;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.codec.CodecService;
import org.onosproject.ecord.carrierethernet.app.CarrierEthernetVirtualConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the JSON codec brokering service for Carrier Ethernet app.
 */
@Component(immediate = true)
public class EvcCodecRegistrator {

    private static final Logger log = LoggerFactory.getLogger(EvcCodecRegistrator.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CodecService codecService;

    @Activate
    public void activate() {
        codecService.registerCodec(CarrierEthernetVirtualConnection.class, new EvcCodec());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }
}
