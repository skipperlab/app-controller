package org.skipperlab.k8s.deploy.config;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.TimeZone;

@Component
public class FreeMarkerConfig {

    @Bean("FreeMarkerConfig")
    public Configuration freeMarkerConfig() {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);

        cfg.setObjectWrapper(new DefaultObjectWrapper());
//        cfg.setDirectoryForTemplateLoading(new File("/where/you/store/templates"));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
        cfg.setSQLDateAndTimeTimeZone(TimeZone.getDefault());

        return cfg;
    }
}
