/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2009 ESSCC, The University of Queensland
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.portal.server.web.controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.ServerException;
import java.util.List;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.auscope.portal.server.gridjob.CMARJobManager;

import org.auscope.portal.server.gridjob.FileInformation;
import org.auscope.portal.server.gridjob.GridAccessController;
import org.auscope.portal.server.gridjob.Util;
import org.auscope.portal.server.gridjob.CMARJob;
import org.auscope.portal.server.gridjob.GeodesySeries;
import org.globus.ftp.DataChannelAuthentication;
import org.globus.ftp.GridFTPClient;
import org.globus.ftp.GridFTPSession;
import org.ietf.jgss.GSSCredential;
import org.globus.ftp.FileInfo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Controller for the job list view.
 *
 * @author Cihan Altinay
 * @author Abdi Jama
 */
@Controller
public class JobListController {

    /** Logger for this class */
    private final Log logger = LogFactory.getLog(getClass());

    @Autowired
    private GridAccessController gridAccess;
    @Autowired
    private CMARJobManager jobManager;

    /**
     * Sets the <code>GridAccessController</code> to be used for grid
     * activities.
     *
     * @param gridAccess the GridAccessController to use
     */
     //public void setGridAccess(GridAccessController gridAccess) {
     //   this.gridAccess = gridAccess;
     //}

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
	        return new ModelAndView("joblist");
	    } else {
	        request.getSession().setAttribute(
	                "redirectAfterLogin", "/joblist.html");
	        logger.warn("Proxy not initialized. Redirecting to gridLogin.");
	        return new ModelAndView(
	                new RedirectView("/gridLogin.html", true, false, false));
	    }
    }*/

    /**
     * Triggers the retrieval of latest job files
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found in the job manager.
     */
    @RequestMapping("/retrieveJobFiles.do")
    public ModelAndView retrieveJobFiles(HttpServletRequest request,
                                         HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        CMARJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }

        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        } else {
            logger.warn("No job ID specified!");
        }

        if (job == null) {
            final String errorString = "The requested job was not found.";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);

        } else {
            logger.debug("jobID = " + jobIdStr);
            boolean success = false;
            String jobState = gridAccess.retrieveJobStatus(
                    job.getReference(), credential);
            if (jobState != null && jobState.equals("Active")) {
                success = gridAccess.retrieveJobResults(
                        job.getReference(), credential);
            } else {
                mav.addObject("error", "Cannot retrieve files of a job that is not running!");
            }
            logger.debug("Success = "+success);
            mav.addObject("success", success);
        }

        return mav;
    }
    /**
     * Delete the job given by its reference.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found or can not be deleted.
     */
    @RequestMapping("/deleteJob.do")
    public ModelAndView deleteJob(HttpServletRequest request,
                                HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        CMARJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success = false;
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }


        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        } else {
            logger.warn("No job ID specified!");
        }


        //Disabled
        mav.addObject("success", false);

        return mav;
    }
    /**
     * delete all jobs of given series.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the series was not found in the job manager.
     */
    @RequestMapping("/deleteSeriesJobs.do")
    public ModelAndView deleteSeriesJobs(HttpServletRequest request,
                                       HttpServletResponse response) {

        String seriesIdStr = request.getParameter("seriesId");
        List<CMARJob> jobs = null;
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success = false;
        int seriesId = -1;
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }


        

        //disabled
        mav.addObject("success", false);
        return mav;
    }
    
    /**
     * Kills the job given by its reference.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the job was not found in the job manager.
     */
    @RequestMapping("/killJob.do")
    public ModelAndView killJob(HttpServletRequest request,
                                HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        CMARJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success = false;
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }


        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        } else {
            logger.warn("No job ID specified!");
        }

        //Disabled
        mav.addObject("success", false);

        return mav;
    }

    /**
     * Kills all jobs of given series.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a success attribute and an error attribute
     *         in case the series was not found in the job manager.
     */
    @RequestMapping("/killSeriesJobs.do")
    public ModelAndView killSeriesJobs(HttpServletRequest request,
                                       HttpServletResponse response) {

        String seriesIdStr = request.getParameter("seriesId");
        List<CMARJob> jobs = null;
        ModelAndView mav = new ModelAndView("jsonView");
        boolean success = false;
        int seriesId = -1;
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }


        //Disabled
        mav.addObject("success", false);
        return mav;
    }

    /**
     * Returns a JSON object containing an array of files belonging to a
     * given job.
     *
     * @param request The servlet request including a jobId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a files attribute which is an array of
     *         FileInformation objects. If the job was not found in the job
     *         manager the JSON object will contain an error attribute
     *         indicating the error.
     */
    @RequestMapping("/jobFiles.do")
    public ModelAndView jobFiles(HttpServletRequest request,
                                 HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String dirPathStr = request.getParameter("dirPath");
        String dirNameStr = request.getParameter("dirName");
        CMARJob job = null;
        ModelAndView mav = new ModelAndView("jsonView");
        Object credential = request.getSession().getAttribute("userCred");

        if (credential == null) {
        	logger.error("Error invalid credential.");
        }
        
        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        } else {
            logger.warn("No job ID specified!");
        }
        
        FileInformation[] fileDetails = new FileInformation[0];
        if (job == null) {
            final String errorString = "The requested job was not found.";
            logger.error(errorString);
            mav.addObject("error", errorString);

        } else {
        	if(dirPathStr == null){
        		fileDetails = getDirectoryListing(job.getRemoteOutputDir(), credential);
        	}else{
        		if(dirNameStr == null || dirNameStr.equals(".")){
        		   fileDetails = getDirectoryListing(dirPathStr, credential);
        		}else if(dirNameStr.equals("..")){
        			// This is the top directory for this job, cannot allow further up
        			if(dirPathStr.equals(job.getRemoteOutputDir()))
        				fileDetails = getDirectoryListing(dirPathStr, credential);
        			else
        			{
        		        //Going one directory up
        				String tempDir = dirPathStr.substring(0, (dirPathStr.length() -1));
        		        fileDetails = getDirectoryListing(tempDir.substring(0, (tempDir.lastIndexOf("/")+1)), credential);
        			}
        		}else{        		   
        		   fileDetails = getDirectoryListing(dirPathStr+dirNameStr+"/", credential);
        		}
        	}
        }
        mav.addObject("files", fileDetails);
    	return mav;
    }
    

    /**
     * Sends the contents of a job file to the client.
     *
     * @param request The servlet request including a jobId parameter and a
     *                filename parameter
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist view with an error parameter on
     *         failure.
     */
    @RequestMapping("/downloadFile.do")
    public ModelAndView downloadFile(HttpServletRequest request,
                                     HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String fileName = request.getParameter("filename");
        CMARJob job = null;
        String errorString = null;

        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (job != null && fileName != null) {
            logger.debug("Download "+fileName+" of job with ID "+jobIdStr+".");
            File f = new File(job.getRemoteOutputDir()+File.separator+fileName);
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
            if (job == null) {
                errorString = new String("Invalid job specified!");
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
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Sends the contents of one or more job files as a ZIP archive to the
     * client.
     *
     * @param request The servlet request including a jobId parameter and a
     *                files parameter with the filenames separated by comma
     * @param response The servlet response receiving the data
     *
     * @return null on success or the joblist view with an error parameter on
     *         failure.
     */
    @RequestMapping("/downloadAsZip.do")
    public ModelAndView downloadAsZip(HttpServletRequest request,
                                      HttpServletResponse response) {

        String jobIdStr = request.getParameter("jobId");
        String filesParam = request.getParameter("files");
        CMARJob job = null;
        String errorString = null;

        if (jobIdStr != null) {
            try {
                int jobId = Integer.parseInt(jobIdStr);
                job = jobManager.getJobById(jobId);
            } catch (NumberFormatException e) {
                logger.error("Error parsing job ID!");
            }
        }

        if (job != null && filesParam != null) {
            String[] fileNames = filesParam.split(",");
            logger.debug("Archiving " + fileNames.length + " file(s) of job " +
                    jobIdStr);

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition",
                    "attachment; filename=\"jobfiles.zip\"");

            try {
                boolean readOneOrMoreFiles = false;
                ZipOutputStream zout = new ZipOutputStream(
                        response.getOutputStream());
                for (String fileName : fileNames) {
                    File f = new File(job.getRemoteOutputDir()+File.separator+fileName);
                    if (!f.canRead()) {
                        // if a file could not be read we go ahead and try the
                        // next one.
                        logger.error("File "+f.getPath()+" not readable!");
                    } else {
                        byte[] buffer = new byte[16384];
                        int count = 0;
                        zout.putNextEntry(new ZipEntry(fileName));
                        FileInputStream fin = new FileInputStream(f);
                        while ((count = fin.read(buffer)) != -1) {
                            zout.write(buffer, 0, count);
                        }
                        zout.closeEntry();
                        readOneOrMoreFiles = true;
                    }
                }
                if (readOneOrMoreFiles) {
                    zout.finish();
                    zout.flush();
                    zout.close();
                    return null;

                } else {
                    zout.close();
                    errorString = new String("Could not access the files!");
                    logger.error(errorString);
                }

            } catch (IOException e) {
                errorString = new String("Could not create ZIP file: " +
                        e.getMessage());
                logger.error(errorString);
            }
        }

        // We only end up here in case of an error so return a suitable message
        if (errorString == null) {
            if (job == null) {
                errorString = new String("Invalid job specified!");
                logger.error(errorString);
            } else if (filesParam == null) {
                errorString = new String("No filename(s) provided!");
                logger.error(errorString);
            } else {
                // should never get here
                errorString = new String("Something went wrong.");
                logger.error(errorString);
            }
        }
        return new ModelAndView("joblist", "error", errorString);
    }

    /**
     * Returns a JSON object containing an array of jobs for the given series.
     *
     * @param request The servlet request including a seriesId parameter
     * @param response The servlet response
     *
     * @return A JSON object with a jobs attribute which is an array of
     *         <code>CMARJob</code> objects.
     */
    @RequestMapping("/listJobs.do")
    public ModelAndView listJobs(HttpServletRequest request,
                                 HttpServletResponse response) {

        String seriesIdStr = request.getParameter("seriesId");
        List<CMARJob> seriesJobs = null;
        ModelAndView mav = new ModelAndView("jsonView");
        Object credential = request.getSession().getAttribute("userCred");
        int seriesId = -1;

        if (credential == null) {
            final String errorString = "Invalid grid credentials!";
            logger.error(errorString);
            mav.addObject("error", errorString);
            mav.addObject("success", false);
            return mav;
        }

       
            
        
        //Disabled
        logger.debug("Returning series job list");
        return mav;
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
	
	private boolean directoryExist(String fullDirname, Object credential){
		GridFTPClient gridStore = null;
		boolean rtnValue = true;
		try {
			gridStore = new GridFTPClient(gridAccess.getRepoHostName(), gridAccess.getRepoHostFTPPort());		
			gridStore.authenticate((GSSCredential)credential); //authenticating
			gridStore.setDataChannelAuthentication(DataChannelAuthentication.SELF);
			gridStore.setDataChannelProtection(GridFTPSession.PROTECTION_SAFE);
			logger.debug("Change to Grid StageOut dir:"+fullDirname);
			gridStore.changeDir(fullDirname);
			
		} catch (ServerException e) {
			logger.error("GridFTP ServerException: " + e.getMessage());
			rtnValue = false;
		} catch (IOException e) {
			logger.error("GridFTP IOException: " + e.getMessage());
			rtnValue = false;
		} catch (Exception e) {
			logger.error("GridFTP Exception: " + e.getMessage());
			rtnValue = false;
		}
		finally{
			try{
				if(gridStore != null)
					gridStore.close();
			}catch (Exception e) {
				logger.error("GridFTP Exception: " + e.getMessage());
			}
		}
		return rtnValue;		
	}
}

