import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension

/**
 * Configure signing for Maven Central publications.
 * Prefers in-memory keys if provided via Gradle properties
 * (e.g., signingInMemoryKey* in ~/.gradle/gradle.properties or CI environment),
 * otherwise falls back to standard Gradle signing properties if present.
 */
fun Project.configureSigning() {
    extensions.configure<SigningExtension>("signing") {
        // Read optional in-memory GPG credentials from gradle.properties
        val keyId = findProperty("signingInMemoryKeyId") as String?
        val key = findProperty("signingInMemoryKey") as String?
        val keyPass = findProperty("signingInMemoryKeyPassword") as String?

        if (!key.isNullOrBlank() && !keyPass.isNullOrBlank()) {
            // When keyId is null, Gradle will derive it from the key
            useInMemoryPgpKeys(keyId, key, keyPass)
            println("Signing configured with in-memory PGP key")
        } else {
            println("WARNING: No signing credentials found. Publications will not be signed.")
            println("To sign releases, add signingInMemoryKey and signingInMemoryKeyPassword to ~/.gradle/gradle.properties")
        }

        // Sign all publications created by the Maven Publish plugin
        val publishing = extensions.findByName("publishing")
        if (publishing != null) {
            @Suppress("UNCHECKED_CAST")
            val publishingExtension = publishing as PublishingExtension
            sign(publishingExtension.publications)
        }
    }
}
