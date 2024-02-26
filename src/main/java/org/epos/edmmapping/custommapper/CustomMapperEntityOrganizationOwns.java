package org.epos.edmmapping.custommapper;

import org.epos.edmmapping.StandardIngestor;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMapperEntityOrganizationOwns implements CustomMapperEntity {

    @Override
    public List map(Map<String, Object> objectMap) {
    	
    	System.out.println(objectMap);

        if(objectMap.containsKey("#father#")){
        	System.out.println("HOLA");
            List<Facility> facilityList = new ArrayList<>();
            for (String value : objectMap.keySet()) {
            	System.out.println(objectMap.get(value).getClass());
            	System.out.println(objectMap.get(value).toString());

            }
            return facilityList;
        } else {
            return null;
        }
    }
}
