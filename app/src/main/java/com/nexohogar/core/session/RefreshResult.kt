package com.nexohogar.core.session

/**
 * Sealed class representing the outcome of a token refresh attempt.
 *
 * Used by [TokenRefreshCoordinator] and consumed by
 * [com.nexohogar.core.network.AuthInterceptor] and [SessionRefresher].
 */
sealed class RefreshResult {
    /** Refresh succeeded; [accessToken] is the new JWT. */
    data class Success(val accessToken: String) : RefreshResult()

    /** Token was already valid; no refresh was needed. */
    object AlreadyFresh : RefreshResult()

    /** Supabase rejected the refresh_token (revoked / expired). Session is unrecoverable. */
    object ServerRejected : RefreshResult()

    /** Transient network error; the refresh_token may still be valid. */
    data class NetworkError(val message: String) : RefreshResult()

    /** Convenience check: true when the access token is usable after this call. */
    val isSuccess: Boolean get() = this is Success || this is AlreadyFresh
}
