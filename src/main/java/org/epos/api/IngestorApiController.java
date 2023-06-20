package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import org.apache.commons.codec.digest.DigestUtils;
import org.epos.configuration.GsonByteArrayToBase64;
import org.epos.configuration.GsonLocalDateTime;
import org.epos.edmmapping.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.router_framework.RpcRouter;
import org.epos.router_framework.domain.Request;
import org.epos.router_framework.domain.RequestBuilder;
import org.epos.router_framework.domain.Response;
import org.epos.router_framework.types.ServiceType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import javax.servlet.http.HttpServletRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
@Api(tags={ "Ingestor Service" })
public class IngestorApiController implements IngestorApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(IngestorApiController.class);

	private final String code = System.getenv("INGESTOR_HASH");


	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Autowired
	private RpcRouter router;

	@Autowired
	private Gson gsonSingleton;

	@org.springframework.beans.factory.annotation.Autowired
	public IngestorApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<ApiResponseMessage> ingestorPost(@Parameter(in = ParameterIn.HEADER, description = "path of the element to ingest" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" }
			)) @RequestHeader(value="type", required=true) String type,@Parameter(in = ParameterIn.HEADER, description = "path of the element to ingest" ,required=true,schema=@Schema()) @RequestHeader(value="path", required=true) String path,@Parameter(in = ParameterIn.HEADER, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestHeader(value="securityCode", required=true) String securityCode) {


		if( !validSecurityPhrase(securityCode) )
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		try {
			if (path != null && type != null) {
				/*
				 * retrieve ttl content via url(s)
				 * */
				String url = path;

				boolean multiline = type.equals("single") ? false : true;

				IngestorBuilder ingestorBuilder = new IngestorBuilderDCAT_EDM();
				Ingestor ingestor = ingestorBuilder.build();

				HashMap<String, Object> headers = new HashMap<>();

				headers.put("kind", "ingestor.post.eposdatamodel");
				/**
				 * MULTILINE CLAUSE
				 */
				if (multiline) {
					URL urlMultiline = new URL(url);
					Scanner s = new Scanner(urlMultiline.openStream());
					while (s.hasNextLine()) {
						String urlsingle = s.nextLine();
						System.out.println(urlsingle);
						ingestUrl(ingestor, headers, urlsingle);
					}
					s.close();
				} else {
					System.out.println(url);
					ingestUrl(ingestor, headers, url);
				}
			} else {
				return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,"Invalid JSON arguments. Choose (url,multiline), (pluginid,installpath) or (ttl)."));
			}

		} catch (Exception e) {
			e.printStackTrace();
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,"Something went wrong: "+e.getLocalizedMessage()));
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,"DONE, correcly ingested "+path));
	}

	private void ingestUrl(Ingestor ingestor, HashMap<String, Object> headers, String urlsingle) throws IOException {
		List<EPOSDataModelEntity> ingestedObject = ingestor.prepareIngest(urlsingle);
		
		ingestor.ingest(ingestedObject);

		System.out.println("ingested " + ingestedObject.size() + " entities");

		/*List<MessageEDMWrap> ingestedObjectSerialized = ingestedObject.stream()
				.map( eposDataModelObject -> {
					MessageEDMWrap wrap = new MessageEDMWrap();
					wrap.setObjectName(eposDataModelObject.getClass().getSimpleName());
					wrap.setObject(gsonSingleton.toJson(eposDataModelObject));
					return wrap;
				})
				.collect(Collectors.toList());

		String payload = gsonSingleton.toJson(ingestedObjectSerialized);
		
		String requestType = request.getRequestURI()
				.replaceAll("^\\/+", "")
				.replaceAll("\\/+$", "")
				.replaceAll("\\/", "\\.");

		Request request = RequestBuilder.instance(ServiceType.INGESTOR, "post", requestType)//
				.addPayloadPlainText(payload)
				.addHeaders(headers)//
				.build();

		Response response = router.makeRequest(request);*/

	}

	private  boolean validSecurityPhrase( String phrase ) {

		if(!DigestUtils.sha1Hex(phrase).toUpperCase().equals(code)) {
			LOGGER.error("Security phrase incorrect. Input code is {}, encodes to {}", phrase, DigestUtils.sha1Hex(phrase));
			if ( code == null || code.isBlank() )
				LOGGER.error("No hash of security phrase provided in this environment");
			return false;
		}
		return true;
	}

}
