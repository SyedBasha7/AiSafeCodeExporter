package com.inneo.aisafecodesync.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaSqlTest {

    @Test
    void schemaSqlCanInitializeAnEmptyDatabaseAndRunAgainWithoutWarnings() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                "jdbc:h2:mem:schema-idempotent;MODE=PostgreSQL;DATABASE_TO_UPPER=false;DB_CLOSE_DELAY=-1"
        )) {
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator(new ClassPathResource("schema.sql"));
            SingleConnectionDataSource dataSource = new SingleConnectionDataSource(connection, true);

            populator.execute(dataSource);
            populator.execute(dataSource);

            try (var statement = connection.createStatement()) {
                statement.executeUpdate("""
                        insert into sync_profiles
                            (id, name, profile_type, allow_target_inside_source)
                        values
                            (100, 'Schema test profile', 'AI_SAFE_EXPORT', false)
                        """);
                statement.executeUpdate("""
                        insert into replacement_rules
                            (id, profile_id, rule_id, search_value, replacement_value, case_sensitive, regex, enabled, sort_order)
                        values
                            (200, 100, 'project-name', 'DemoCustomerPortal', 'demo-app', true, false, true, 0)
                        """);
                statement.executeUpdate("""
                        insert into replacement_rule_apply_targets
                            (replacement_rule_id, apply_target)
                        values
                            (200, 'FILE_CONTENT')
                        """);
                try (var resultSet = statement.executeQuery("select count(*) from replacement_rules")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getInt(1)).isEqualTo(1);
                }
            }
        }
    }
}
