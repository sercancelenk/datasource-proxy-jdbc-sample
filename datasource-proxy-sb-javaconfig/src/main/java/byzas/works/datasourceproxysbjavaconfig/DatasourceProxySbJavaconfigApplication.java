package byzas.works.datasourceproxysbjavaconfig;

import net.ttddyy.dsproxy.listener.logging.DefaultQueryLogEntryCreator;
import net.ttddyy.dsproxy.listener.logging.SystemOutQueryLoggingListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.hibernate.engine.jdbc.internal.Formatter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.Arrays;

@SpringBootApplication
public class DatasourceProxySbJavaconfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(DatasourceProxySbJavaconfigApplication.class, args);
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
    @Primary
    public DataSource dataSource(DataSource actualDatasource) {
        PrettyQueryEntryCreator prettyQueryEntryCreator = new PrettyQueryEntryCreator();
        prettyQueryEntryCreator.setMultiline(true);

        SystemOutQueryLoggingListener listener = new SystemOutQueryLoggingListener();
        listener.setQueryLogEntryCreator(prettyQueryEntryCreator);

        return ProxyDataSourceBuilder
                .create(actualDatasource)
                .name("MyDS")
                .listener(listener)
                .proxyResultSet()
                .afterMethod(methodExecutionContext -> {
                    Method method = methodExecutionContext.getMethod();
                    Class<?> targetClass = methodExecutionContext.getTarget().getClass();
                    System.out.println("JDBC: " + targetClass.getSimpleName() + "#" + method.getName());
                })
                .afterQuery((executionInfo, queryInfoList) -> {
                    System.out.println("Query took " + executionInfo.getElapsedTime() + "ms");
                })
                .build();
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
