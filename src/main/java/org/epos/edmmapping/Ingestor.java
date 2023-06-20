package org.epos.edmmapping;

import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.io.IOException;
import java.util.List;

public interface Ingestor {

    List<EPOSDataModelEntity> prepareIngest(String url) throws IOException;
    
    String ingest(List<EPOSDataModelEntity> entities);
}
