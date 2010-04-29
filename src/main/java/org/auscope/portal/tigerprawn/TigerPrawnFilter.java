/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.auscope.portal.tigerprawn;

import org.auscope.portal.mineraloccurrence.IFilter;



/**
 *
 * @author VOT002
 */
public class TigerPrawnFilter implements IFilter {
    private String startDate;
    private String endDate;

    /**
     * Given a mine name, this object will build a filter to do a PropertyIsLike serach for mine names
     *
     * @param mineName
     */
    public TigerPrawnFilter(String startDate, String endDate) {
        this.startDate = startDate;
        this.endDate = endDate;
        //this.kvps = "service=WFS&version=1.1.0&request=GetFeature&typeName=mo:Mine&namespace=xmlns(mo=urn:cgi:xmlns:GGIC:MineralOccurrence:1.0)";
    }


    public String getFilterString() {
        return  "        <ogc:Filter " +
                "                   xmlns:ogc=\"http://www.opengis.net/ogc\" " +
                "                   xmlns:gml=\"http://www.opengis.net/gml\" " +
                "                   xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "                   xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "                   xmlns:auscope=\"http://datacentre.csiro.au/gecko/auscope\"\n" +



                "                <ogc:PropertyIsBetween>\n" +
                "                    <ogc:PropertyName>auscope:START_DATETIME</ogc:PropertyName>\n" +
                "                    <ogc:LowerBoundary><ogc:Literal>" + startDate + "</ogc:Literal></ogc:LowerBoundary>\n" +
                "                    <ogc:UpperBoundary><ogc:Literal>" + endDate + "</ogc:Literal></ogc:UpperBoundary>\n" +
                "                </ogc:PropertyIsBetween>\n" +
                "        </ogc:Filter>";

    }

}