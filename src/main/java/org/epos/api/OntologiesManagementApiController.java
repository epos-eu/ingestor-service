package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import model.Ontologies;
import org.apache.commons.codec.digest.DigestUtils;
import org.epos.core.OntologiesManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
public class OntologiesManagementApiController implements OntologiesManagementApi {

	private static final Logger LOGGER = LoggerFactory.getLogger(OntologiesManagementApiController.class);

	private final String code = System.getenv("INGESTOR_HASH");

	private final ObjectMapper objectMapper;

	private final HttpServletRequest request;

	@Autowired
	private Gson gsonSingleton;

	@Autowired
	public OntologiesManagementApiController(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}

	public ResponseEntity<ApiResponseMessage> ontologyPopulate(
			@Parameter(in = ParameterIn.QUERY, description = "path to the ontology file" ,required=true,schema=@Schema()) @RequestParam(value="path", required=true) String path,
			@Parameter(in = ParameterIn.QUERY, description = "ontology name" ,required=true,schema=@Schema()) @RequestParam(value="name", required=true) String name,
			@Parameter(in = ParameterIn.QUERY, description = "ontology type" ,required=true,schema=@Schema(allowableValues = {"BASE", "MAPPING"})) @RequestParam(value="type", required=true) String type,
			@Parameter(in = ParameterIn.QUERY, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestParam(value="securityCode", required=true) String securityCode) {


		if( !validSecurityPhrase(securityCode) )
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		try {
			OntologiesManager.createOntology(name, type, path);
		}catch(IOException e){
			return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(ApiResponseMessage.ERROR,"ERROR on adding ontology from: "+path));
		}
		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(ApiResponseMessage.OK,"DONE, correcly added ontology from: "+path));
	}

	public ResponseEntity<List<Ontologies>> ontologyRetrieve(
			@Parameter(in = ParameterIn.QUERY, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestParam(value="securityCode", required=true) String securityCode) {


		if( !validSecurityPhrase(securityCode) )
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		List<Ontologies> ontologiesList = new ArrayList<>();
		ontologiesList = OntologiesManager.retrieveOntologies();

		return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(ontologiesList);
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
