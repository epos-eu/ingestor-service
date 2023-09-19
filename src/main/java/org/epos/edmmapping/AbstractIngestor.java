package org.epos.edmmapping;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RiotException;
import org.epos.edmmapping.custommapper.CustomMapperEntity;
import org.epos.edmmapping.custommapper.CustomMapperProperty;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.Contract;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Equipment;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.Person;
import org.epos.eposdatamodel.Publication;
import org.epos.eposdatamodel.Service;
import org.epos.eposdatamodel.SoftwareApplication;
import org.epos.eposdatamodel.SoftwareSourceCode;
import org.epos.eposdatamodel.State;
import org.epos.eposdatamodel.WebService;
import org.epos.handler.dbapi.dbapiimplementation.CategoryDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.CategorySchemeDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.ContactPointDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.ContractDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.DataProductDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.DistributionDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.EquipmentDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.FacilityDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.OperationDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.OrganizationDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.PersonDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.PublicationDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.ServiceDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.SoftwareApplicationDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.SoftwareSourceCodeDBAPI;
import org.epos.handler.dbapi.dbapiimplementation.WebServiceDBAPI;

import java.io.ByteArrayInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.*;
import static org.epos.edmmapping.EPOSDataModelMainEntity.EPOSDataModelMainEntityList;

import static org.epos.edmmapping.IngestorUtil.*;

@SuppressWarnings("unchecked")
public abstract class AbstractIngestor implements Ingestor{

	protected final List<String> methodPrefix = Arrays.asList("add", "set");

	protected HashMap<String, Set<String>> vocabularyMap;

	// from edm class name to possibile dcat vocabolary name to edm property and type
	// e.g. Person -> schema:familyName -> (familyName, Literal)
	protected HashMap<String,HashMap<String, AbstractMap.SimpleEntry<String,String>>> proprietyMap;

	//from entity dcat vocabolary name to edm class name e.g. schema:person -> Person
	protected HashMap<String, String> classMap;

	protected List<CustomMapperEntity> customMapperEntities = new LinkedList<>();

	protected List<CustomMapperProperty> customMappersProperties = new LinkedList<>();

	public AbstractIngestor vocabularyMap(HashMap<String, Set<String>> vocabularyMap) {
		this.vocabularyMap = vocabularyMap;
		return this;
	}

	public AbstractIngestor proprietyMap(HashMap<String, HashMap<String, AbstractMap.SimpleEntry<String, String>>> proprietyMap) {
		this.proprietyMap = proprietyMap;
		return this;
	}

	public AbstractIngestor classMap(HashMap<String, String> classMap) {
		this.classMap = classMap;
		return this;
	}

	public AbstractIngestor customMappers(List<CustomMapperEntity> customMappers) {
		this.customMapperEntities = customMappers;
		return this;
	}

	public AbstractIngestor addCustomMapperEntities(CustomMapperEntity customMappers) {
		this.customMapperEntities.add(customMappers);
		return this;
	}

	public AbstractIngestor addCustomMapperProperties(CustomMapperProperty customMappers) {
		this.customMappersProperties.add(customMappers);
		return this;
	}

	@Override
	public List<EPOSDataModelEntity> prepareIngest(String url) throws IOException {
		return prepareIngest(url, null);
	}

