package org.orev.nahidka.api

class UsersApi {

    suspend fun login(login: String, password: String): User {
        return User("1", login, "test@test.com")
    }

    suspend fun register(login: String, password: String): User {
        return User("1", login, "test@test.com")
    }

    suspend fun oauthGoogle(token: String): User {
        return User("1", "google_user", "test@google.com")
    }

    suspend fun getProfile(): UserProfile {
        return UserProfile("1", "Hello World")
    }

    suspend fun updateProfile(profile: UserProfile) {
    }

    suspend fun patchProfile(bio: String) {
    }

    suspend fun getApplicationOptions(): ApplicationOptions {
        return ApplicationOptions("dark", "en")
    }

    suspend fun updateApplicationOptions(options: ApplicationOptions) {
    }

    suspend fun patchApplicationOptions(theme: String) {
    }
}
