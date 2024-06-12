package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import org.apache.commons.codec.digest.DigestUtils;
import org.epos.core.MetadataPopulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
@Api(tags={ "Metadata Management Service" })
public class MetadataPopulationApiController implements MetadataPopulationApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetadataPopulationApiController.class);

	private final String code = System.getenv("INGESTOR_HASH");

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Autowired
	private Gson gsonSingleton;

	@Autowired
	public MetadataPopulationApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<ApiResponseMessage> metadataPopulate(
			@Parameter(in = ParameterIn.HEADER, description = "population type (single file or multiple lines file)" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" })) @RequestHeader(value="type", required=true) String type,
			@Parameter(in = ParameterIn.HEADER, description = "path of the file to use" ,required=true,schema=@Schema()) @RequestHeader(value="path", required=true) String path,
			@Parameter(in = ParameterIn.HEADER, description = "metadata model" ,required=true,schema=@Schema(allowableValues={ "EPOS-DCAT-AP-V1", "EPOS-DCAT-AP-V3"})) @RequestHeader(value="path", required=true) String model,
			@Parameter(in = ParameterIn.HEADER, description = "metadata mapping model" ,required=true,schema=@Schema(allowableValues={ "EDM-TO-DCAT-AP"})) @RequestHeader(value="path", required=true) String mapping,
			@Parameter(in = ParameterIn.HEADER, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestHeader(value="securityCode", required=true) String securityCode) {


		if( !validSecurityPhrase(securityCode) )
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		boolean multiline = type.equals("single") ? false : true;

		if (multiline) {
			URL urlMultiline = null;
			try {
				urlMultiline = new URL(path);
				Scanner s = new Scanner(urlMultiline.openStream());
				while (s.hasNextLine()) {
					String urlsingle = s.nextLine();
					System.out.println(urlsingle);
					MetadataPopulator.startMetadataPopulation(urlsingle,mapping);
				}
				s.close();
			} catch (IOException e) {
				return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,e.getLocalizedMessage()));
			}

		} else {
			MetadataPopulator.startMetadataPopulation(path,mapping);
		}

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,"DONE, correcly ingested "+path));
	}

	private boolean validSecurityPhrase( String phrase ) {

		if(!DigestUtils.sha1Hex(phrase).toUpperCase().equals(code)) {
			LOGGER.error("Security phrase incorrect. Input code is {}, encodes to {}", phrase, DigestUtils.sha1Hex(phrase));
			if ( code == null || code.trim().equals("") )
				LOGGER.error("No hash of security phrase provided in this environment");
			return false;
		}
		return true;
	}
}
