/*
 * This file is part of the AuScope Virtual Rock Lab (VRL) project.
 * Copyright (c) 2009 ESSCC, The University of Queensland
 *
 * Licensed under the terms of the GNU Lesser General Public License.
 */
package org.auscope.portal.server.gridjob;

import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import org.auscope.gridtools.GridJob;

/**
 * Simple class to represent a CMAR prawn/bran job specific to the workflow (Insert better comment here).
 *
 * @author Josh Vote
 */
public class CMARJob extends GridJob {
    public static final String STATUS_UNSUBMITTED = "UNSUBMITTED";
    public static final String STATUS_SUBMITTED_R = "SUBMITTED_R";
    public static final String STATUS_COMPLETE = "COMPLETE";

    /** The name of the software to run Geodesy jobs with */
    public static final String CODE_NAME = "R";

    /** The job type */
    private static final String JOB_TYPE = "single";

    /** A description for this job */
    private String    description;
    /** A unique identifier for this job */
    private Integer   id;
    /** Directory containing output files */
    private String    workingDir;
    /** A unique job reference (e.g. EPR). */
    private String    reference;
    /** The descriptive job title */
    private String    name;

    /** The site the code is running at */
    private String    site;
    /** The version of code to run at */
    private String    version;

    /** The job start date */
    private Date startDate;

    /** The job end date */
    private Date endDate;

    /** The current status of the job*/
    private String jobStatus;

    /** The list of species this job references*/
    private String[] speciesCodes;






	/**
     * Does some basic setting up of the class variables to prevent errors.
     * Strings are initialized to the empty String, and integers are
     * initialized with zero.
     */
    public CMARJob() {
        super();
        description = workingDir = reference = name = site = version = jobStatus = "";
        id = 0;
        speciesCodes = new String[0];
        startDate = endDate = new Date();
        setCode(CODE_NAME);
        setJobType(JOB_TYPE);
    }

    /**
     * Alternate constructor initializing <code>GridJob</code> members.
     *
     * @param site              The site the job will be run at
     * @param name              A descriptive name for this job
     * @param version           Version of code to use
     * @param arguments         Arguments for the code
     * @param queue             Which queue to use
     * @param maxWallTime       Amount of time we plan to use
     * @param maxMemory         Amount of memory we plan to use
     * @param cpuCount          Number of CPUs to use (if jobType is single)
     * @param inTransfers       Files to be transferred in
     * @param outTransfers      Files to be transferred out
     * @param emailAddress      The email address for PBS notifications
     * @param stdInput          The std input file for the job
     * @param stdOutput         The std output file for the job
     * @param stdError          The std error file for the job
     */
    public CMARJob(String site, String name, String version, String[] arguments,
                  String queue, String maxWallTime, String maxMemory,
                  Integer cpuCount, String[] inTransfers,
                  String[] outTransfers, String emailAddress, String stdInput,
                  String stdOutput, String stdError)
    {
        super(site, name, CODE_NAME, version, arguments, queue, JOB_TYPE,
                maxWallTime, maxMemory, cpuCount, inTransfers, outTransfers,
                emailAddress, stdInput, stdOutput, stdError);
        description = workingDir = reference = name = site = version = jobStatus = "";
        id = 0;
        speciesCodes = new String[0];
        startDate = endDate = new Date();
    }


    /**
     * Returns the unique identifier of this job.
     *
     * @return The ID of this job.
     */
    public Integer getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this job.
     *
     * @param id The unique ID of this job.
     */
    private void setId(Integer id) {
        assert (id != null);
        this.id = id;
    }

    /**
     * Returns the description of this job.
     *
     * @return The description of this job.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this job.
     *
     * @param description The description of this job.
     */
    public void setDescription(String description) {
        assert (description != null);
        this.description = description;
    }


    /**
     * Returns the unique reference of this job.
     *
     * @return The reference of this job.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Sets the unique reference of this job.
     *
     * @param reference The unique reference of this job.
     */
    public void setReference(String reference) {
        assert (reference != null);
        this.reference = reference;
    }

    /**
     * Returns the status of this job.
     *
     * @return The status of this job.
     */
    public String getJobStatus() {
        return jobStatus;
    }

    /**
     * Sets the status of this job.
     *
     * @param status The status of this job.
     */
    public void setJobStatus(String status) {
        assert (status != null);
        this.jobStatus = status;
    }public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String[] getSpeciesCodes() {
        return speciesCodes;
    }

    public void setSpeciesCodes(String[] speciesCodes) {
        this.speciesCodes = speciesCodes;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public String getWorkingDir() {
        return workingDir;
    }

    public void setWorkingDir(String workingDir) {
        this.workingDir = workingDir;
    }

	/**
     * Returns a String representing the state of this <code>GeodesyJob</code>
     * object.
     *
     * @return A summary of the values of this object's fields
     */
    public String toString() {
        return super.toString() +
               ", id=" + id +
               ", name=" + name +
               ", description=\"" + description + "\"" +
               ", status=\"" + jobStatus + "\"";
    }
}

