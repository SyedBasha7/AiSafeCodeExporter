package com.inneo.aisafecodesync.web;

import com.inneo.aisafecodesync.web.controller.ExecutionController;
import com.inneo.aisafecodesync.web.dto.ProfileStatus;
import com.inneo.aisafecodesync.web.service.ProfileService;
import com.inneo.aisafecodesync.web.service.SyncRunCoordinator;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExecutionControllerTest {

    private final ProfileService profileService = mock(ProfileService.class);
    private final SyncRunCoordinator syncRunCoordinator = mock(SyncRunCoordinator.class);
    private final ExecutionController controller = new ExecutionController(profileService, syncRunCoordinator);

    @Test
    void executionPostRequiresServerSideAcknowledgement() {
        when(profileService.status(7L)).thenReturn(readyStatus());
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.execute(7L, false, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profiles/7/execute");
        assertThat(redirectAttributes.getFlashAttributes().get("error"))
                .isEqualTo("Confirm that actual execution may write files to the target folder.");
        verify(syncRunCoordinator, never()).execute(7L);
    }

    @Test
    void executionPostStaysBlockedWhenDryRunIsNotCurrent() {
        when(profileService.status(7L)).thenReturn(new ProfileStatus(
                true,
                List.of(),
                "current",
                null,
                null,
                false,
                false,
                List.of("AI-safe export requires a successful dry-run before actual execution.")
        ));
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        String view = controller.execute(7L, true, redirectAttributes);

        assertThat(view).isEqualTo("redirect:/profiles/7/execute");
        assertThat(redirectAttributes.getFlashAttributes().get("error").toString()).contains("successful dry-run");
        verify(syncRunCoordinator, never()).execute(7L);
    }

    @Test
    void acknowledgedExecutionRunsWhenProfileIsReady() {
        when(profileService.status(7L)).thenReturn(readyStatus());
        when(syncRunCoordinator.execute(7L)).thenReturn(42L);

        String view = controller.execute(7L, true, new RedirectAttributesModelMap());

        assertThat(view).isEqualTo("redirect:/reports/42");
        verify(syncRunCoordinator).execute(7L);
    }

    private ProfileStatus readyStatus() {
        Instant now = Instant.parse("2026-05-20T10:00:00Z");
        return new ProfileStatus(true, List.of(), "hash", "hash", now, true, true, List.of());
    }
}