	public List<EPOSDataModelEntity> prepareIngest(String url, String selectedIngestClass) throws IOException {

		InputStreamReader is = createStream(url);

		ArrayList<EPOSDataModelEntity> toIngestObject = new ArrayList<>();

		// parse the ttl file in input via apache jena
		Map<String, Object> entity = new HashMap<>();
		Model model = ModelFactory.createDefaultModel();
		try {
			model.read(is, null, "TTL");
		} catch (RiotException e){
			System.err.println("riot exception: ");
			System.err.println(e);
			return new ArrayList<>();
		}

		// loop on all the triple and create all entities tree's with depth=1
		fromTripleToMap(entity, model);

		// loop on all entities and create a map with all the entities with subentities
		generateTree(entity);

		//test list
		ArrayList<Object> e = new ArrayList<>();

		// map all found entities from the epos_dcat to EDM
		// for every entity tree found inside the turtle file try to map it to an EDM bean
		for (String key : entity.keySet()) {

			// objectEntity is one of the tree built that rappresent a entity
			@SuppressWarnings("unchecked")
			Map<String, Object> objectEntity = (Map<String, Object>) entity.get(key);

			try {
				// inizialize a stack, to stop the recursion if it end inside some sort of circular dependency
				List<String> stack = new ArrayList<>();
				stack.add(key);
				// the function "buildObject" try to map the tree to a bean
				Object objectEposDataModel = buildObject(objectEntity, 0, stack);

				// if the object mapped is null skip a loop (usually this happens when it can't find the right EDM beans' name)
				if(objectEposDataModel == null) continue;

				// try to set the uid if is present
				try {
					String methodUIDName = "setUid";
					Method methodUID = objectEposDataModel.getClass().getMethod(methodUIDName, String.class);
					methodUID.invoke(objectEposDataModel, key );
				} catch (NoSuchMethodException | NullPointerException | IllegalAccessException | InvocationTargetException ignored) {}

				try {
					String methodFileprovenanceName = "setFileProvenance";
					Method methodFileprovenance = objectEposDataModel.getClass().getMethod(methodFileprovenanceName, String.class);
					methodFileprovenance.invoke(objectEposDataModel, url.substring(url.indexOf("/files/") + 7));
				} catch (NoSuchMethodException | NullPointerException | IllegalAccessException | InvocationTargetException ignored) {}

				// add object to the list to easy debug
				e.add(objectEposDataModel);

				String objectName = objectEposDataModel.getClass().getSimpleName();
				if(selectedIngestClass != null && !selectedIngestClass.equals(objectName))
					continue;

				if(EPOSDataModelMainEntityList.contains(objectName)) {

					if(objectName.equals("Category"))
						System.out.println(objectEposDataModel);
					if(objectName.equals("CategoryScheme"))
						System.out.println(objectEposDataModel);
					if(objectName.equals("WebService"))
						System.out.println(objectEposDataModel);

					toIngestObject.add((EPOSDataModelEntity) objectEposDataModel);
				}



			} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException ignored) {}

		}

