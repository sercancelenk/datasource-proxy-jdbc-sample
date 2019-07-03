package byzas.works.datasourceproxyaspect;

import net.ttddyy.dsproxy.listener.logging.SLF4JLogLevel;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.apache.commons.lang3.BooleanUtils;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
public class DatasourceProxyBeanPostProcesor implements BeanPostProcessor {


    private static Environment env;

    public static Long getSlowQueriesThresoldsMs() {
        String val = env.getProperty("log.slowqueries.thresholdms");
        if (StringUtils.hasText(val))
            return Long.valueOf(val);
        return 1L;
    }

    public static Boolean getLogAllQueries() {
        Boolean result = Boolean.FALSE;
        try {
            result = env.getProperty("log.all-queries", Boolean.class);
        } catch (Exception ex) {
        }
        return result;
    }

    @Autowired
    public void setEnv(Environment _env) {
        env = _env;
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
        if (bean instanceof DataSource) {
            ProxyFactory proxyFactory = new ProxyFactory(bean);
            proxyFactory.setProxyTargetClass(true);
            proxyFactory.addAdvice(new ProxyDataSourceInterceptor((DataSource) bean));
            return proxyFactory.getProxy();
        }
        return bean;
    }

    private static class ProxyDataSourceInterceptor implements MethodInterceptor {
        private final DataSource dataSource;


        public ProxyDataSourceInterceptor(final DataSource dataSource) {
            super();
            ProxyDataSourceBuilder builder = ProxyDataSourceBuilder
                    .create(dataSource)
                    .countQuery();


            builder.logSlowQueryBySlf4j(Objects.isNull(getSlowQueriesThresoldsMs()) ? 500L : getSlowQueriesThresoldsMs(), TimeUnit.MILLISECONDS, SLF4JLogLevel.INFO);

            if (BooleanUtils.isTrue( getLogAllQueries())) {
                builder.logQueryBySlf4j(SLF4JLogLevel.INFO);
            }

            this.dataSource = builder.asJson().build();
        }

        @Override
        public Object invoke(final MethodInvocation invocation) throws Throwable {

            Method proxyMethod = ReflectionUtils.findMethod(dataSource.getClass(), invocation.getMethod().getName());
            if (proxyMethod != null) {
                return proxyMethod.invoke(dataSource, invocation.getArguments());
            }
            return invocation.proceed();
        }
    }
}
