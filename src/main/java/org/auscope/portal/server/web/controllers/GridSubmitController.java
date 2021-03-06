/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2009 ESSCC, The University of Queensland
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.portal.server.web.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.net.URL;
import java.rmi.ServerException;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.sf.json.JSONArray;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.csw.CSWRecord;
import org.auscope.portal.csw.ICSWMethodMaker;
import org.auscope.portal.server.gridjob.CMARJob;
import org.auscope.portal.server.gridjob.CMARJobManager;

import org.auscope.portal.server.gridjob.FileInformation;
import org.auscope.portal.server.gridjob.GridAccessController;
import org.auscope.portal.server.gridjob.Util;
import org.auscope.portal.server.gridjob.GeodesySeries;
import org.auscope.portal.server.web.service.CSWService;
import org.auscope.portal.server.web.service.HttpServiceCaller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.ModelAndView;


//Globus stuff
import org.globus.ftp.DataChannelAuthentication;
import org.globus.ftp.FileInfo;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.GridFTPSession;
import org.globus.io.urlcopy.UrlCopy;
import org.globus.io.urlcopy.UrlCopyException;
import org.globus.util.GlobusURL;
import org.ietf.jgss.GSSCredential;
import org.springframework.ui.ModelMap;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;


/**
 * Controller for the job submission view.
 *
 * @author Cihan Altinay
 * @author Abdi Jama
 */
