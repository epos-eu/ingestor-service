package org.epos.configuration;

import org.epos.edmmapping.DBService;
import org.epos.router_framework.RpcRouter;
import org.epos.router_framework.exception.RoutingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

@Component
public class ReadinessHealthIndicator implements HealthIndicator {

	@Autowired
	private RpcRouter router;

    @Override
    public Health health() {
        int errorCode = check();
        if (errorCode != 0) {
            return Health.down().withDetail("No Database Connection", errorCode).build();
        }
        try {
			if(!router.doHealthCheck()) return Health.down().withDetail("RabbitMQ Connection", "Not Available").build();
		} catch (RoutingException e) {
			return Health.down().withDetail("RabbitMQ Connection", "Not Available").build();
		}
        return Health.up().build();
    }

    private int check() {

        try (
                Connection c = DBService.getDBConnection();
                Statement stmt = c.createStatement();
                ResultSet resultSetPropertyMapping = stmt.executeQuery("select * from class_mapping cm");
        ) {
        } catch (Exception ignored){
            return 1;
        }
        return 0;
    }
}
