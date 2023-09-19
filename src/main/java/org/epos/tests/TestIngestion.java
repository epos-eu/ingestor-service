package org.epos.tests;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.epos.edmmapping.Ingestor;
import org.epos.edmmapping.IngestorBuilder;
import org.epos.edmmapping.IngestorBuilderDCAT_EDM;
import org.epos.eposdatamodel.EPOSDataModelEntity;

public class TestIngestion {
	
	public static void main(String[] args) throws IOException {
		String url = "http://192.168.252.173:4200/SUPERPROCESSING.ttl";

		IngestorBuilder ingestorBuilder = new IngestorBuilderDCAT_EDM();

		Ingestor ingestor = ingestorBuilder.build();

		HashMap<String, Object> headers = new HashMap<>();

		headers.put("kind", "ingestor.post.eposdatamodel");
		TestIngestion.ingestUrl(ingestor, headers, url);
	}
	
	public static void ingestUrl(Ingestor ingestor, HashMap<String, Object> headers, String urlsingle) throws IOException {
		List<EPOSDataModelEntity> ingestedObject = ingestor.prepareIngest(urlsingle);
		

		for(EPOSDataModelEntity entity : ingestedObject) {
			System.out.println(entity);
		}

		ingestor.ingest(ingestedObject);

		System.out.println("ingested " + ingestedObject.size() + " entities");


	}

}
