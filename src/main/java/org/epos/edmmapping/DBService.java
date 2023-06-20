package org.epos.edmmapping;


import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;


public class DBService {

	public static Connection getDBConnection() throws SQLException {
		String postgresql_connection_string = System.getenv("POSTGRESQL_CONNECTION_STRING");
		if(postgresql_connection_string != null) {
			Map<String,String> properties = getPropertieFromConnectionString(postgresql_connection_string);
			return DriverManager.getConnection(
					properties.get("javax.persistence.jdbc.url"));
		}else {
			Map<String, String> properties = getProperties();
			return DriverManager.getConnection(
					properties.get("javax.persistence.jdbc.url"),
					properties.get("javax.persistence.jdbc.user"),
					properties.get("javax.persistence.jdbc.password"));
		}

	}

	private static Map<String,String> getPropertieFromConnectionString(String connectionString) {
		HashMap<String,String> properties = new HashMap <>();
		properties.put("javax.persistence.jdbc.url",connectionString);
		return properties;
	}


	private static Map<String,String> getProperties() {
		HashMap<String,String> properties = new HashMap <>();

		String dburl="jdbc:postgresql://";

		dburl+= System.getenv("POSTGRESQL_HOST");
		dburl+="/";
		dburl+= System.getenv("POSTGRESQL_DBNAME");

		String user=System.getenv("POSTGRESQL_USERNAME");
		String password=System.getenv("POSTGRESQL_PASSWORD");




		properties.put("javax.persistence.jdbc.url",dburl);
		properties.put("javax.persistence.jdbc.user",user);
		properties.put("javax.persistence.jdbc.password",password);
		return properties;
	}
}
