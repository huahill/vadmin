# 模块开发

[English](../../en/user/modules.md) | 简体中文

VAdmin 自带完整的管理外壳。你通过声明模块来添加业务页面——一个小型 Spring 配置，
告诉 VAdmin 路由、权限、翻译和视图 Bean。无需任何外壳、布局或主题工作。

## 声明模块

模块是一个 `@Configuration` 类，暴露一个 `AdminModule` Bean。以下示例添加一个
带读权限的 `inventory` 页面：

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

要点：

- **模块 ID**（`"inventory"`）必须在应用内唯一。
- **页面**声明路由（`inventory/items`）、所需权限、图标 key 以及标题和意图的
  翻译 key——均以模块 ID 为前缀。
- **视图**是 Spring Bean（原型作用域），不声明 `@Route`。VAdmin 从页面元数据注册
  路由，并在构造视图前检查权限。
- **权限**同时出现在页面和模块的权限集合中。格式为 `领域:资源:动作`，例如
  `inventory:item:read`。

## 翻译

在 classpath 上提供两种语言：

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

VAdmin 将你的翻译与自身的翻译合并，外壳和模块都会按用户选择的语言渲染。在视图
中用 `getTranslation(page.titleKey())` 解析文本，不要硬编码标签。

## 生产构建锚点

Vaadin 的生产前端 bundle 需要对每个视图的静态引用。在你的应用类上为每个动态视图
添加一个 `@Uses`：

```java
@Uses(InventoryView.class)
@SpringBootApplication
public final class InventoryApplication {
}
```

不要给视图加 `@Route`，也不要为 VAdmin 的系统视图加 `@Uses`——VAdmin 自己处理。

## 授权

导航可见性和直接路由访问由页面元数据中声明的权限控制。写操作需要在你的服务或
命令中再次检查权限再变更状态——UI 检查改善体验，服务层检查才是权威边界。

## 使用共享页面模式

VAdmin 为常见管理场景提供了高层 Flow 模式：`AdminPageFrame`、`PageHeader`、
`PageToolbar` 和 `DataWorkspace`。标准列表/详情/编辑页面优先使用这些模式，而不是
从布局基本组件开始拼装。领域专属 CSS 应限定在自己的组件类内，基于 Vaadin 组件
官方 API。

## 替换外壳

默认外壳是有意的整体替换设计。只有完整产品边界需要不同外壳时才替换——不能用于
零散修改个别页面或样式。替换意味着拥有完整的 `AppShellConfigurator`、`@Theme` 和
布局，同时仍使用 VAdmin 的模块装配、权限和路由注册。大多数使用方应保留默认外壳
不变。
