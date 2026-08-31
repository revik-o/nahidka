package org.orev.nahidka.api

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred

class UsersApi {

    fun login(login: String, password: String): Deferred<User> {
        val deferred = CompletableDeferred<User>()
        deferred.complete(User("1", login, "test@test.com"))
        return deferred
    }

    fun register(login: String, password: String): Deferred<User> {
        val deferred = CompletableDeferred<User>()
        deferred.complete(User("1", login, "test@test.com"))
        return deferred
    }

    fun oauthGoogle(token: String): Deferred<User> {
        val deferred = CompletableDeferred<User>()
        deferred.complete(User("1", "google_user", "test@google.com"))
        return deferred
    }

    fun getProfile(): Deferred<UserProfile> {
        val deferred = CompletableDeferred<UserProfile>()
        deferred.complete(UserProfile("1", "Hello World"))
        return deferred
    }

    fun updateProfile(profile: UserProfile): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }

    fun patchProfile(bio: String): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }

    fun getApplicationOptions(): Deferred<ApplicationOptions> {
        val deferred = CompletableDeferred<ApplicationOptions>()
        deferred.complete(ApplicationOptions("dark", "en"))
        return deferred
    }

    fun updateApplicationOptions(options: ApplicationOptions): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }

    fun patchApplicationOptions(theme: String): Deferred<Unit> {
        val deferred = CompletableDeferred<Unit>()
        deferred.complete(Unit)
        return deferred
    }
}
