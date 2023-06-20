package org.epos.edmmapping;

import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.State;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class IngestorTest {

    @BeforeEach
    void setUp() throws IOException, ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        IngestorBuilder ingestorBuilder = new IngestorBuilderDCAT_EDM();
        Ingestor ingestor = ingestorBuilder.build();


        /*
        List<Object> ingest = ingestor.ingest("http://localhost:4998/AHEpisodesPlugin.ttl");
        EPOSDataModelDBAPI.saveAll(ingest);
        System.out.println(1);


        URL urlMultiline = new URL("http://localhost:4998/index.txt");
        Scanner s = new Scanner(urlMultiline.openStream());

        DBAPIClient client = new DBAPIClient(false);
        while (s.hasNextLine()) {
            String urlsingle = s.nextLine();
            System.out.println(urlsingle);
            List<EPOSDataModelEntity> ingest = ingestor.ingest(urlsingle);
            client.startTransaction();
            ingest.forEach(x -> {
                x.setState(State.PUBLISHED);
                x.setEditorId("ingestor");
                client.create(x);
            });
            client.closeTransaction(true);
            //EPOSDataModelDBAPI.saveAll(ingest);
            System.out.println("");
        }
*/

    }

    @Test
    void ingest() {
    }
}