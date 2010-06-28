package org.auscope.portal.server.web.controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CTDPlotController {

	static final String IMAGE_TYPE = "png";

	@RequestMapping("/getCTDPlot.do")
	public void gridLogin(HttpServletRequest request,
			HttpServletResponse response, String stationId) throws IOException{

		// the output stream to render the captcha image as jpeg into
		ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
		try {

			BufferedImage img = null;

			ImageIO.write(img, IMAGE_TYPE, pngOutputStream);
		} catch (IllegalArgumentException e) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		} catch (Exception e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			return;
		}

		// flush it in the response
		response.setHeader("Cache-Control", "no-store");
		response.setHeader("Pragma", "no-cache");
		response.setDateHeader("Expires", 0);
		response.setContentType("image/" + IMAGE_TYPE);

		ServletOutputStream responseOutputStream = response.getOutputStream();
		responseOutputStream.write(pngOutputStream.toByteArray());
		responseOutputStream.flush();
		responseOutputStream.close();

	}

}
