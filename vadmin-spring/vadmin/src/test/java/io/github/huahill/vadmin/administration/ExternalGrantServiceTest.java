package io.github.huahill.vadmin.administration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.huahill.vadmin.contracts.audit.AuditSink;
import io.github.huahill.vadmin.contracts.auth.AuthorizationService;
import io.github.huahill.vadmin.contracts.auth.ExternalDirectory;
import io.github.huahill.vadmin.contracts.auth.ExternalPerson;
import io.github.huahill.vadmin.contracts.auth.SubjectId;
import io.github.huahill.vadmin.platform.access.ExternalGrantStore;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExternalGrantServiceTest {

    @Test
    void showsThePersonNameNextToTheStoredLogin() {
        var store = mock(ExternalGrantStore.class);
        var directory = mock(ExternalDirectory.class);
        var service = new ExternalGrantService(store, directory, mock(AuthorizationService.class), mock(AuditSink.class));
        var subject = SubjectId.of("480116");
        when(store.list()).thenReturn(List.of(new ExternalGrantStore.ExternalGrant(
                subject, "huangx10", "", 1, List.of("administrator"))));
        when(directory.search("huangx10")).thenReturn(List.of(
                new ExternalPerson(subject, "huangx10", "黄校")));

        assertThat(service.grants()).singleElement().satisfies(grant -> {
            assertThat(grant.label()).isEqualTo("huangx10");
            assertThat(grant.displayName()).isEqualTo("黄校");
        });
    }

    @Test
    void keepsTheStoredLabelWhenTheDirectoryCannotBeSearched() {
        var store = mock(ExternalGrantStore.class);
        var directory = mock(ExternalDirectory.class);
        var service = new ExternalGrantService(store, directory, mock(AuthorizationService.class), mock(AuditSink.class));
        var subject = SubjectId.of("480116");
        when(store.list()).thenReturn(List.of(new ExternalGrantStore.ExternalGrant(
                subject, "huangx10", "", 1, List.of("administrator"))));
        when(directory.search("huangx10")).thenThrow(new IllegalStateException("directory down"));

        assertThat(service.grants()).singleElement().satisfies(grant -> {
            assertThat(grant.label()).isEqualTo("huangx10");
            assertThat(grant.displayName()).isEmpty();
        });
    }
}
