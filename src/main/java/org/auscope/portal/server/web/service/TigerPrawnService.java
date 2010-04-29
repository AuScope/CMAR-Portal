/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.auscope.portal.server.web.service;

import org.auscope.portal.server.web.IWFSGetFeatureMethodMaker;
import org.auscope.portal.server.web.WFSGetFeatureMethodMakerPOST;
import org.auscope.portal.mineraloccurrence.*;
import org.apache.commons.httpclient.HttpMethodBase;
import org.springframework.stereotype.Service;

import java.util.List;
import org.auscope.portal.tigerprawn.TigerPrawnFilter;

/**
 *
 * @author VOT002
 */
@Service
public class TigerPrawnService {
    private HttpServiceCaller httpServiceCaller;
    private MineralOccurrencesResponseHandler mineralOccurrencesResponseHandler;
    private IWFSGetFeatureMethodMaker methodMaker;

    /**
     * Initialise
     */
    public TigerPrawnService() {
        this.httpServiceCaller = new HttpServiceCaller();
        this.mineralOccurrencesResponseHandler = new MineralOccurrencesResponseHandler();
        this.methodMaker = new WFSGetFeatureMethodMakerPOST();
    }

    public TigerPrawnService(HttpServiceCaller httpServiceCaller, MineralOccurrencesResponseHandler mineralOccurrencesResponseHandler, IWFSGetFeatureMethodMaker methodMaker) {
        this.httpServiceCaller = httpServiceCaller;
        this.mineralOccurrencesResponseHandler = mineralOccurrencesResponseHandler;
        this.methodMaker = methodMaker;
    }

    /**
     * Get all the prawns in a given date range
     *
     * @param serviceURL - the service to get all of the mines from
     * @return
     * @throws Exception
     */
    public String getPrawnsWithinRangeGML(String serviceURL, String startDate, String endDate) throws Exception {
        //get the prawns method
        TigerPrawnFilter filter = new TigerPrawnFilter(startDate,endDate);
        HttpMethodBase method = methodMaker.makeMethod(serviceURL, "auscope:TIGER_CATCH",filter.getFilterString());

        //Run it
        return httpServiceCaller.getMethodResponseAsString(method, httpServiceCaller.getHttpClient());
    }
}