		return toIngestObject;
	}

	@Override
	public String ingest(List<EPOSDataModelEntity> entities) {

		for(EPOSDataModelEntity entity : entities) {
			entity.setState(State.PUBLISHED);
			entity.setEditorId("ingestor");
			switch(entity.getClass().getSimpleName()) {
			case "Category":
				CategoryDBAPI catApi = new CategoryDBAPI();
				catApi.save((Category) entity);	
				break;
			case "CategoryScheme":
				CategorySchemeDBAPI catSchemeApi = new CategorySchemeDBAPI();
				catSchemeApi.save((CategoryScheme) entity);
				break;
			case "ContactPoint":
				ContactPointDBAPI contactApi = new ContactPointDBAPI();
				contactApi.save((ContactPoint) entity);
				break;
			case "Contract":
				ContractDBAPI contractApi = new ContractDBAPI();
				contractApi.save((Contract) entity);
				break;
			case "DataProduct":
				DataProductDBAPI dataproductApi = new DataProductDBAPI();
				dataproductApi.save((DataProduct) entity);
				break;
			case "Distribution":
				DistributionDBAPI distributionApi = new DistributionDBAPI();
				distributionApi.save((Distribution) entity);
				break;
			case "Equipment":
				EquipmentDBAPI equipmentApi = new EquipmentDBAPI();
				equipmentApi.save((Equipment) entity);
				break;
			case "Facility":
				FacilityDBAPI facilityApi = new FacilityDBAPI();
				facilityApi.save((Facility) entity);
				break;
			case "Operation":
				OperationDBAPI operationApi = new OperationDBAPI();
				operationApi.save((Operation) entity);
				break;
			case "Organization":
				OrganizationDBAPI organizationApi = new OrganizationDBAPI();
				organizationApi.save((Organization) entity);
				break;
			case "Person":
				PersonDBAPI personApi = new PersonDBAPI();
				personApi.save((Person) entity);
				break;
			case "Publication":
				PublicationDBAPI publicationApi = new PublicationDBAPI();
				publicationApi.save((Publication) entity);
				break;
			case "Service":
				ServiceDBAPI serviceApi = new ServiceDBAPI();
				serviceApi.save((Service) entity);
				break;
			case "SoftwareApplication":
				SoftwareApplicationDBAPI softwareApplicationApi = new SoftwareApplicationDBAPI();
				softwareApplicationApi.save((SoftwareApplication) entity);
				break;
			case "SoftwareSourceCode":
				SoftwareSourceCodeDBAPI softwareSourceCodeApi = new SoftwareSourceCodeDBAPI();
				softwareSourceCodeApi.save((SoftwareSourceCode) entity);
				break;
			case "WebService":
				WebServiceDBAPI webserviceApi = new WebServiceDBAPI();
				System.out.println((WebService)entity);
				webserviceApi.save((WebService) entity);
				break;
			default:
				break;
			}
		}
		return null;
	}

	//recursive function, trasform a entities tree in eposdatamodel bean
	private Object buildObject(Map<String, Object> objectMap, int depth, List<String> stack) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

		//--------------------------
		for (CustomMapperEntity customMapper : customMapperEntities){
			Object customMapperResults = customMapper.map(objectMap);
			if (customMapperResults != null)
				return customMapperResults;
		}
		//--------------------------

		String className = findEposDataModelClass(objectMap, classMap);

		if(className==null) return null;
		Class<?> classObject = Class.forName("org.epos.eposdatamodel." + className);
		Object objectEposDataModel = createObjectFromClass(classObject);

		if(objectMap.containsKey("#father#")){
			((List<TripleDataStruct>)objectMap.get("#father#")).forEach(triple -> {
				try {
					if(classMap.containsKey(triple.getFathertype()) && proprietyMap.containsKey(className) &&
							proprietyMap.get(className).containsKey(triple.getPredicate()) &&
							classMap.get(triple.getFathertype()).equals(proprietyMap.get(className).get(triple.getPredicate()).getValue())
							&& !triple.getPredicate().equals("schema:memberOf")) //sorry, I know this is horrible, I promise I'm going to fix this one day before I die but it must be done
						setPropertyObject(className, objectEposDataModel, triple.getValue(), triple.getPredicate());
				} catch (NoSuchMethodException ignored) {}
			});
		}

		Queue<String> keysQueue = new LinkedList<>(objectMap.keySet());
		ArrayList<String> controlQueue = new ArrayList<>(objectMap.keySet());
		while (!keysQueue.isEmpty()) {
			String propertyValue = keysQueue.remove();
			Object predicateNameOrMap = objectMap.get(propertyValue);
			boolean isMap = predicateNameOrMap.getClass().equals(HashMap.class);
			boolean isArrayList = predicateNameOrMap.getClass().equals(ArrayList.class);

			if (isMap) {
				Map<String, Object> predicateMap = (Map<String, Object>) predicateNameOrMap;
				String relation = propertyValue.split("#relation#")[1];//getRelation(predicateMap);

				if (depth > 4 || stack.contains(propertyValue)) {
					try {
						setPropertyObject(className, objectEposDataModel, propertyValue, relation);
					} catch (NoSuchMethodException ignored) {
					}

				} else {
					try {
						List<String> subStack = new ArrayList<>(stack);
						subStack.add(propertyValue);
						Object subObject = buildObject(predicateMap, depth + 1, subStack);

						if (subObject == null) {
							predicateMap.keySet().forEach( predicateMapKey -> {
								if( !propertyValue.equals(predicateMapKey) && !controlQueue.contains(predicateMapKey) &&
										!predicateMapKey.equals("#father#") &&
										!( predicateMap.get(predicateMapKey).getClass().equals(String.class) && predicateMap.get(predicateMapKey).equals("#nodeType#")) &&
										!( predicateMap.get(predicateMapKey).getClass().equals(ArrayList.class) &&  ((List) predicateMap.get(predicateMapKey)).contains("rdf:type") )) {
									keysQueue.add(predicateMapKey);
									controlQueue.add(predicateMapKey);
									objectMap.put(predicateMapKey, predicateMap.get(predicateMapKey));
								}
							});
						} else {
							setPropertyObject(className, objectEposDataModel, subObject, relation);
						}

					} catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException | IllegalAccessException e) {
						try {
							setPropertyObject(className, objectEposDataModel, propertyValue, relation);
						} catch (NoSuchMethodException ignored) {
						}
					}
				}

			} else if (isArrayList) {
				for (String predicateTmp : ((ArrayList<String>) predicateNameOrMap)) {

					try {
						setPropertyObject(className, objectEposDataModel, propertyValue, predicateTmp);
					} catch (NoSuchMethodException ignored) {
					}

				}
			}
		}

		return objectEposDataModel;
	}

	// this function take the object "object", his className "className" and use the attribute name in dcat
	// to get the correct name of the property in edm (it use the propertyMap). Then put inside the object the value
	// the correct place
	private void setPropertyObject(String className, Object object, Object value, String attributeNameDCAT) throws NoSuchMethodException{
		boolean done = false;

		//--------------------------
		for (CustomMapperProperty customMapper : customMappersProperties){
			if(customMapper.isToBeMapped(className,attributeNameDCAT, value.getClass())){
				customMapper.map(object,value);
				return;
			}
		}
		//--------------------------

		if(value.getClass().equals(String.class)) {
			// discard all artificial fields with #relation# inside the value
			if(((String) value).contains("#relation#")) return;

			// try to parse a date if possible
			try {
				DateTimeFormatter formatter = new DateTimeFormatterBuilder()
						.appendPattern("yyyy-MM-dd['T'HH:mm:ss'Z']")
						.parseDefaulting(ChronoField.HOUR_OF_DAY, 0)
						.parseDefaulting(ChronoField.MINUTE_OF_HOUR, 0)
						.parseDefaulting(ChronoField.SECOND_OF_MINUTE, 0)
						.toFormatter();
				value = LocalDateTime.parse((String)value, formatter);
				//System.out.println(value);
			} catch (DateTimeParseException ignored){}
		}
		
		//iterate to try to get the correct method prefix, "add" or "set"
		try {
			for (String elem : methodPrefix) {
				String methodName = elem + proprietyMap.get(className).get(attributeNameDCAT).getKey().replace(" ", "");
				try {
					//System.out.println(object.getClass()+" "+methodName+" "+value.getClass());
					Method method = object.getClass().getMethod(methodName, value.getClass());
					if (methodName.equals("setHasQualityAnnotation") &&
							value.getClass().equals(String.class) &&
							(((String) value).length() == 32) && !((String) value).contains("http"))
						return;
					method.invoke(object, value);
					done = true;
				} catch (NoSuchMethodException | NullPointerException | IllegalAccessException | InvocationTargetException e) {
					if (value.getClass().equals(LocalDateTime.class)) {
						try {
							Method method = object.getClass().getMethod(methodName, String.class);
							method.invoke(object, value.toString());
							done = true;
						} catch (NoSuchMethodException | NullPointerException | IllegalAccessException | InvocationTargetException ignored) {
						}
					} else {
						try {
							Method method = object.getClass().getMethod(methodName, LinkedEntity.class);
							LinkedEntity linkedEntity = new LinkedEntity();
							linkedEntity.setUid((String) value);
							method.invoke(object, linkedEntity);
							done = true;
						} catch (ClassCastException | NoSuchMethodException | NullPointerException | IllegalAccessException | InvocationTargetException ignored) {
						}
					}
				}
			}
		} catch (NullPointerException ignored) {}
		if(!done){
			throw new NoSuchMethodException();
		}
	}

	// loop throught all subject to nest where is possible to create complete tree of all entity
	private void generateTree(Map<String, Object> entity) {

		// it searches where the subject and the object are equal
		for (String key : entity.keySet()) {
			@SuppressWarnings("unchecked")
			Map<String, Object> son = (Map<String, Object>) entity.get(key);
			for (String key2 : entity.keySet()) {
				addToSubject(entity.get(key2), key, son, key2, new ArrayList<>(), 0);
			}
		}
	}

	private boolean addToSubject(Object father, String keyToSearch, Map<String, Object> son, String fatherKey, List<String> stack, int depth){
		try {

			@SuppressWarnings("unchecked")
			Set<String> keyset = new HashSet<> ((Set<String>) ((Map)father).keySet());


			for (String key : keyset) {

				Object fatherSubMap =  ((Map)father).get(key);
				Method methodPut = Map.class.getMethod("put", Object.class, Object.class);

				boolean isMap = fatherSubMap.getClass().equals(HashMap.class);

				if(key.equals(keyToSearch)){

					if (isMap) {
						methodPut.invoke(fatherSubMap, key, son);
					} else {
						son.put(((List<String>)fatherSubMap).get(0), "#nodeType#");

						String fatherType = getRDFTypeFromMap((Map<String,Object>)father);

						if(!son.containsKey("#father#"))
							son.put("#father#", new LinkedList<>());
						((List)son.get("#father#")).add(new TripleDataStruct(fatherKey, ((List<String>) fatherSubMap).get(0), fatherType));

						methodPut.invoke(father, key + "#relation#" + ((List<String>)fatherSubMap).get(0), son);
						return true;
					}
				}

				if(isMap) {
					if (stack.contains(key) || depth>3) return false;
					List<String> subStack = new ArrayList<>(stack);
					subStack.add(key);
					boolean find = addToSubject(fatherSubMap, keyToSearch, son, key, subStack, depth + 1);

				}
			}
		}
		catch (SecurityException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | IllegalArgumentException e) { e.printStackTrace(); }
		return false;
	}

	// return the currect entity type from a map
	private String getRDFTypeFromMap(Map<String, Object> father) {
		for (String key : father.keySet()) {
			if (father.get(key).getClass().equals(ArrayList.class) && ((List)father.get(key)).contains("rdf:type")) {
				return key;
			}
		}
		return null;
	}


	private void fromTripleToMap(Map<String, Object> entity, Model model) {

		//Iterate on all triple of the file
		StmtIterator it =  model.listStatements();
		while (it.hasNext()) {
			Statement stmt = it.next();

			// decompose the triple in subject - predicate - object
			String subject = stmt.getSubject().toString().replace("file:///", "").split("\\^\\^")[0];
			String predicate = model.shortForm(stmt.getPredicate().toString());
			String objectString = model.shortForm(stmt.getObject().toString());


			if(objectString.contains("@") && (objectString.length() - objectString.indexOf("@") < 4)) {
				objectString = objectString.substring(0, objectString.indexOf("@"));
			}
			String object = objectString.replace("file:///", "").split("\\^\\^")[0];

			// control if there is already the subject as root of the tree
			// the root of the object is the key of the map
			if(entity.containsKey(subject)){

				//the subject is already inside,
				HashMap<String, Object> tmpMap = (HashMap<String, Object>) entity.get(subject);

				// if an object is already inside the tree, it puts the predicate inside a list (like collision list)
				if (!tmpMap.containsKey(object)) {
					List<String> predicateList = new ArrayList<>();
					predicateList.add(predicate);
					tmpMap.put(object, predicateList);
				} else {
					((List) tmpMap.get(object)).add(predicate);
				}

			} else {
				// (the key is the root of the entity tree)
				HashMap<String, Object> tmpMap = new HashMap<>();
				List<String> predicateList = new ArrayList<>();
				predicateList.add(predicate);
				tmpMap.put(object, predicateList);
				entity.put(subject, tmpMap);
			}
		}
		
		System.out.println(entity.toString());
	}

	class TripleDataStruct{
		private String value;
		private String predicate;
		private String fathertype;

		public TripleDataStruct(String value, String predicate, String fathertype) {
			this.value = value;
			this.predicate = predicate;
			this.fathertype = fathertype;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}

		public String getPredicate() {
			return predicate;
		}

		public void setPredicate(String predicate) {
			this.predicate = predicate;
		}

		public String getFathertype() {
			return fathertype;
		}

		public void setFathertype(String fathertype) {
			this.fathertype = fathertype;
		}
	}


	public InputStreamReader createStream(String urlstring) throws IOException {

		URL url = new URL(urlstring);

		String contentType = url.openConnection().getContentType();

		Scanner sc= new Scanner(new URL(urlstring).openStream(), "UTF-8");
		String out = sc.useDelimiter("\\A").next();
		InputStream is = new ByteArrayInputStream(out.getBytes(StandardCharsets.UTF_8));
		sc.close();

		return new InputStreamReader(is);
	}

	public static void writeToFile(String thing) {
		try {
			FileWriter myWriter = new FileWriter("testout.json");
			myWriter.write(thing);
			myWriter.close();
			System.out.println("Successfully wrote to the file.");
		} catch (IOException e) {
			System.out.println("An error occurred.");
			e.printStackTrace();
		}
	}

}
