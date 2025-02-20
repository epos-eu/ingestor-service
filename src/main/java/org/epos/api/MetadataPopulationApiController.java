package org.epos.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import model.MetadataGroup;
import org.apache.commons.codec.digest.DigestUtils;
import org.epos.core.MetadataPopulator;
import org.epos.eposdatamodel.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import usermanagementapis.UserGroupManagementAPI;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@RestController
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
			@Parameter(in = ParameterIn.QUERY, description = "population type (single file or multiple lines file)" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" })) @RequestParam(value="type", required=true) String type,
			@Parameter(in = ParameterIn.QUERY, description = "path of the file to use" ,required=true,schema=@Schema()) @RequestParam(value="path", required=true) String path,
			@Parameter(in = ParameterIn.QUERY, description = "metadata model" ,required=true,schema=@Schema()) @RequestParam(value="model", required=true) String model,
			@Parameter(in = ParameterIn.QUERY, description = "metadata mapping model" ,required=true,schema=@Schema()) @RequestParam(value="mapping", required=true) String mapping,
			@Parameter(in = ParameterIn.QUERY, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestParam(value="securityCode", required=true) String securityCode,
			@Parameter(in = ParameterIn.QUERY, description = "metadata group where the resource should be placed" ,required=false,schema=@Schema()) @RequestParam(value="metadataGroup", required=false) String metadataGroup) {


		if( !validSecurityPhrase(securityCode) )
			return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);

		if (metadataGroup == null || metadataGroup.isEmpty()) {
			metadataGroup = "ALL";
		}

        try {
            metadataGroup=java.net.URLDecoder.decode(metadataGroup, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
			LOGGER.error("[Error] Error decoding groupname {}", e.getLocalizedMessage());
        }

        Group selectedGroup = UserGroupManagementAPI.retrieveGroupByName(metadataGroup);
		if (selectedGroup == null) {
			selectedGroup = UserGroupManagementAPI.retrieveGroupByName("ALL");
		}

		boolean multiline = type.equals("single") ? false : true;

		if (multiline) {
			URL urlMultiline = null;
			try {
				urlMultiline = new URL(path);
				Scanner s = new Scanner(urlMultiline.openStream());
				while (s.hasNextLine()) {
					String urlsingle = s.nextLine();
					LOGGER.info("[Ingestion initialized] Ingesting file {} using mapping {} in the group {}", urlsingle, mapping, selectedGroup.getName());
					MetadataPopulator.startMetadataPopulation(urlsingle,mapping, selectedGroup);
					LOGGER.info("[Ingestion finished] Ingested file {}", urlsingle);
				}
				s.close();
			} catch (IOException e) {
				return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(new ApiResponseMessage(4,e.getLocalizedMessage()));
			}

		} else {
			LOGGER.info("[Ingestion initialized] Ingesting file {} using mapping {} in the group {}", path, mapping, selectedGroup.getName());
			MetadataPopulator.startMetadataPopulation(path,mapping, selectedGroup);
			LOGGER.info("[Ingestion finished] Ingested file {}", path);
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
