package io.github.huahill.vadmin.administration;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "io.github.huahill.vadmin", importOptions = ImportOption.DoNotIncludeTests.class)
class AdministrationArchitectureTest {
    @ArchTest
    static final ArchRule administrationMustNotDependOnPersistence = noClasses()
            .that().resideInAPackage("io.github.huahill.vadmin.administration..")
            .should().dependOnClassesThat().resideInAnyPackage("io.github.huahill.vadmin.springjpa..",
                    "io.github.huahill.vadmin.springjdbc..", "org.springframework.jdbc..", "jakarta.persistence..")
            .because("shared administration services consume the platform storage contract");
}
