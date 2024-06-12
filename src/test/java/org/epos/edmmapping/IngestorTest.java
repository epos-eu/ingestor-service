package org.epos.edmmapping;

import org.epos.api.ApiResponseMessage;
import org.epos.core.MetadataPopulator;
import org.epos.core.OntologiesManager;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.net.URL;
import java.util.Scanner;

class IngestorTest {

	public static void populateWithOntologies() throws IOException {
		System.out.println("Populating ontologies");
		String metadataOntologyDCATAPIV1 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-shapes/epos-dcat-ap_shapes.ttl";
		String metadataOntologyDCATAPIV3 = "https://raw.githubusercontent.com/epos-eu/EPOS-DCAT-AP/EPOS-DCAT-AP-v3.0/docs/epos-dcat-ap_v3.0.0_shacl.ttl";
		String metadataMappingEPOSDataModel = "https://raw.githubusercontent.com/epos-eu/EPOS_Data_Model_Mapping/main/edm-schema-shapes.ttl";

		OntologiesManager.createOntology("EPOS-DCAT-AP-V1", metadataOntologyDCATAPIV1);
		OntologiesManager.createOntology("EPOS-DCAT-AP-V3", metadataOntologyDCATAPIV3);
		OntologiesManager.createOntology("EDM-TO-DCAT-AP", metadataMappingEPOSDataModel);
	}

	public static void main(String[] args){

		Boolean multiline = false;
		final String path = "http://10.101.10.44:4200/WP11_IMO_DDSS-067_GNSS_stations_corrected.ttl";
		final String mapping = "EDM-TO-DCAT-AP";

		try {
			populateWithOntologies();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
				System.out.println(e.getLocalizedMessage());
			}

		} else {
			MetadataPopulator.startMetadataPopulation(path,mapping);
		}

	}

}