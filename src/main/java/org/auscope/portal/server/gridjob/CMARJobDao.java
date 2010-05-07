/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.auscope.portal.server.gridjob;

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 *
 * @author VOT002
 */
public class CMARJobDao extends HibernateDaoSupport {
    protected final Log logger = LogFactory.getLog(getClass());


    /**
     * Retrieves jobs that belong to a specific user
     *
     * @param user the user whose jobs are to be retrieved
     *
     * @return list of <code>GeodesyJob</code> objects belonging to given user
     */
    public List<CMARJob> getJobsByUser(final String user) {
        return (List<CMARJob>) getHibernateTemplate()
            .findByNamedParam("from CMARJob j where j.user=:searchUser",
                    "searchUser", user);
        /*
        return sessionFactory.getCurrentSession()
            .createQuery("from jobs j where j.user=:searchUser")
            .setString("searchUser", user)
            .list();
        */
    }

    /**
     * Retrieves the job with given ID.
     *
     * @return <code>GeodesyJob</code> object with given ID.
     */
    public CMARJob get(final int id) {
        return (CMARJob) getHibernateTemplate().get(CMARJob.class, id);
    }


    /**
     * Deletes the job with given ID.
     *
     * @return <code>GeodesyJob</code> object with given ID.
     */
    public void deleteJob(final CMARJob job) {
        getHibernateTemplate().delete(job);
    }

    /**
     * Saves or updates the given job.
     */
    public void save(final CMARJob job) {
        getHibernateTemplate().saveOrUpdate(job);
    }
}
