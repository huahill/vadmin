package io.github.huahill.vadmin.contracts.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ExternalPersonTest {

    @Test
    void accountLabelIncludesTheNameAndLogin() {
        var person = new ExternalPerson(SubjectId.of("480116"), "huangx10", "黄校");

        assertThat(person.accountLabel()).isEqualTo("黄校 (huangx10)");
    }

    @Test
    void accountLabelKeepsTheLoginWhenThereIsNoSeparateName() {
        var person = new ExternalPerson(SubjectId.of("480116"), "huangx10", " huangx10 ");

        assertThat(person.accountLabel()).isEqualTo("huangx10");
    }
}
