package org.epos.edmmapping.custommapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface CustomMapperEntity {

    Object map(Map<String, Object> objectMap);

}