@Controller
public class GridSubmitController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());
    @Autowired
    private GridAccessController gridAccess;
    @Autowired
    private CMARJobManager jobManager;
    @Autowired
    private CSWService cswService;
    @Autowired
    private HttpServiceCaller serviceCaller;



    private static final String WFS_CSV_FILE = "wfs.csv";
    private static final String[] STAGE_IN_DIRECTORIES = new String[] {"bin","bin/RCS","java", "RCS", "tmp"};

    private static final String TABLE_DIR = "tables";
    private static final String RINEX_DIR = "rinex";
    private static final String PRE_STAGE_IN_TABLE_FILES = "/home/grid-auscope/tables/";
    private static final String PRE_STAGE_IN_CMAR_FILES = "/home/grid-auscope/cmar/";
    private static final String IVEC_MIRROR_URL = "http://files.ivec.org/geodesy/";
    private static final String PBSTORE_RINEX_PATH = "//pbstore/cg01/geodesy/ftp.ga.gov.au/gpsdata/";
    private static final String FOR_ALL = "Common";

    //Grid File Transfer messages
    private static final String FILE_COPIED = "Please wait while files being transfered.... ";
    private static final String FILE_COPY_ERROR = "Job submission failed due to file transfer Error.";
    private static final String INTERNAL_ERROR= "Job submission failed due to INTERNAL ERROR";
    private static final String GRID_LINK = "Job submission failed due to GRID Link Error";
    private static final String TRANSFER_COMPLETE = "Transfer Complete";
    private static final String CREDENTIAL_ERROR = "Job submission failed due to Invalid Credential Error";
    
    
    /**
     * Given a list of stations and a date range this function queries the remote serviceUrl for a list of log files
     * and returns a JSON representation of the response
     * 
     * @param dateFrom The (inclusive) start of date range in YYYY-MM-DD format
     * @param dateTo The (inclusive) end of date range in YYYY-MM-DD format
     * @param serviceUrl The remote service URL to query
     * @param stationList a list (comma seperated) of the GPSSITEID to fetch log files for
     * 
     * Response Format
     * {
     *     success : (true/false),
     *     urlList : [{
     *         url    : (Mapped from url or empty string)
     *         date   : (Mapped from date or empty string)
     *     }]
     * }
     */
    @RequestMapping(value = "/getStationListUrls.do", params = {"dateFrom","dateTo", "stationList", "serviceUrl"})
    public ModelAndView getStationListUrls(@RequestParam("dateFrom") final String dateFrom,
								          @RequestParam("dateTo") final String dateTo,
								          @RequestParam("serviceUrl") final String serviceUrl,
								          @RequestParam("stationList") final String stationList,
								          HttpServletRequest request) {
    	boolean success = true;
    	ModelAndView jsonResponse = new ModelAndView("jsonView");
    	JSONArray urlList = new JSONArray();
    	
    	logger.debug("getStationListUrls : Requesting urls for " + stationList + " in the range " + dateFrom + " -> " + dateTo);
    	
    	try {
    		String gmlResponse = serviceCaller.getMethodResponseAsString(new ICSWMethodMaker() {
                public HttpMethodBase makeMethod() {
                    GetMethod method = new GetMethod(serviceUrl);

                    //Generate our filter string based on date and station list
                    String cqlFilter = "(date>='" + dateFrom + "')AND(date<='" + dateTo + "')";
                    if (stationList != null && stationList.length() > 0) {
                    	
                    	String[] stations = stationList.split(",");
                    	
                    	cqlFilter += "AND(";
                    	
                    	for (int i = 0; i < stations.length; i++) {
                    		if (i > 0)
                    			cqlFilter += "OR";
                    		
                    		cqlFilter += "(id='" + stations[i] + "')";
                    	}
                    	
                    	cqlFilter += ")";
                    }

                    //attach them to the method
                    method.setQueryString(new NameValuePair[]{new NameValuePair("request", "GetFeature"), 
                    											new NameValuePair("outputFormat", "GML2"),
                    											new NameValuePair("typeName", "geodesy:station_observations"),
                    											new NameValuePair("PropertyName", "geodesy:date,geodesy:url"),
                    											new NameValuePair("CQL_FILTER", cqlFilter)});

                    return method;
                }
            }.makeMethod(), serviceCaller.getHttpClient());
    		
    		//Parse our XML string into a document
    		XPath xPath = XPathFactory.newInstance().newXPath();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document responseDocument = builder.parse(new InputSource(new StringReader(gmlResponse)));
    		
            //Extract the URL list and parse it into the JSON list
            HttpSession userSession = request.getSession();
            String featureMemberExpression = "/FeatureCollection/featureMember";
            NodeList nodes = (NodeList) xPath.evaluate(featureMemberExpression, responseDocument, XPathConstants.NODESET);
            if (nodes != null) {
	            for(int i=0; i < nodes.getLength(); i++ ) {
	            	ModelMap urlMap = new ModelMap();
	            	Node tempNode = (Node) xPath.evaluate("station_observations/url", nodes.item(i), XPathConstants.NODE);
	            	urlMap.addAttribute("url", tempNode == null ? "" : tempNode.getTextContent());
	            	
	            	tempNode = (Node) xPath.evaluate("station_observations/date", nodes.item(i), XPathConstants.NODE);
	            	urlMap.addAttribute("date", tempNode == null ? "" : tempNode.getTextContent());
	            	
	            	urlList.add(urlMap);
	            }
            }
            
    	} catch (Exception ex) {
    		logger.warn("selectStationList.do : Error " + ex.getMessage());
    		urlList.clear();
            success = false;
    	}
    	
    	jsonResponse.addObject("success", success);
    	jsonResponse.addObject("urlList", urlList);
    	
        return jsonResponse;
    }
    
    /**
     * Returns every Geodesy station (and some extra descriptive info) in a JSON format.
     * 
     * Response Format
     * {
     *     success : (true/false),
     *     records : [{
     *         stationNumber : (Mapped from STATIONNO or empty string)
     *         stationName   : (Mapped from STATIONNAME or empty string)
     *         gpsSiteId     : (Mapped from GPSSITEID or empty string)
     *         countryId     : (Mapped from COUNTRYID or empty string)
     *         stateId       : (Mapped from STATEID or empty string)
     *     }]
     * }
     */
    @RequestMapping("/getStationList.do")
    public ModelAndView getStationList() {
        final String stationTypeName = "ngcp:GnssStation";
        ModelAndView jsonResponse = new ModelAndView("jsonView");
        JSONArray stationList = new JSONArray();
        boolean success = true;

        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            
            //Query every geodesy station provider for the raw GML
            CSWRecord[] geodesyRecords = cswService.getWFSRecordsForTypename(stationTypeName);
            if (geodesyRecords == null || geodesyRecords.length == 0)
            	throw new Exception("No " + stationTypeName + " records available");
            
            //This makes the assumption of only a single geodesy WFS
            CSWRecord record = geodesyRecords[0];
            final String serviceUrl = record.getServiceUrl();
            
            jsonResponse.addObject("serviceUrl", serviceUrl);

            logger.debug("getStationListXML.do : Requesting " + stationTypeName + " for " + serviceUrl);

            String gmlResponse = serviceCaller.getMethodResponseAsString(new ICSWMethodMaker() {
                public HttpMethodBase makeMethod() {
                    GetMethod method = new GetMethod(serviceUrl);

                    //set all of the parameters
                    NameValuePair request = new NameValuePair("request", "GetFeature");
                    NameValuePair elementSet = new NameValuePair("typeName", stationTypeName);

                    //attach them to the method
                    method.setQueryString(new NameValuePair[]{request, elementSet});

                    return method;
                }
            }.makeMethod(), serviceCaller.getHttpClient());
            
            //Parse the raw GML and generate some useful JSON objects
            Document doc = builder.parse(new InputSource(new StringReader(gmlResponse)));
            
            String serviceTitleExpression = "/FeatureCollection/featureMembers/GnssStation";
            NodeList nodes = (NodeList) xPath.evaluate(serviceTitleExpression, doc, XPathConstants.NODESET);

            //Lets pull some useful info out
            for(int i=0; nodes != null && i < nodes.getLength(); i++ ) {
                Node node = nodes.item(i);
                ModelMap stationMap = new ModelMap();

                Node tempNode = (Node) xPath.evaluate("STATIONNO", node, XPathConstants.NODE);
                stationMap.addAttribute("stationNumber", tempNode == null ? "" : tempNode.getTextContent());
                
                tempNode = (Node) xPath.evaluate("GPSSITEID", node, XPathConstants.NODE);
                stationMap.addAttribute("gpsSiteId", tempNode == null ? "" : tempNode.getTextContent());
                
                tempNode = (Node) xPath.evaluate("STATIONNAME", node, XPathConstants.NODE);
                stationMap.addAttribute("stationName", tempNode == null ? "" : tempNode.getTextContent());
                
                tempNode = (Node) xPath.evaluate("COUNTRYID", node, XPathConstants.NODE);
                stationMap.addAttribute("countryId", tempNode == null ? "" : tempNode.getTextContent());
                
                tempNode = (Node) xPath.evaluate("STATEID", node, XPathConstants.NODE);
                stationMap.addAttribute("stateId", tempNode == null ? "" : tempNode.getTextContent());

                stationList.add(stationMap);
            }
        } catch (Exception ex) {
            logger.warn("getStationListXML.do : Error " + ex.getMessage());
            success = false;
            stationList.clear();
        }

        //Form our response object
        jsonResponse.addObject("stations", stationList);
        jsonResponse.addObject("success", success);
        return jsonResponse;
    }

    /**
     * Sets the <code>GridAccessController</code> to be used for grid
     * activities.
     *
     * @param gridAccess the GridAccessController to use
     */
    /*public void setGridAccess(GridAccessController gridAccess) {
        this.gridAccess = gridAccess;
    }*/

    /**
     * Sets the <code>GeodesyJobManager</code> to be used to retrieve and store
     * series and job details.
     *
     * @param jobManager the JobManager to use
     */
    /*public void setJobManager(GeodesyJobManager jobManager) {
        this.jobManager = jobManager;
    }*/

    /*protected ModelAndView handleNoSuchRequestHandlingMethod(
            NoSuchRequestHandlingMethodException ex,
            HttpServletRequest request,
            HttpServletResponse response) {

        // Ensure user has valid grid credentials
        if (gridAccess.isProxyValid(
                request.getSession().getAttribute("userCred"))) {
        	logger.debug("No/invalid action parameter; returning gridsubmit view.");
        	return new ModelAndView("gridsubmit");
        } 
        else 
        {
        	request.getSession().setAttribute(
                "redirectAfterLogin", "/gridsubmit.html");
        	logger.warn("Proxy not initialized. Redirecting to gridLogin.");
        	return new ModelAndView(
                new RedirectView("/gridLogin.html", true, false, false));
        }
    }*/
    

    /**
     * Very simple helper class (bean).
     */
    public class SimpleBean {
        private String value;
        public SimpleBean(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    /**
     * Returns a JSON object containing a list of code.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         SimpleBean objects contain available code.
     */
    @RequestMapping("/getCodeObject.do")
    public ModelAndView getCodeObject(HttpServletRequest request,
                                 HttpServletResponse response) {

        String user = request.getRemoteUser();
        logger.debug("Querying code list for "+user);
        List<SimpleBean> code = new ArrayList<SimpleBean>();
        code.add(new SimpleBean(CMARJob.CODE_NAME));
        

        logger.debug("Returning list of "+code.size()+" codeObject.");
        return new ModelAndView("jsonView", "code", code);
    }    

    /**
     * Returns a JSON object containing a list of jobTypes.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         SimpleBean objects contain available jobTypes.
     */
    @RequestMapping("/listJobTypes.do")
    public ModelAndView listJobTypes(HttpServletRequest request,
                                 HttpServletResponse response) {

        String user = request.getRemoteUser();
        logger.debug("Querying job types list for "+user);
        List<SimpleBean> jobType = new ArrayList<SimpleBean>();
        jobType.add(new SimpleBean("single"));
        

        logger.debug("Returning list of "+jobType.size()+" jobType.");
        return new ModelAndView("jsonView", "jobType", jobType);
    }    

    /**
     * Returns a JSON object containing a list of jobTypes.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a series attribute which is an array of
     *         SimpleBean objects contain available jobTypes.
     */
    @RequestMapping("/getGetArguments.do")
    public ModelAndView getGetArguments(HttpServletRequest request,
                                 HttpServletResponse response) {

        String user = request.getRemoteUser();
        logger.debug("Querying param for "+user);
        List<SimpleBean> params = new ArrayList<SimpleBean>();
        params.add(new SimpleBean("enter args ..."));
        

        logger.debug("Returning list of "+params.size()+" params.");
        return new ModelAndView("jsonView", "paramLines", params);
    }
    
    
    /**
     * Returns a JSON object containing an array of ESyS-particle sites.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a sites attribute which is an array of
     *         sites on the grid that have an installation of ESyS-particle.
     */
   /* @RequestMapping("/listSites.do")    
    public ModelAndView listSites(HttpServletRequest request,
                                  HttpServletResponse response) {

        logger.debug("Retrieving sites with "+GeodesyJob.CODE_NAME+" installations.");
        String[] particleSites = gridAccess.
                retrieveSitesWithSoftwareAndVersion(GeodesyJob.CODE_NAME, "");

        List<SimpleBean> sites = new ArrayList<SimpleBean>();
        for (int i=0; i<particleSites.length; i++) {
            sites.add(new SimpleBean(particleSites[i]));
            logger.debug("Site name: "+particleSites[i]);
        }

        logger.debug("Returning list of "+particleSites.length+" sites.");
        return new ModelAndView("jsonView", "sites", sites);
    }*/

    /**
     * Returns a JSON object containing an array of sites that have the code.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a sites attribute which is an array of
     *         sites on the grid that have an installation of the selected code.
     */
    @RequestMapping("/listSites.do")    
    public ModelAndView listSites(HttpServletRequest request,
                                  HttpServletResponse response) {
    	String myCode = request.getParameter("code");
        logger.debug("Retrieving sites with "+myCode+" installations.");
        String[] particleSites = gridAccess.
                retrieveSitesWithSoftwareAndVersion(myCode, "");

        List<SimpleBean> sites = new ArrayList<SimpleBean>();
        for (int i=0; i<particleSites.length; i++) {
            sites.add(new SimpleBean(particleSites[i]));
            logger.debug("Site name: "+particleSites[i]);
        }

        logger.debug("Returning list of "+particleSites.length+" sites.");
        return new ModelAndView("jsonView", "sites", sites);
    }
    
    /**
     * Returns a JSON object containing an array of job manager queues at
     * the specified site.
     *
     * @param request The servlet request including a site parameter
     * @param response The servlet response
     *
     * @return A JSON object with a queues attribute which is an array of
     *         job queues available at requested site.
     */
    @RequestMapping("/listSiteQueues.do")    
    public ModelAndView listSiteQueues(HttpServletRequest request,
                                       HttpServletResponse response) {

        String site = request.getParameter("site");
        List<SimpleBean> queues = new ArrayList<SimpleBean>();

        if (site != null) {
            logger.debug("Retrieving queue names at "+site);

            String[] siteQueues = gridAccess.
                    retrieveQueueNamesAtSite(site);

            for (int i=0; i<siteQueues.length; i++) {
                queues.add(new SimpleBean(siteQueues[i]));
            }
        } else {
            logger.warn("No site specified!");
        }

        logger.debug("Returning list of "+queues.size()+" queue names.");
        return new ModelAndView("jsonView", "queues", queues);
    }

    /**
     * Returns a JSON object containing an array of versions at
     * the specified site.
     *
     * @param request The servlet request including a site parameter
     * @param response The servlet response
     *
     * @return A JSON object with a versions attribute which is an array of
     *         versions installed at requested site.
     */
    @RequestMapping("/listSiteVersions.do")    
    public ModelAndView listSiteVersions(HttpServletRequest request,
                                         HttpServletResponse response) {

        String site = request.getParameter("site");
        String myCode = request.getParameter("code");
        List<SimpleBean> versions = new ArrayList<SimpleBean>();

        if (site != null || myCode != null ) {
            logger.debug("Retrieving versions at "+site);

            String[] siteVersions = gridAccess.
                    retrieveCodeVersionsAtSite(site, myCode);

            for (int i=0; i<siteVersions.length; i++) {
                versions.add(new SimpleBean(siteVersions[i]));
            }
        } else {
            logger.warn("No site or code are specified!");
        }

        logger.debug("Returning list of "+versions.size()+" versions.");
        return new ModelAndView("jsonView", "versions", versions);
    }

    /**
     * Returns a JSON object containing a populated GeodesyJob object.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a data attribute containing a populated
     *         GeodesyJob object and a success attribute.
     */
    @RequestMapping("/getJobObject.do")    
    public ModelAndView getJobObject(HttpServletRequest request,
                                     HttpServletResponse response) {

        CMARJob job = prepareModel(request);

        logger.debug("Returning job.");
        ModelAndView result = new ModelAndView("jsonView");

        if(job == null) {
            logger.error("Job setup failure.");
            result.addObject("success", false);
        } else {
            result.addObject("data", job);
            result.addObject("success", true);
        }

        return result;
    }

    /**
     * Returns a JSON object containing an array of filenames and sizes which
     * are currently in the job's stage in directory.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         filenames.
     */
    @RequestMapping("/listJobFiles.do")    
    public ModelAndView listJobFiles(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("localJobInputDir");
        String jobType = (String) request.getParameter("jobType");
        logger.debug("Inside listJobFiles.do");
        List files = new ArrayList<FileInformation>();

        if (jobInputDir != null) {
        	if(jobType != null ){
        		String filePath = jobInputDir+GridSubmitController.RINEX_DIR;
	            File dir = new File(filePath+File.separator);
	            addFileNamesOfDirectory(files, dir, GridSubmitController.FOR_ALL, filePath);
	            filePath = jobInputDir+GridSubmitController.TABLE_DIR;
	            dir = new File(filePath+File.separator);
	            addFileNamesOfDirectory(files, dir, GridSubmitController.FOR_ALL, filePath);
	            logger.debug("Inside listJobFiles.do multi job");
	            boolean subJobExist = true;
	            int i = 0;
	            while(subJobExist){
	            	String subDirID = "subJob_"+i;
	            	File subDir = new File(jobInputDir+subDirID+File.separator);
	            	if(subDir.exists()){
	            		//list rinex dir
	            		filePath = jobInputDir+subDirID+File.separator+GridSubmitController.RINEX_DIR;
	            		subDir = new File(filePath +File.separator);
	            		if(subDir.exists()){
	            			addFileNamesOfDirectory(files, dir, subDirID, filePath);
	            		}
	            		//list tables dir
	            		filePath = jobInputDir+subDirID+File.separator+GridSubmitController.TABLE_DIR;
	            		subDir = new File(filePath+File.separator);
	            		if(subDir.exists()){
	            			addFileNamesOfDirectory(files, dir, subDirID, filePath);
	            		}
	            	}
	            	else
	            	{
	            		//exit loop
	            		subJobExist = false;
	            	}	            	
	            	i++;
	            }
	             
        	}/*else{
        		logger.debug("Inside listJobFiles.do single job");
        		String filePath = jobInputDir+GridSubmitController.RINEX_DIR;
	            File dir = new File(filePath+File.separator);
	            addFileNamesOfDirectory(files, dir, GridSubmitController.FOR_ALL, filePath);
	            
	            filePath = jobInputDir+GridSubmitController.TABLE_DIR;
	            dir = new File(filePath+File.separator);
	            addFileNamesOfDirectory(files, dir, GridSubmitController.FOR_ALL, filePath);
        	}*/
        }

        logger.debug("Returning list of "+files.size()+" files.");
        return new ModelAndView("jsonView", "files", files);
    }

    /**
     * Sends the contents of a input job file to the client.
     *
     * @param request The servlet request including a filename parameter
     *                
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist view with an error parameter on
     *         failure.
     */
    @RequestMapping("/downloadInputFile.do")
    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        String dirPathStr = request.getParameter("dirPath");
        String fileName = request.getParameter("filename");
        String errorString = null;
        
        if (dirPathStr != null && fileName != null) {
            logger.debug("Downloading: "+dirPathStr+fileName+".");
            File f = new File(dirPathStr+File.separator+fileName);
            if (!f.canRead()) {
                logger.error("File "+f.getPath()+" not readable!");
                errorString = new String("File could not be read.");
            } else {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition",
                        "attachment; filename=\""+fileName+"\"");

                try {
                    byte[] buffer = new byte[16384];
                    int count = 0;
                    OutputStream out = response.getOutputStream();
                    FileInputStream fin = new FileInputStream(f);
                    while ((count = fin.read(buffer)) != -1) {
                        out.write(buffer, 0, count);
                    }
                    out.flush();
                    return null;

                } catch (IOException e) {
                    errorString = new String("Could not send file: " +
                            e.getMessage());
                    logger.error(errorString);
                }
            }
        }

        // We only end up here in case of an error so return a suitable message
        if (errorString == null) {
            if (dirPathStr == null) {
                errorString = new String("Invalid input job file path specified!");
                logger.error(errorString);
            } else if (fileName == null) {
                errorString = new String("No filename provided!");
                logger.error(errorString);
            } else {
                // should never get here
                errorString = new String("Something went wrong.");
                logger.error(errorString);
            }
        }
        return new ModelAndView("jsonView", "error", errorString);
    }
    
    /**
     * Processes a file upload request returning a JSON object which indicates
     * whether the upload was successful and contains the filename and file
     * size.
     *
     * @param request The servlet request
     * @param response The servlet response containing the JSON data
     *
     * @return null
     */
    @RequestMapping("/uploadFile.do")    
    public ModelAndView uploadFile(HttpServletRequest request,
                                   HttpServletResponse response) {
    	logger.debug("Entering upload.do ");
        String jobInputDir = (String) request.getSession()
            .getAttribute("localJobInputDir");
        
        MultipartHttpServletRequest mfReq =
            (MultipartHttpServletRequest) request; 
        String jobType = (String) mfReq.getParameter("jobType");
        String subJobId = (String) mfReq.getParameter("subJobId");


        
        boolean success = true;
        String error = null;
        FileInformation fileInfo = null;
        String destinationPath = null;

        MultipartFile f = mfReq.getFile("file");
        
        if (f != null) {        	
        	String fileType = checkFileType(f.getOriginalFilename());
            //check if multiJob or not
            if(jobType.equals("single") || subJobId.equals(GridSubmitController.FOR_ALL)){
            	logger.debug("uploading file for single job ");
            	subJobId = GridSubmitController.FOR_ALL;
            	if(fileType.equals(GridSubmitController.TABLE_DIR)){
            		destinationPath = jobInputDir+GridSubmitController.TABLE_DIR+File.separator;
            	}else{
            		destinationPath = jobInputDir+GridSubmitController.RINEX_DIR+File.separator;
            	}
            }
            else{
            	logger.debug("uploading file for multi job ");
                
                String subJobInputDir = jobInputDir+subJobId.trim()+File.separator;
            	if(createLocalSubJobDir(request, subJobInputDir, fileType, subJobId.trim())){
            		if(fileType.equals(GridSubmitController.TABLE_DIR)){
            			destinationPath = subJobInputDir+GridSubmitController.TABLE_DIR+File.separator;
            		}
            		else{
            			destinationPath = subJobInputDir+GridSubmitController.RINEX_DIR+File.separator;
            		}    		
            	}
            	else{

                    logger.error("Could not create local subJob Directories.");
                    success = false;
                    error = new String("Could not create local subJob Directories.");        		
            	}        	
            }
            if (jobInputDir != null && success) {

                    logger.info("Saving uploaded file "+f.getOriginalFilename());
                    //TO-DO allow to upload on tables directory as well. GUI functions to be added.
                    File destination = new File(destinationPath+f.getOriginalFilename());
                    if (destination.exists()) {
                        logger.debug("Will overwrite existing file.");
                    }
                    try {
                        f.transferTo(destination);
                    } catch (IOException e) {
                        logger.error("Could not move file: "+e.getMessage());
                        success = false;
                        error = new String("Could not process file.");
                    }
                    fileInfo = new FileInformation(
                            f.getOriginalFilename(), f.getSize());

            } else {
                logger.error("Input directory not found or couldn't be created in current session!");
                success = false;
                error = new String("Internal error. Please reload the page.");
            }        	
        }else{
            logger.error("No file parameter provided.");
            success = false;
            error = new String("Invalid request.");
        }        


        // We cannot use jsonView here since this is a file upload request and
        // ExtJS uses a hidden iframe which receives the response.
        response.setContentType("text/html");
        response.setStatus(HttpServletResponse.SC_OK);
        try {
            PrintWriter pw = response.getWriter();
            pw.print("{success:'"+success+"'");
            if (error != null) {
                pw.print(",error:'"+error+"'");
            }
            if (fileInfo != null) {
                pw.print(",name:'"+fileInfo.getName()+"',size:"+fileInfo.getSize()+",subJob:'"+subJobId+"'");
            }
            pw.print("}");
            pw.flush();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
        return null;
    }

    /**
     * Deletes one or more uploaded files of the current job.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute that indicates whether
     *         the files were successfully deleted.
     */
    @RequestMapping("/deleteFiles.do")    
    public ModelAndView deleteFiles(HttpServletRequest request,
                                    HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("localJobInputDir");
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success;
        
        if (jobInputDir != null) {
            success = true;
            String filesPrm = request.getParameter("files");
            String subJobPrm = (String) request.getParameter("subJobId");
            
            logger.debug("Request to delete "+filesPrm);
            String[] files = (String[]) JSONArray.toArray(
                    JSONArray.fromObject(filesPrm), String.class);
            String[] subJobId = (String[]) JSONArray.toArray(
                    JSONArray.fromObject(subJobPrm), String.class);
            int i =0;
            for (String filename: files) {
            	String fileType = checkFileType(filename);
            	String fullFilename = null;
            	
            	if(subJobId[i] == null || subJobId[i].equals(""))
            		subJobId[i] = GridSubmitController.FOR_ALL;
            	
            	if(subJobId[i].equals(GridSubmitController.FOR_ALL)){
            		logger.debug("Deleting "+filename+" for subJob"+subJobId[i]);
                	if(fileType.equals(GridSubmitController.TABLE_DIR)){
                		fullFilename = jobInputDir+GridSubmitController.TABLE_DIR
                		                          +File.separator+filename;
                	}else{
                		fullFilename = jobInputDir+GridSubmitController.RINEX_DIR
                		                          +File.separator+filename;
                	}
            	}else{
            		logger.debug("Deleting "+filename+" for subJob"+subJobId[i]);
                	if(fileType.equals(GridSubmitController.TABLE_DIR)){
                		fullFilename = jobInputDir+subJobId[i]+File.separator
                		               +GridSubmitController.TABLE_DIR+File.separator+filename;
                	}else{
                		fullFilename = jobInputDir+subJobId[i]+File.separator
                		               +GridSubmitController.RINEX_DIR+File.separator+filename;
                	}           		
            	}

                File f = new File(fullFilename);
                if (f.exists() && f.isFile()) {
                    logger.debug("Deleting "+f.getPath());
                    boolean lsuccess = f.delete();
                    if (!lsuccess) {
                        logger.warn("Unable to delete "+f.getPath());
                        success = false;
                    }
                } else {
                    logger.warn(f.getPath()+" does not exist or is not a file!");
                }
                i++;
            }
        } else {
            success = false;
        }

        mav.addObject("success", success);
        return mav;
    }


    
    /**
     * Get status of the current job submission.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute that indicates the status.
     *         
     */
    @RequestMapping("/getJobStatus.do")  
    public ModelAndView getJobStatus(HttpServletRequest request,
                                    HttpServletResponse response) {

        ModelAndView mav = new ModelAndView("jsonView");
        GridTransferStatus jobStatus = (GridTransferStatus)request.getSession().getAttribute("gridStatus");
        if (jobStatus != null) {
        	mav.addObject("data", jobStatus.currentStatusMsg);
        	mav.addObject("jobStatus", jobStatus.jobSubmissionStatus);
        } else {
        	mav.addObject("data", "Grid File Transfere failed.");
        	mav.addObject("jobStatus", JobSubmissionStatus.Failed);
        }

        mav.addObject("success", true);
        return mav;
    }
    
    /**
     * Cancels the current job submission. Called to clean up temporary files.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return null
     */
    @RequestMapping("/cancelSubmission.do")    
    public ModelAndView cancelSubmission(HttpServletRequest request,
                                         HttpServletResponse response) {

        String jobInputDir = (String) request.getSession()
            .getAttribute("localJobInputDir");

        if (jobInputDir != null) {
            logger.debug("Deleting temporary job files.");
            File jobDir = new File(jobInputDir);
            Util.deleteFilesRecursive(jobDir);
            request.getSession().removeAttribute("localJobInputDir");
        }

        return null;
    }

    /**
     * Processes a job submission request.
     *
     * @param request The servlet request
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute that indicates whether
     *         the job was successfully submitted.
     */
    @RequestMapping("/submitJob.do")    
    public ModelAndView submitJob(HttpServletRequest request,
                                  HttpServletResponse response,
                                  CMARJob job) {

        logger.debug("Job details:\n"+job.toString());

        boolean success = true;
        final String user = request.getRemoteUser();

        ModelAndView mav = new ModelAndView("jsonView");
        Object credential = request.getSession().getAttribute("userCred");

        //Used to store Job Submission status, because there will be another request checking this.
		GridTransferStatus gridStatus = new GridTransferStatus();

        //Dont let the user proceed if they dont have grid credentials
        if (credential == null) {
            //final String errorString = "Invalid grid credentials!";
            logger.error(GridSubmitController.CREDENTIAL_ERROR);
            gridStatus.currentStatusMsg = GridSubmitController.CREDENTIAL_ERROR;
            gridStatus.jobSubmissionStatus = JobSubmissionStatus.Failed;
            
            // Save in session for status update request for this job.
            request.getSession().setAttribute("gridStatus", gridStatus);
            //mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }


        Date startDate = (Date)request.getSession().getAttribute("startDate");
        Date endDate = (Date)request.getSession().getAttribute("endDate");
        String remoteJobInputDir = (String)request.getSession().getAttribute("jobInputDir");
        String localJobInputDir = (String)request.getSession().getAttribute("localJobInputDir");

        //job.setArguments(new String[] { job.getScriptFile() });
        logger.info("args count: "+job.getArguments().length);
        job.setJobType(job.getJobType().replace(",", ""));
        JSONArray args = JSONArray.fromObject(request.getParameter("arguments"));
        logger.info("Args in Json : "+args.toArray().length);

        job.setArguments((String[])args.toArray(new String [args.toArray().length]));

        // Create a new directory for the output files of this job
        String jobID = generateJobDirectoryId(request) + File.separator;
        String jobOutputDir = gridAccess.getGridFtpServer() + gridAccess.getGridFtpStageOutDir() + jobID;
        logger.debug("jobOutputDir: "+jobOutputDir);

        // Add grid stage-in directory and local stage-in directory.
        String stageInURL = gridAccess.getGridFtpServer()+remoteJobInputDir;
        logger.debug("stageInURL: "+stageInURL);

        String localStageInURL = gridAccess.getLocalGridFtpServer() + (String) request.getSession().getAttribute("localJobInputDir");
        job.setInTransfers(new String[]{stageInURL,localStageInURL});

        logger.debug("localStageInURL: "+localStageInURL);

        String submitEPR = null;
        job.setEmailAddress(user);
        job.setEndDate(endDate);
        job.setStartDate(startDate);
        job.setRemoteInputDir(remoteJobInputDir);
        job.setRemoteOutputDir(jobOutputDir);
        job.setLocalInputDir(localJobInputDir);
        job.setOutTransfers(new String[] { jobOutputDir });

        logger.info("Submitting job with name " + job.getName() + " to " + job.getSite());

        // ACTION!
        if(success)
            submitEPR = gridAccess.submitJob(job, credential);

        if (submitEPR == null) {
            success = false;
            gridStatus.jobSubmissionStatus = JobSubmissionStatus.Failed;
            gridStatus.currentStatusMsg = GridSubmitController.INTERNAL_ERROR;
        } else {
            logger.info("SUCCESS! EPR: "+submitEPR);
            String status = gridAccess.retrieveJobStatus(submitEPR, credential);
            job.setReference(submitEPR);
            jobManager.saveJob(job);
            request.getSession().removeAttribute("jobInputDir");
            request.getSession().removeAttribute("localJobInputDir");

            //This means job submission to the grid done.
            gridStatus.jobSubmissionStatus = JobSubmissionStatus.Done;
            gridStatus.currentStatusMsg = GridSubmitController.TRANSFER_COMPLETE;
        }

        
        // Save in session for status update request for this job.
        request.getSession().setAttribute("gridStatus", gridStatus);
        mav.addObject("success", success);

        return mav;
    }

    

    /**
     * Generates a name for a directory that a job can use based on user name and timestamp
     * @param request
     * @return
     */
    private String generateJobDirectoryId(HttpServletRequest request) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String dateFmt = sdf.format(new Date());
        return request.getRemoteUser() + "-" + dateFmt;
    }

    /**
     * Creates a new Job object with predefined values for some fields.
     *
     * @param request The servlet request containing a session object
     *
     * @return The new job object.
     */
    private CMARJob prepareModel(HttpServletRequest request) {
        final String user = request.getRemoteUser();
        final String maxWallTime = "60"; // in minutes
        final String maxMemory = "2048"; // in MB
        final String stdInput = "";
        final String stdOutput = "stdOutput.txt";
        final String stdError = "stdError.txt";
        final String[] arguments = new String[0];
        final String[] inTransfers = new String[0];
        final String[] outTransfers = new String[0];
        String name = "CMARJob";
        String site = "TPAC";
        Integer cpuCount = 1;
        String version = "";
        String queue = "";
        String description = "";

        // Set a default version and queue
        String[] allVersions = gridAccess.retrieveCodeVersionsAtSite(
                site, CMARJob.CODE_NAME);
        if (allVersions.length > 0)
            version = allVersions[0];

        String[] allQueues = gridAccess.retrieveQueueNamesAtSite(site);
        if (allQueues.length > 0)
            queue = allQueues[0];

        // Create a job id to stamp our directories
        String jobID = generateJobDirectoryId(request) + File.separator;

        //create local and remote stage in directory structure
        String remoteJobInputDir = createRemoteDir(request, jobID);
        if(remoteJobInputDir == null){
        	logger.error("Setting up remote StageIn directory failed.");
        	return null;
        }
        //Create local stageIn directory. (Which will mirror the remote stage in directory)
        String localJobInputDir = createLocalDir(request, jobID);
        if(localJobInputDir == null){
        	logger.error("Setting up local StageIn directory failed.");
        	return null;
        }

        // Save the directories into the session for later use
        request.getSession().setAttribute("localJobInputDir", localJobInputDir);
        request.getSession().setAttribute("jobInputDir", remoteJobInputDir);
        


        logger.debug("Creating new CMARJob instance");
        CMARJob job = new CMARJob(site, name, version, arguments, queue,
                maxWallTime, maxMemory, cpuCount, inTransfers, outTransfers,
                user, stdInput, stdOutput, stdError);

        job.setDescription(description);

        return job;
    }

    /**
     * Create stageIn directories on the remote grid server (returns path on succes, null on failure)
     * @param request
     * @return
     */
    private String createRemoteDir(HttpServletRequest request, String jobId) {
        String jobInputDir = gridAccess.getGridFtpStageInDir() + jobId;

        boolean success =  createGridDir(request, jobInputDir);

        //create directory structure
        for (int i = 0; success && i < STAGE_IN_DIRECTORIES.length; i++) {
            success = createGridDir(request, jobInputDir + STAGE_IN_DIRECTORIES[i] + File.separator);
        }

        if (!success) {
            logger.error("Could not create remote stageIn directories ");
            return null;
        }

        return jobInputDir;
    }

	/** 
     * Create stageIn directories on portal host, so user can upload files easy.
     *
     * Returns the path to the directory created (or null on failure
     *
     */
	private String createLocalDir(HttpServletRequest request,String jobId) {
        String jobInputDir = gridAccess.getLocalGridFtpStageInDir() + jobId;
        
        boolean success = (new File(jobInputDir)).mkdir();
        
        //create directory structure
        for (int i = 0; success && i < STAGE_IN_DIRECTORIES.length; i++) {
            success = (new File(jobInputDir + STAGE_IN_DIRECTORIES[i] + File.separator)).mkdir();
        }

        //Create our "CSV" file 
        FileWriter fw = null;
        try {
            fw = new FileWriter(jobInputDir + WFS_CSV_FILE);
            fw.write((String)request.getSession().getAttribute("selectedCSV"));
        } catch (Exception ex) {
            logger.error("Error writing WFS CSV file: " + ex);
            success = false;
        } finally {
            try {
                if (fw != null)
                    fw.close();
            } catch (Exception ex) {
                logger.error("Error closing WFS CSV file: " + ex);
            }
        }

        if (success)
            success = Util.copyFilesRecursive(new File(PRE_STAGE_IN_CMAR_FILES), new File(jobInputDir));
        
        //tables files.
        /*success = Util.copyFilesRecursive(new File(GridSubmitController.PRE_STAGE_IN_TABLE_FILES),
        		                new File(jobInputDir+GridSubmitController.TABLE_DIR+File.separator));*/

        if (!success) {
            logger.error("Could not create local stageIn directories ");
            return null;
        }
        
        return jobInputDir;
	}

	/** 
     * Create subJob stageIn directory on portal host, so user can upload files easy.
     * This method is called each time the user is uploading a file for a multiJob.
     *
     */
	private boolean createLocalSubJobDir(HttpServletRequest request, String subJobInputDir, String fileType, String subJobId) {
		
        boolean success = false;
        File subJobFile = new File(subJobInputDir); //create if subJob directory does not exist
        success = subJobFile.exists();
        if(!success){
        	success = subJobFile.mkdir();
        	Hashtable localSubJobDir = (Hashtable) request.getSession().getAttribute("localSubJobDir");
        	
        	if(localSubJobDir == null)
        		localSubJobDir = new Hashtable();
        	
        	if(!localSubJobDir.containsKey(subJobId)){
        		localSubJobDir.put(subJobId, gridAccess.getLocalGridFtpServer()+subJobInputDir);
        		request.getSession().setAttribute("localSubJobDir", localSubJobDir);
        	}
        }

        if(fileType.equals(GridSubmitController.RINEX_DIR)){
            //create rinex directory for the subJob.
            File subJobRinexDir = new File(subJobInputDir+GridSubmitController.RINEX_DIR+File.separator);
            success = subJobRinexDir.exists();
            if(!success){
            	success = subJobRinexDir.mkdir();
            }     	
        }
        else{
            //create tables directory for the subJob.
            File subJobTablesDir = new File(subJobInputDir+GridSubmitController.TABLE_DIR+File.separator);
            success = subJobTablesDir.exists();
            if(!success){
                success = subJobTablesDir.mkdir();        	
            }	
        }
        
        if (!success) {
            logger.error("Could not create local subJobStageIn directories ");
        }
        
        return success;
	}
	/** 
	 * urlCopy
	 * 
     * Copy data to the Grid Storage using URLCopy.  
     * This is method which does authentication, remote create directory 
     * and files copying
     *
     * @param fromURLs	an array of URLs to copy to the storage
     * 
     * @return          GridTransferStatus of files copied
     * 
     */
	/*private GridTransferStatus urlCopy(String[] fromURLs, HttpServletRequest request) {

		Object credential = request.getSession().getAttribute("userCred");		
		String jobInputDir = (String) request.getSession().getAttribute("jobInputDir");				
        GridTransferStatus status = new GridTransferStatus();
		
		if( jobInputDir != null )
		{
			for (int i = 0; i < fromURLs.length; i++) {
				// StageIn to Grid etc
				
				try {
					GlobusURL from = new GlobusURL(fromURLs[i].replace("http://files.ivec.org/geodesy/", "gsiftp://pbstore.ivec.org:2811//pbstore/cg01/geodesy/ftp.ga.gov.au/"));
					logger.info("fromURL is: " + from.getURL());
					
					String fullFilename = new URL(fromURLs[i]).getFile();

					// Extract just the filename
					String filename = new File(fullFilename).getName();	
					status.file = filename;
					
					
					// Full URL
					// e.g. "gsiftp://pbstore.ivec.org:2811//pbstore/au01/grid-auscope/Abdi.Jama@csiro.au-20091103_163322/"
					//       +"rinex/" + "abeb0010.00d.Z"
					String toURL = gridAccess.getGridFtpServer()+File.separator+ jobInputDir 
					               +GridSubmitController.RINEX_DIR+File.separator+ filename;
					GlobusURL to = new GlobusURL(toURL);		
					logger.info("toURL is: " + to.getURL());
					
					//Not knowing how long UrlCopy will take, the UI request status update of 
					//file transfer periodically
					UrlCopy uCopy = new UrlCopy();
					uCopy.setCredentials((GSSCredential)credential);
					uCopy.setDestinationUrl(to);
					uCopy.setSourceUrl(from);
					// Disables usage of third party transfers, for grid security reasons.
					uCopy.setUseThirdPartyCopy(false); 	
					uCopy.copy();
										
					
					
					logger.info(to.getProtocol()+"://"+to.getHost()+":"+to.getPort()+"/");
					
					String gridServer = to.getProtocol() + "://" + to.getHost() + ":" + to.getPort();

					String gridFullURL =  gridServer + "/"+ jobInputDir;
										
					status.numFileCopied++;
					status.currentStatusMsg = GridSubmitController.FILE_COPIED + status.numFileCopied
					+" of "+fromURLs.length+" files transfered.";
					status.gridFullURL = gridFullURL;
					status.gridServer = gridServer;
					logger.debug(status.currentStatusMsg+" : "+fromURLs[i]);
					
					// Save in session for status update request for this job.
			        request.getSession().setAttribute("gridStatus", status);
					
				} catch (UrlCopyException e) {
					logger.error("UrlCopy Error: " + e.getMessage());
					status.numFileCopied = i;
					status.currentStatusMsg = GridSubmitController.FILE_COPY_ERROR;
					status.jobSubmissionStatus = JobSubmissionStatus.Failed;
					// Save in session for status update request for this job.
			        request.getSession().setAttribute("gridStatus", status);
				} catch (Exception e) {
					logger.error("Error: " + e.getMessage());
					status.numFileCopied = i;
					status.currentStatusMsg = GridSubmitController.INTERNAL_ERROR;
					status.jobSubmissionStatus = JobSubmissionStatus.Failed;
					// Save in session for status update request for this job.
			        request.getSession().setAttribute("gridStatus", status);
				}
			}
		}
		
		return status;
	}*/	
	/** 
	 * urlCopy
	 * 
     * Copy data to the Grid Storage using URLCopy.  
     * This is method which does authentication, remote create directory 
     * and files copying
     *
     * @param fromURLs	an array of URLs to copy to the storage
     * 
     * @return          GridTransferStatus of files copied
     * 
     */
	private GridTransferStatus urlCopy(String[] fromURLs, HttpServletRequest request) {

		Object credential = request.getSession().getAttribute("userCred");		
		String jobInputDir = (String) request.getSession().getAttribute("jobInputDir");				
        GridTransferStatus status = new GridTransferStatus();
		
		if( jobInputDir != null )
		{
			for (int i = 0; i < fromURLs.length; i++) {
				// StageIn to Grid etc
				int rtnValue = urlCopy(fromURLs[i], credential, jobInputDir, status );
				//This means time-out issue exception, so retry 2 more times.
				if(rtnValue == 1){
					logger.info("UrlCopy timed-out retry 2 for: " + fromURLs[i]);
					rtnValue = urlCopy(fromURLs[i], credential, jobInputDir, status );
					if(rtnValue == 1){
						logger.info("UrlCopy timed-out retry 3 for: " + fromURLs[i]);
						rtnValue = urlCopy(fromURLs[i], credential, jobInputDir, status );
						if(rtnValue == 1){
							status.numFileCopied = i;
							status.currentStatusMsg = GridSubmitController.FILE_COPY_ERROR;
							status.jobSubmissionStatus = JobSubmissionStatus.Failed;
							// Save in session for status update request for this job.
					        request.getSession().setAttribute("gridStatus", status);
					        logger.error("UrlCopy retry timed-out for: " + fromURLs[i]);
							break;
						}
					}
				}
				
				//This means bad exception like network error, so quit.
				if(rtnValue == 2){
					request.getSession().setAttribute("gridStatus", status);
					break;
				}
				
				status.numFileCopied++;
				status.currentStatusMsg = GridSubmitController.FILE_COPIED + status.numFileCopied
				+" of "+fromURLs.length+" files transfered.";
				logger.debug(status.currentStatusMsg+" : "+fromURLs[i]);
				
				// Save in session for status update request for this job.
		        request.getSession().setAttribute("gridStatus", status);															
			}
		}
		
		return status;
	}

	/**
     * Copy file to the Grid Storage using URLCopy.  
     * This is method which does authentication and transfers the file.
	 * @param fileUri The file to transfer
	 * @param credential Credential to use
	 * @param jobInputDir Path to copy to
	 * @param status holds status of the transfer
	 * @return
	 */
    private int urlCopy(String fileUri, Object credential, String jobInputDir, GridTransferStatus status ) 
    {
        int rtnValue = 0;
        try {
    		GlobusURL from = new GlobusURL(fileUri.replace("http://files.ivec.org/geodesy/", "gsiftp://pbstore.ivec.org:2811//pbstore/cg01/geodesy/ftp.ga.gov.au/"));
    		logger.info("fromURL is: " + from.getURL());
    		
    		String fullFilename = new URL(fileUri).getFile();

    		// Extract just the filename
    		String filename = new File(fullFilename).getName();	
    		status.file = filename;   		
    		
    		// Full URL
    		// e.g. "gsiftp://pbstore.ivec.org:2811//pbstore/au01/grid-auscope/Abdi.Jama@csiro.au-20091103_163322/"
    		//       +"rinex/" + "abeb0010.00d.Z"
    		String toURL = gridAccess.getGridFtpServer()+File.separator+ jobInputDir 
    		               +GridSubmitController.RINEX_DIR+File.separator+ filename;
    		GlobusURL to = new GlobusURL(toURL);		
    		logger.info("toURL is: " + to.getURL());
    		
    		//Not knowing how long UrlCopy will take, the UI request status update of 
    		//file transfer periodically
    		UrlCopy uCopy = new UrlCopy();
    		uCopy.setCredentials((GSSCredential)credential);
    		uCopy.setDestinationUrl(to);
    		uCopy.setSourceUrl(from);
    		// Disables usage of third party transfers, for grid security reasons.
    		uCopy.setUseThirdPartyCopy(false); 	
    		uCopy.copy();   		
    	} catch (UrlCopyException e) {
    		logger.info("UrlCopy timed-out: " + e.getMessage());
    		rtnValue = 1;
    	} catch (Exception e) {
    		logger.error("Error: " + e.getMessage());
    		status.currentStatusMsg = GridSubmitController.INTERNAL_ERROR;
    		status.jobSubmissionStatus = JobSubmissionStatus.Failed;
    		rtnValue = 2;
    	}    	   
        return rtnValue;
    }

    /**
     * Create a stageIn directories on Pbstore. If any errors update status.
     * @param the request to save created directories.
     * 
     */
	private boolean createGridDir(HttpServletRequest request, String myDir) {
		GridTransferStatus status = new GridTransferStatus();
        Object credential = request.getSession().getAttribute("userCred");
        boolean success = true;
        if (credential == null) {
        	status.currentStatusMsg = GridSubmitController.CREDENTIAL_ERROR;
        	return false;
        }
        		
		try {
			GridFTPClient gridStore = new GridFTPClient(gridAccess.getRepoHostName(), gridAccess.getRepoHostFTPPort());		
			gridStore.authenticate((GSSCredential)credential); //authenticating
			gridStore.setDataChannelAuthentication(DataChannelAuthentication.SELF);
			gridStore.setDataChannelProtection(GridFTPSession.PROTECTION_SAFE);
			gridStore.makeDir(myDir);

	        logger.debug("Created Grid Directory.");
	        gridStore.close();
			
		} catch (ServerException e) {
			logger.error("GridFTP ServerException: " + e.getMessage());
			status.currentStatusMsg = GridSubmitController.GRID_LINK;
			status.jobSubmissionStatus = JobSubmissionStatus.Failed;
			success = false;
		} catch (IOException e) {
			logger.error("GridFTP IOException: " + e.getMessage());
			status.currentStatusMsg = GridSubmitController.GRID_LINK;
			status.jobSubmissionStatus = JobSubmissionStatus.Failed;
			success = false;
		} catch (Exception e) {
			logger.error("GridFTP Exception: " + e.getMessage());
			status.currentStatusMsg = GridSubmitController.GRID_LINK;
			status.jobSubmissionStatus = JobSubmissionStatus.Failed;
			success = false;
		}
		
		// Save in session for status update request for this job.
        request.getSession().setAttribute("gridStatus", status);
        return success;
	}

	/**
	 * function that moves local GPS files at ivec to a separate list.
	 * @param list of selected GPS files
	 * @return list of local GPS files.
	 */
	private List<String> getLocalGPSFiles(List<String> list){
		List<String> ivecList = new ArrayList<String>();
		for(String fileName : list){
			if (fileName.contains(".ivec.org")){
				ivecList.add(convertFilePathToIvec(fileName));
				//The file can not be in two list
				list.remove(fileName);				
			}
		}		
		return ivecList;
	}

	/**
	 * 
	 * @param fileName file which to change it's path name
	 * @return
	 */
	private String convertFilePathToIvec(String fileName){
		//replace "http://files.ivec.org/geodesy/"  
		//with "gsiftp://pbstore.ivec.org:2811//pbstore/cg01/geodesy/ftp.ga.gov.au/gpsdata/"
		return fileName.replace(IVEC_MIRROR_URL, gridAccess.getGridFtpServer()+PBSTORE_RINEX_PATH);
	}

	/**
	 * function that checks the file type is rinex or not
	 * @param fileName
	 * @return
	 */
	private String checkFileType(String fileName){
	  String rtnValue = null;
	  if(fileName.trim().toLowerCase().endsWith(".z")){
		  logger.debug("file is of rinex.");
		  rtnValue = GridSubmitController.RINEX_DIR;
	  }else{
		  logger.debug("file is of tables.");
		  rtnValue = GridSubmitController.TABLE_DIR;
	  }
	  return rtnValue;
	}
	/**
	 * Funtion th
	 * @param files
	 * @param dir
	 * @param subJob
	 */
	private void addFileNamesOfDirectory(List files, File dir, String subJob, String filePath){
        String fileNames[] = dir.list();
        logger.debug("Inside listJobFiles.do adding files.");
        for (int i=0; i<fileNames.length; i++) {
            File f = new File(dir, fileNames[i]);
            FileInformation fileInfo = new FileInformation(fileNames[i], f.length(), subJob);
            fileInfo.setParentPath(filePath);
            logger.debug("File path is:"+filePath);
            files.add(fileInfo);     
        }
	}

    /**
     * This method using GridFTP Client returns directory list of stageOut directory 
     * and sub directories.
     * @param fullDirname
     * @param credential
     * @return
     */
	private FileInformation[] getDirectoryListing(String fullDirname, Object credential){
		GridFTPClient gridStore = null;
		FileInformation[] fileDetails = new FileInformation[0];
		try {
			gridStore = new GridFTPClient(gridAccess.getRepoHostName(), gridAccess.getRepoHostFTPPort());		
			gridStore.authenticate((GSSCredential)credential); //authenticating
			gridStore.setDataChannelAuthentication(DataChannelAuthentication.SELF);
			gridStore.setDataChannelProtection(GridFTPSession.PROTECTION_SAFE);
			logger.debug("Change to Grid StageOut dir:"+fullDirname);
			gridStore.changeDir(fullDirname);
			logger.debug("List files in StageOut dir:"+gridStore.getCurrentDir());
			gridStore.setType(GridFTPSession.TYPE_ASCII);
			gridStore.setPassive();
			gridStore.setLocalActive();
			
			Vector list = gridStore.list("*");

			if (list != null && !(list.isEmpty())) {
				fileDetails = new FileInformation[list.size()];
				for (int i = list.size() - 1; i >= 0; i--) {
					FileInfo fInfo = (FileInfo) list.get(i);
		            fileDetails[i] = new FileInformation(
		            		fInfo.getName(), fInfo.getSize(), fullDirname, fInfo.isDirectory());
				}                    
			} 
		} catch (ServerException e) {
			logger.error("GridFTP ServerException: " + e.getMessage());
		} catch (IOException e) {
			logger.error("GridFTP IOException: " + e.getMessage());
		} catch (Exception e) {
			logger.error("GridFTP Exception: " + e.getMessage());
		}
		finally{
			try{
				if(gridStore != null)
					gridStore.close();
			}catch (Exception e) {
				logger.error("GridFTP Exception: " + e.getMessage());
			}
		}
		return fileDetails;
	}	
	/**
	 * Simple object to hold Grid file transfer status.
	 * @author jam19d
	 *
	 */
	class GridTransferStatus {
		
		public int numFileCopied = 0;
		public String file = "";
		public String gridFullURL = "";
		public String gridServer = "";
		public String currentStatusMsg = "";
		public JobSubmissionStatus jobSubmissionStatus = JobSubmissionStatus.Running;				
	}
	

	/**
	 * Enum to indicate over all job submission status.
	 */
	public enum JobSubmissionStatus{Running,Done,Failed }
}