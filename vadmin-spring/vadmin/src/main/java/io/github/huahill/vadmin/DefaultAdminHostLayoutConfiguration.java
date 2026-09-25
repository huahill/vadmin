package io.github.huahill.vadmin;

import io.github.huahill.vadmin.flow.navigation.AdminHostLayout;
import io.github.huahill.vadmin.flow.navigation.AdminMessageBundle;
import io.github.huahill.vadmin.springflow.AdminFlowAutoConfiguration;
import io.github.huahill.vadmin.brand.AdminBrandProperties;
import io.github.huahill.vadmin.shell.AdminShellProperties;
import io.github.huahill.vadmin.views.DefaultMainLayout;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@AutoConfigurationPackage
@AutoConfigureBefore(AdminFlowAutoConfiguration.class)
@EnableConfigurationProperties({AdminBrandProperties.class, AdminShellProperties.class})
public class DefaultAdminHostLayoutConfiguration {
    @Bean
    @ConditionalOnMissingBean(AdminHostLayout.class)
    AdminHostLayout adminHostLayout() {
        return new AdminHostLayout(DefaultMainLayout.class);
    }

    @Bean(name = "defaultShellMessageBundle")
    AdminMessageBundle defaultShellMessageBundle() {
        return new AdminMessageBundle("system", "i18n.system");
    }
}
