package org.epos.edmmapping.custommapper;

import org.epos.eposdatamodel.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CustomMapperEntityLocation implements CustomMapperEntity {

    @Override
    public List map(Map<String, Object> objectMap) {

        if(objectMap.containsKey("dct:Location")){
            List<Location> locationList = new ArrayList<>();
            for (String value : objectMap.keySet()) {
                if (objectMap.get(value).getClass().equals(ArrayList.class) && ((List) objectMap.get(value)).contains("locn:geometry")) {
                    Location location = new Location();
                    location.setLocation(value);
                    locationList.add(location);
                }
            }
            return locationList;
        } else {
            return null;
        }
    }
}
