package io.github.huahill.vadmin.theme;

import java.io.Serializable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("vadmin.appearance")
public final class AdminAppearanceProperties implements Serializable {
    private String visualLanguage = "vaadin";

    public AdminVisualLanguage visualLanguage() {
        return AdminVisualLanguage.from(visualLanguage);
    }

    public void setVisualLanguage(String visualLanguage) {
        this.visualLanguage = visualLanguage;
    }

}
