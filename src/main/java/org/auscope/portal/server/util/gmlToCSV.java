/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.auscope.portal.server.util;

import java.io.*;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.beans.factory.annotation.Qualifier;


import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import org.springframework.ui.ModelMap;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * This class is specifc to the CMAR Portal and as such contains many hardcoded relationshisps (was done in a hurry)
 * @author Josh Vote
 */
public class gmlToCSV {
   private static final Log log = LogFactory.getLog(GmlToKml.class);

   @Autowired
   @Qualifier(value = "propertyConfigurer")
   private PortalPropertyPlaceholderConfigurer hostConfigurer;

   /**
    * Utility method to transform xml file into a CSV file stream at out
    */
   public void convert(String geoXML, StringWriter out) {
      log.debug("GML input: " + geoXML);

      try {
         XPath xPath = XPathFactory.newInstance().newXPath();
         DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
         DocumentBuilder builder = factory.newDocumentBuilder();
         Document responseDocument = builder.parse(new InputSource(new StringReader(geoXML)));

         out.write("ID,SPECIES,START_DATETIME,SHOT_TIME,LATITUDE,LONGITUDE,TRAWL_DURATION,CATCH_COUNT\n");

         String featureMemberExpression = "/FeatureCollection/featureMembers/TIGER_CATCH";
         NodeList nodes = (NodeList) xPath.evaluate(featureMemberExpression, responseDocument, XPathConstants.NODESET);
         if (nodes != null) {
            for(int i=0; i < nodes.getLength(); i++ ) {
	            Node tempNode = (Node) xPath.evaluate("./START_DATETIME", nodes.item(i), XPathConstants.NODE);
	            final String startDate = (tempNode != null) ? tempNode.getTextContent() : "";

	            tempNode = (Node) xPath.evaluate("./SHOT_TIME", nodes.item(i), XPathConstants.NODE);
	            final String shotTime = (tempNode != null) ? tempNode.getTextContent() : "";

                tempNode = (Node) xPath.evaluate("./LATITUDE", nodes.item(i), XPathConstants.NODE);
	            final String latitude = (tempNode != null) ? tempNode.getTextContent() : "";

                tempNode = (Node) xPath.evaluate("./LONGITUDE", nodes.item(i), XPathConstants.NODE);
	            final String longitude = (tempNode != null) ? tempNode.getTextContent() : "";

                tempNode = (Node) xPath.evaluate("./TRAWL_DURATION", nodes.item(i), XPathConstants.NODE);
	            final String trawlDuration = (tempNode != null) ? tempNode.getTextContent() : "";

                tempNode = (Node) xPath.evaluate("./CATCH_COUNT", nodes.item(i), XPathConstants.NODE);
	            final String catchCount = (tempNode != null) ? tempNode.getTextContent() : "";

                //ID and species are compounded to make an ID
                String rawIDString = (String) xPath.evaluate("@id", nodes.item(i), XPathConstants.STRING);
                if (rawIDString == null)
                    rawIDString = "";

                String[] idComponents = rawIDString.split("\\.");
                if (idComponents.length != 3)
                    continue;

                //Write the CSV row
                out.write(idComponents[1]); //ID
                out.write(",");
                out.write(idComponents[2]); //Species
                out.write(",");
                out.write(startDate);
                out.write(",");
                out.write(shotTime);
                out.write(",");
                out.write(latitude);
                out.write(",");
                out.write(longitude);
                out.write(",");
                out.write(trawlDuration);
                out.write(",");
                out.write(catchCount);
                out.write("\n");
	        }
        }

      } catch (Exception e) {
         log.error(e);
      }
   }
}
