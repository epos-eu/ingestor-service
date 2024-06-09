package org.epos.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2021-10-12T08:15:11.660Z[GMT]")
@Validated
public interface IngestorApi {

    @Operation(summary = "ingestor operation", description = "API for internal use only!.", tags={ "Ingestor Service" })
    @ApiResponses(value = { 
        @ApiResponse(responseCode = "200", description = "ok.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "201", description = "Created.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "204", description = "No content.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "301", description = "Moved Permanently.", content = @Content(mediaType = "*/*", schema = @Schema(implementation = ApiResponseMessage.class))),
        
        @ApiResponse(responseCode = "400", description = "Bad request."),
        
        @ApiResponse(responseCode = "401", description = "Token is missing or invalid"),
        
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        
        @ApiResponse(responseCode = "404", description = "Not Found") })
    @RequestMapping(value = "/ingestor",
        produces = { "*/*" }, 
        method = RequestMethod.POST)
    ResponseEntity<ApiResponseMessage> ingestorPost(
    		@Parameter(in = ParameterIn.HEADER, description = "ingestion type (single file or multiple lines file)" ,required=true,schema=@Schema(allowableValues={ "single", "multiple" })) @RequestHeader(value="type", required=true) String type, 
    		@Parameter(in = ParameterIn.HEADER, description = "path of the file to ingest" ,required=true,schema=@Schema()) @RequestHeader(value="path", required=true) String path,  
    		@Parameter(in = ParameterIn.HEADER, description = "metadata model" ,required=true,schema=@Schema(allowableValues={ "EPOS-DCAT-AP-V1", "EPOS-DCAT-AP-V3"})) @RequestHeader(value="path", required=true) String model,
    		@Parameter(in = ParameterIn.HEADER, description = "security code for internal things" ,required=true,schema=@Schema()) @RequestHeader(value="securityCode", required=true) String securityCode);

}

