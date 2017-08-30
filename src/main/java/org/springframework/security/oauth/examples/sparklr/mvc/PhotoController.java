package org.springframework.security.oauth.examples.sparklr.mvc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Collection;
import java.util.Iterator;

import org.codehaus.jackson.map.util.JSONPObject;
import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth.examples.sparklr.PhotoInfo;
import org.springframework.security.oauth.examples.sparklr.PhotoService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Ryan Heaton
 * @author Dave Syer
 */
@Controller
public class PhotoController {

	public static final String STNC_RESPONSE = "{\n" +
			"  \"requestId\": \"ff36a3cc-ec34-11e6-b1a0-64510650abcf\",\n" +
			"  \"payload\": {\n" +
			"    \"agentUserId\": \"1836.15267389\",\n" +
			"    \"devices\": [{\n" +
			"      \"id\": \"123\",\n" +
			"      \"type\": \"action.devices.types.OUTLET\",\n" +
			"      \"traits\": [\n" +
			"        \"action.devices.traits.OnOff\"\n" +
			"      ],\n" +
			"      \"name\": {\n" +
			"        \"defaultNames\": [\"My Outlet 1234\"],\n" +
			"        \"name\": \"Night light\",\n" +
			"        \"nicknames\": [\"wall plug\"]\n" +
			"      },\n" +
			"      \"willReportState\": true,\n" +
			"        \"deviceInfo\": {\n" +
			"          \"manufacturer\": \"lights-out-inc\",\n" +
			"          \"model\": \"hs1234\",\n" +
			"          \"hwVersion\": \"3.2\",\n" +
			"          \"swVersion\": \"11.4\"\n" +
			"        },\n" +
			"        \"customData\": {\n" +
			"          \"fooValue\": 74,\n" +
			"          \"barValue\": true,\n" +
			"          \"bazValue\": \"foo\"\n" +
			"        }\n" +
			"    },{\n" +
			"      \"id\": \"456\",\n" +
			"      \"type\": \"action.devices.types.LIGHT\",\n" +
			"        \"traits\": [\n" +
			"          \"action.devices.traits.OnOff\", \"action.devices.traits.Brightness\",\n" +
			"          \"action.devices.traits.ColorTemperature\",\n" +
			"          \"action.devices.traits.ColorSpectrum\"\n" +
			"        ],\n" +
			"        \"name\": {\n" +
			"          \"defaultNames\": [\"lights out inc. bulb A19 color hyperglow\"],\n" +
			"          \"name\": \"lamp1\",\n" +
			"          \"nicknames\": [\"reading lamp\"]\n" +
			"        },\n" +
			"        \"willReportState\": true,\n" +
			"        \"attributes\": {\n" +
			"          \"temperatureMinK\": 2000,\n" +
			"          \"temperatureMaxK\": 6500\n" +
			"        },\n" +
			"        \"deviceInfo\": {\n" +
			"          \"manufacturer\": \"lights out inc.\",\n" +
			"          \"model\": \"hg11\",\n" +
			"          \"hwVersion\": \"1.2\",\n" +
			"          \"swVersion\": \"5.4\"\n" +
			"        },\n" +
			"        \"customData\": {\n" +
			"          \"fooValue\": 12,\n" +
			"          \"barValue\": false,\n" +
			"          \"bazValue\": \"bar\"\n" +
			"        }\n" +
			"      }]\n" +
			"  }\n" +
			"}";
	public static final String QUERY_RESPONSE = "{\n" +
			"  \"requestId\": \"ff36a3cc-ec34-11e6-b1a0-64510650abcf\",\n" +
			"  \"payload\": {\n" +
			"    \"devices\": {\n" +
			"      \"123\": {\n" +
			"        \"on\": true,\n" +
			"        \"online\": true\n" +
			"      },\n" +
			"      \"456\": {\n" +
			"        \"on\": true,\n" +
			"        \"online\": true,\n" +
			"        \"brightness\": 80,\n" +
			"        \"color\": {\n" +
			"          \"name\": \"cerulian\",\n" +
			"          \"spectrumRGB\": 31655\n" +
			"        }\n" +
			"      }\n" +
			"    }\n" +
			"  }\n" +
			"}";
	public static final String EXEC_RESPONSE = "{\n" +
            "  \"requestId\": \"ff36a3cc-ec34-11e6-b1a0-64510650abcf\",\n" +
            "  \"payload\": {\n" +
            "    \"commands\": [{\n" +
            "      \"ids\": [\"123\"],\n" +
            "      \"status\": \"SUCCESS\",\n" +
            "      \"states\": {\n" +
            "        \"on\": true,\n" +
            "        \"online\": true\n" +
            "      }\n" +
            "    },{\n" +
            "      \"ids\": [\"456\"],\n" +
            "      \"status\": \"ERROR\",\n" +
            "      \"errorCode\": \"deviceTurnedOff\"\n" +
            "    }]\n" +
            "  }\n" +
            "}";
	private PhotoService photoService;

