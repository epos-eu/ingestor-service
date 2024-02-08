package org.epos.edmmapping;

import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class IngestorTest {

	public static void main(String[] args) throws IOException {
		String url = "http://10.101.10.44:4200/test.ttl";
		IngestorBuilder ingestorBuilder = new IngestorBuilderDCAT_EDM();
		Ingestor ingestor = ingestorBuilder.build();


		HashMap<String, Object> headers = new HashMap<>();

		headers.put("kind", "ingestor.post.eposdatamodel");
		List<EPOSDataModelEntity> ingestedObject = ingestor.prepareIngest(url);


		for(EPOSDataModelEntity entity : ingestedObject) {
			System.out.println(entity);
		}

		ingestor.ingest(ingestedObject);

		System.out.println("ingested " + ingestedObject.size() + " entities");
	}
}