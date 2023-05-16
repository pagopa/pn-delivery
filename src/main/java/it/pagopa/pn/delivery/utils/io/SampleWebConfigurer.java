package it.pagopa.pn.delivery.utils.io;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
public class SampleWebConfigurer implements WebMvcConfigurer {

    @Autowired
    private HttpVerifierHandlerInterceptor interceptor;
    @Autowired
    private SampleLollipopConsumerConfig sampleLollipopConsumerConfig;

    @Bean
    public CommonsRequestLoggingFilter loggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludeClientInfo(true);
        filter.setIncludeHeaders(true);
        filter.setIncludePayload(true);
        return filter;
    }


    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(sampleLollipopConsumerConfig.getEndpoint())
                .pathMatcher(new AntPathMatcher());
    }
}
