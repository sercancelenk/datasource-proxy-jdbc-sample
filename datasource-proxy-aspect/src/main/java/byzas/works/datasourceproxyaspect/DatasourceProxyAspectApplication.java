package byzas.works.datasourceproxyaspect;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.Arrays;

/**
 * @author Sercan CELENK
 */

@SpringBootApplication
public class DatasourceProxyAspectApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceProxyAspectApplication.class, args);
    }

    @Bean
    public DataSource actualDatasource(){
        EmbeddedDatabaseBuilder databaseBuilder = new EmbeddedDatabaseBuilder();
        return databaseBuilder.setType(EmbeddedDatabaseType.HSQL).build();
    }

    // use hibernate to format queries
    private static class PrettyQueryEntryCreator extends DefaultQueryLogEntryCreator {
        private Formatter formatter = FormatStyle.BASIC.getFormatter();

        @Override
        protected String formatQuery(String query) {
            return this.formatter.format(query);
        }
    }


    @Bean
    CommandLineRunner init(JdbcTemplate jdbcTemplate, DataSource ds){
        return args -> {
            System.out.println("********************************");
            jdbcTemplate.execute("CREATE TABLE users (id INT, name VARCHAR(20))");

            jdbcTemplate.batchUpdate("INSERT INTO users (id, name) VALUES (?, ?)",
                    Arrays.asList(new Object[][] { { 1, "foo" }, { 2, "bar" } }));

            PreparedStatement preparedStatement = jdbcTemplate.getDataSource().getConnection()
                    .prepareStatement("INSERT INTO users (id, name) VALUES (?, ?)");
            preparedStatement.setString(2, "FOO");
            preparedStatement.setInt(1, 3);
            preparedStatement.addBatch();
            preparedStatement.setInt(1, 4);
            preparedStatement.setString(2, "BAR");
            preparedStatement.addBatch();
            preparedStatement.executeBatch();

            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            System.out.println("**********************************************************");
        };
    }

}
