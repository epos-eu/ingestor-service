package org.epos.edmmapping;

import org.epos.edmmapping.custommapper.CustomMapperEntityLocation;
import org.epos.edmmapping.custommapper.CustomMapperEntityPotentialAction;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class IngestorBuilderDCAT2_EDM extends IngestorBuilderGenericDCAT_EDM implements IngestorBuilder{
    @Override
    public Ingestor build() {
        Map<String, Object> returns = this.getSemanticDCAT("epos-dcat-apv2");

        HashMap<String, Set<String>> vocabularyMap = (HashMap<String, Set<String>>) returns.get("vocabulary");
        HashMap<String,HashMap<String, AbstractMap.SimpleEntry<String,String>>> proprietyMap = (HashMap<String, HashMap<String, AbstractMap.SimpleEntry<String, String>>>) returns.get("propriety");
        HashMap<String, String>  classMap = (HashMap<String, String>) returns.get("class");

        return new StandardIngestor()
                .vocabularyMap(vocabularyMap)
                .proprietyMap(proprietyMap)
                .classMap(classMap)
                .addCustomMapperEntities(new CustomMapperEntityLocation())
                .addCustomMapperEntities(new CustomMapperEntityPotentialAction());

    }
}