	@RequestMapping("/photos/{photoId}")
	public ResponseEntity<byte[]> getPhoto(@PathVariable("photoId") String id) throws IOException {
		InputStream photo = getPhotoService().loadPhoto(id);
		if (photo == null) {
			return new ResponseEntity<byte[]>(HttpStatus.NOT_FOUND);
		}
		else {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int len = photo.read(buffer);
			while (len >= 0) {
				out.write(buffer, 0, len);
				len = photo.read(buffer);
			}
			HttpHeaders headers = new HttpHeaders();
			headers.set("Content-Type", "image/jpeg");
			return new ResponseEntity<byte[]>(out.toByteArray(), headers, HttpStatus.OK);
		}
	}

	@RequestMapping(value = "/home", method = RequestMethod.POST ,produces = "application/json" )
	public @ResponseBody String getIntent(@RequestBody String _json, HttpServletRequest request,
										 HttpServletResponse response) {

		JSONObject obj = new JSONObject(_json);
		String reqId=obj.getString("requestId");
		String intent = obj.getJSONArray("inputs").getJSONObject(0).getString("intent");

		if (intent.equalsIgnoreCase("action.devices.SYNC")){
//					{
//			"requestId": "ff36a3cc-ec34-11e6-b1a0-64510650abcf",
//			"inputs": [{
//				"intent": "action.devices.SYNC"
//	 		}]
//		}
			return STNC_RESPONSE;
		}else if (intent.equalsIgnoreCase("action.devices.QUERY")){
			return QUERY_RESPONSE;
		}else if (intent.equalsIgnoreCase("action.devices.EXECUTE")){
			return EXEC_RESPONSE;
		}
		return null;
	}



	@RequestMapping(value = "/photos", params = "format=json")
	public ResponseEntity<String> getJsonPhotos(Principal principal) {
		Collection<PhotoInfo> photos = getPhotoService().getPhotosForCurrentUser(principal.getName());
		StringBuilder out = new StringBuilder();
		out.append("{ \"photos\" : [ ");
		Iterator<PhotoInfo> photosIt = photos.iterator();
		while (photosIt.hasNext()) {
			PhotoInfo photo = photosIt.next();
			out.append(String.format("{ \"id\" : \"%s\" , \"name\" : \"%s\" }", photo.getId(), photo.getName()));
			if (photosIt.hasNext()) {
				out.append(" , ");
			}
		}
		out.append("] }");
		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/javascript");
		return new ResponseEntity<String>(out.toString(), headers, HttpStatus.OK);
	}

	@RequestMapping(value = "/photos", params = "format=xml")
	public ResponseEntity<String> getXmlPhotos(Principal principal) {
		Collection<PhotoInfo> photos = photoService.getPhotosForCurrentUser(principal.getName());
		StringBuilder out = new StringBuilder();
		out.append("<photos>");
		for (PhotoInfo photo : photos) {
			out.append(String.format("<photo id=\"%s\" name=\"%s\"/>", photo.getId(), photo.getName()));
		}
		out.append("</photos>");

		HttpHeaders headers = new HttpHeaders();
		headers.set("Content-Type", "application/xml");
		return new ResponseEntity<String>(out.toString(), headers, HttpStatus.OK);
	}

	@RequestMapping("/photos/trusted/message")
	@PreAuthorize("#oauth2.clientHasRole('ROLE_CLIENT')")
	@ResponseBody
	public String getTrustedClientMessage() {
		return "Hello, Trusted Client";
	}

	@RequestMapping("/photos/user/message")
	@ResponseBody
	public String getTrustedUserMessage(Principal principal) {
		return "Hello, Trusted User" + (principal != null ? " " + principal.getName() : "");
	}

	public PhotoService getPhotoService() {
		return photoService;
	}

	public void setPhotoService(PhotoService photoService) {
		this.photoService = photoService;
	}

}
