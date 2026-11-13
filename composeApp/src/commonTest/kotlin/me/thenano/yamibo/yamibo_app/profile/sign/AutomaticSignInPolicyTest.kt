package me.thenano.yamibo.yamibo_app.profile.sign

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AutomaticSignInPolicyTest {
    @Test
    fun retriesFailuresAfterCooldownAndAllowsNextAccountAndDate() {
        val policy = AutomaticSignInPolicy()
        val now = 1_000_000L

        assertTrue(policy.tryStart("one:2026-11-13", true, false, now))
        assertFalse(policy.tryStart("one:2026-11-13", true, false, now + 1_000L))
        policy.finish("one:2026-11-13")
        assertFalse(
            policy.tryStart(
                "one:2026-11-13",
                true,
                false,
                now + AutomaticSignInPolicy.RETRY_INTERVAL_MILLIS - 1L,
            ),
        )
        assertTrue(
            policy.tryStart(
                "one:2026-11-13",
                true,
                false,
                now + AutomaticSignInPolicy.RETRY_INTERVAL_MILLIS,
            ),
        )
        policy.finish("one:2026-11-13")
        assertTrue(policy.tryStart("two:2026-11-13", true, false, now + 1_000L))
        policy.finish("two:2026-11-13")
        assertTrue(policy.tryStart("one:2026-11-14", true, false, now + 2_000L))
    }

    @Test
    fun doesNotStartWhenDisabledAlreadySignedOrAnotherAttemptIsRunning() {
        val policy = AutomaticSignInPolicy()

        assertFalse(policy.tryStart("one:today", false, false, 1L))
        assertFalse(policy.tryStart("one:today", true, true, 1L))
        assertFalse(policy.tryStart(null, true, false, 1L))
        assertTrue(policy.tryStart("one:today", true, null, 1L))
        assertFalse(policy.tryStart("two:today", true, false, 2L))
        policy.reset()
        assertTrue(policy.tryStart("two:today", true, false, 2L))
    }
}
