package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import javax.servlet.http.HttpServletRequest;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
@Api(tags={ "Ingestor Service" })
public class IngestorApiController implements IngestorApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(IngestorApiController.class);

	private final String code = System.getenv("INGESTOR_HASH");


	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Autowired
	private Gson gsonSingleton;

	@org.springframework.beans.factory.annotation.Autowired
	public IngestorApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<ApiResponseMessage> ingestorPost(
			@Parameter(in = ParameterIn.HEADER, description = "ingestion type (single file or multiple lines file)" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" })) @RequestHeader(value="type", required=true) String type, 
			@Parameter(in = ParameterIn.HEADER, description = "path of the file to ingest" ,required=true,schema=@Schema()) @RequestHeader(value="path", required=true) String path,  
			@Parameter(in = ParameterIn.HEADER, description = "metadata model" ,required=true,schema=@Schema(allowableValues={ "EPOS-DCAT-AP-V1", "EPOS-DCAT-AP-V2"})) @RequestHeader(value="model", required=true) String model, 
			@Parameter(in = ParameterIn.HEADER, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestHeader(value="securityCode", required=true) String securityCode) {

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,"DONE, correcly ingested "+path));
	}
}
