# Modules

[简体中文](../../zh-CN/user/modules.md) | English

VAdmin ships a complete administration shell out of the box. You add
business pages by declaring a module — a small Spring configuration that
tells VAdmin the page routes, permissions, translations, and view beans. No
shell, layout, or theme work is needed.

## Declare A Module

A module is a `@Configuration` class that exposes an `AdminModule` bean.
This example adds an `inventory` page with read permission:

```java
package com.example.inventory;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.github.youngledo.vadmin.contracts.auth.PermissionCode;
import io.github.youngledo.vadmin.flow.navigation.AdminMessageBundle;
import io.github.youngledo.vadmin.flow.navigation.AdminModule;
import io.github.youngledo.vadmin.flow.navigation.AdminNavigationGroup;
import io.github.youngledo.vadmin.flow.navigation.AdminPage;
import java.util.List;
import java.util.Set;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

@Configuration(proxyBeanMethods = false)
public class InventoryModuleConfiguration {
  static final PermissionCode INVENTORY_READ =
      PermissionCode.of("inventory:item:read");

  @Bean
  AdminModule inventoryModule() {
    return AdminModule.of("inventory",
        List.of(new AdminNavigationGroup("inventory", "inventory.navigation", 200)),
        List.of(new AdminPage("inventory.items", "inventory", "inventory.items.title",
            "inventory.items.intent", "cube", 100, "inventory/items", INVENTORY_READ,
            InventoryView.class)),
        Set.of(INVENTORY_READ),
        List.of(new AdminMessageBundle("inventory", "i18n.inventory")));
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
  InventoryView inventoryView() {
    return new InventoryView();
  }
}

final class InventoryView extends VerticalLayout {
  InventoryView() {
    add("Inventory");
  }
}
```

Key points:

- **Module ID** (`"inventory"`) must be unique across the application.
- **Page** declares a route (`inventory/items`), a required permission,
  an icon key, and translation keys for title and intent — all prefixed with
  the module ID.
- **View** is a Spring bean (prototype scope) with no `@Route`. VAdmin
  registers the route from the page metadata and checks the permission
  before the view is constructed.
- **Permission** appears in both the page and the module's permission set.
  The format is `domain:resource:action`, e.g. `inventory:item:read`.

## Translations

Provide both locales on the classpath:

```text
src/main/resources/i18n/inventory_en_US.properties
src/main/resources/i18n/inventory_zh_CN.properties
```

```properties
# inventory_en_US.properties
inventory.navigation=Inventory
inventory.items.title=Items
inventory.items.intent=Browse stock and availability
```

```properties
# inventory_zh_CN.properties
inventory.navigation=库存
inventory.items.title=库存项目
inventory.items.intent=查看库存和可用性
```

VAdmin merges your translations with its own, so both the shell and your
module render in the user's selected locale. In your view, resolve text with
`getTranslation(page.titleKey())` rather than hard-coding labels.

## Production Build Anchor

Vaadin's production frontend bundle needs a static reference to each view.
Add `@Uses` on your application class — one per dynamic view:

```java
@Uses(InventoryView.class)
@SpringBootApplication
public final class InventoryApplication {
}
```

Do not add `@Route` to your view, and do not add `@Uses` for VAdmin's
built-in system views — VAdmin handles those.

## Authorization

Navigation visibility and direct-route access are controlled by the
permission declared in the page metadata. For write operations, check the
permission again inside your service or command before changing state — the
UI check improves the experience, but the service-level check is the
authoritative boundary.

## Using Shared Page Patterns

VAdmin provides high-level Flow patterns for common admin workflows:
`AdminPageFrame`, `PageHeader`, `PageToolbar`, and `DataWorkspace`. Use these
for standard list/detail/edit pages instead of assembling layout primitives
yourself. Keep any domain-specific CSS scoped to your own component class and
based on documented Vaadin component APIs.

## Replacing The Shell

The default shell is intentionally all-or-nothing. Only replace it if the
complete product boundary requires a different shell — not to tweak
individual pages or styles. A replacement means owning the full
`AppShellConfigurator`, `@Theme`, and layout, while still using VAdmin's
module assembly, permissions, and route registration. Most consumers should
keep the default shell unchanged.
