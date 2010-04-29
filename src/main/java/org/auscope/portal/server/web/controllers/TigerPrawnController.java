package org.auscope.portal.server.web.controllers;

import java.io.StringWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.ModelMap;
import org.auscope.portal.server.web.service.HttpServiceCaller;
import org.auscope.portal.server.web.view.JSONModelAndView;
import org.auscope.portal.server.util.GmlToKml;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import net.sf.json.JSONArray;
import org.auscope.portal.server.util.gmlToCSV;
import org.auscope.portal.server.web.service.TigerPrawnService;

/**
 * Acts as a proxy to WFS's
 *
 * User: Mathew Wyatt
 * Date: 17/08/2009
 * Time: 12:10:41 PM
 */

@Controller
public class TigerPrawnController {
    protected final Log logger = LogFactory.getLog(getClass().getName());
    private HttpServiceCaller serviceCaller;
    private GmlToKml gmlToKml;
    private TigerPrawnService tigerPrawnService;

    @Autowired
    public TigerPrawnController(HttpServiceCaller serviceCaller,
                          GmlToKml gmlToKml,
                          TigerPrawnService tigerPrawnService) {
        this.serviceCaller = serviceCaller;
        this.gmlToKml = gmlToKml;
        this.tigerPrawnService = tigerPrawnService;
    }

    /**
     * Given a service Url and a feature type this will query for all of the features, then convert them into KML,
     * to be displayed, assuming that the response will be complex feature GeoSciML
     *
     *  This will save the requested WFS response as CSV
     * @param serviceUrl
     * @param featureType
     * @param request
     * @return
     * @throws Exception
     */
    @RequestMapping("/requestPrawns.do")
    public ModelAndView requestPrawns(@RequestParam("serviceUrl") final String serviceUrl,
                                           @RequestParam("startDate") final String startDate,
                                           @RequestParam("endDate") final String endDate,
                                           HttpServletRequest request) throws Exception {


        String gml = "", kml = "", csv = "";
        boolean success = true;

        try {
            gml = tigerPrawnService.getPrawnsWithinRangeGML(serviceUrl, startDate, endDate);
            kml = gmlToKml.convert(gml, request);

            gmlToCSV conv = new gmlToCSV();
            StringWriter sw = new StringWriter();
            conv.convert(gml, sw);

            setDateRange(request,startDate,endDate);
            request.getSession().setAttribute("selectedCSV", sw.toString());
        } catch (Exception ex) {
            success = false;
            logger.error("Error requesting prawns: " + ex.getMessage());
        }

         return makeModelAndViewKML(kml, gml, csv, success);
    }

   

    /**
     * Insert a kml block into a successful JSON response
     * @param kmlBlob
     * @return
     */
    private ModelAndView makeModelAndViewKML(final String kmlBlob, final String gmlBlob, final String csvBlob, final boolean success) {
        final Map data = new HashMap() {{
            put("kml", kmlBlob);
            put("gml", gmlBlob);
            put("csv", csvBlob);
        }};

        ModelMap model = new ModelMap() {{
            put("success", success);
            put("data", data);
        }};

        return new JSONModelAndView(model);
    }


    @RequestMapping("/selectSpecies.do")
   public ModelAndView selectSpecies(HttpServletRequest request,
		                     @RequestParam("speciesCode") String speciesCode) {

        ModelAndView result = new ModelAndView("jsonView");
        boolean success = true, duplicate = false;
        logger.debug("Saving species:"+speciesCode);
        List<String> existingList = (List<String>) request.getSession().getAttribute("selectedSpecies");
        if (existingList == null)
            existingList = new ArrayList<String>();

        if (!existingList.contains(speciesCode)) {
            logger.debug("Duplicate species ignored");
            duplicate = true;
        } else {
            existingList.add(speciesCode);
            request.getSession().setAttribute("selectedSpecies", existingList);
        }

        result.addObject("success", success);
        result.addObject("alreadyExists", duplicate);

        return result;
    }

   @RequestMapping("/getSpeciesSelection.do")
   public ModelAndView getSpeciesSelection(HttpServletRequest request) throws Exception {
	   List<String> selectedSpecies = (List<String>) request.getSession().getAttribute("speciesCode");

       logger.debug("Return user's selected species");

       ModelAndView result = new ModelAndView("jsonView");
       JSONArray codes = new JSONArray();

       if (selectedSpecies != null) {
            for(String s : selectedSpecies) {
                codes.add(s);
            }
       }

       result.addObject("success", true);
       result.addObject("speciesCodes", codes);

       return result;
   }

   @RequestMapping("/setDateRange.do")
   public ModelAndView setDateRange(HttpServletRequest request,
                                  @RequestParam("startDate") String startDate,
                                  @RequestParam("endDate") String endDate) {

	   ModelAndView result = new ModelAndView("jsonView");
       boolean success = true;
       SimpleDateFormat df = new SimpleDateFormat("yyyy-mm-dd");

       logger.debug("Return setting start end date: " + startDate + " " + endDate);

       //Attempt to parse (and report any errors
       try {
           //Ensure we can parse before we set session attribs
           Date start = df.parse(startDate);
           Date end = df.parse(endDate);

            request.getSession().setAttribute("startDate", start);
            request.getSession().setAttribute("endDate", end);
       } catch (ParseException ex) {
            logger.warn("Error setting date range: " + ex.getMessage());
            success = false;
       }

       result.addObject("success", success);

	   return result;
   }
}
