/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.auscope.portal.server.gridjob;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author VOT002
 */
public class CMARJobManager {
    protected final Log logger = LogFactory.getLog(getClass());

    private CMARJobDao cmarJobDao;

    public void setCmarJobDao(CMARJobDao cmarJobDao) {
        this.cmarJobDao = cmarJobDao;
    }


    public CMARJob getJobById(int jobId) {
        return cmarJobDao.get(jobId);
    }

    public void deleteJob(CMARJob job) {
        cmarJobDao.deleteJob(job);
    }

    public void saveJob(CMARJob geodesyJob) {
        cmarJobDao.save(geodesyJob);
    }

}

