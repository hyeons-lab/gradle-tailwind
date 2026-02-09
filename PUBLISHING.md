# Publishing to Maven Central

This guide explains how to publish the Gradle Tailwind Plugin to Maven Central.

## Prerequisites

1. **Sonatype Central Portal Account**
   - Create an account at https://central.sonatype.com/
   - Verify your namespace: `com.hyeons-lab`

2. **GPG Key for Signing**
   - Required for Maven Central publishing
   - Signing artifacts ensures their authenticity

## Setup Instructions

### 1. Generate GPG Key (if you don't have one)

```bash
# Generate a new GPG key
gpg --gen-key

# Follow the prompts:
# - Real name: Your name or "Hyeon's Lab"
# - Email: your-email@example.com
# - Set a strong passphrase
```

### 2. Export and Upload GPG Key

```bash
# List your GPG keys
gpg --list-secret-keys --keyid-format LONG

# Output will look like:
# sec   rsa3072/ABCDEF1234567890 2024-01-01 [SC]
#       XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX
# uid   [ultimate] Your Name <your-email@example.com>

# Copy the key ID (e.g., ABCDEF1234567890)
KEY_ID="ABCDEF1234567890"

# Export the private key in ASCII-armored format
gpg --armor --export-secret-keys $KEY_ID > private-key.asc

# Upload public key to key servers (required by Maven Central)
gpg --keyserver keys.openpgp.org --send-keys $KEY_ID
gpg --keyserver keyserver.ubuntu.com --send-keys $KEY_ID
```

### 3. Get Sonatype Credentials

1. Log in to https://central.sonatype.com/
2. Go to your account settings
3. Generate a User Token
4. Save the username and password (token)

### 4. Configure `~/.gradle/gradle.properties`

Add these properties to `~/.gradle/gradle.properties` (create if it doesn't exist):

```properties
# Maven Central credentials from Sonatype Central Portal
mavenCentralUsername=<your-sonatype-username>
mavenCentralPassword=<your-sonatype-token>

# GPG signing credentials
signingInMemoryKeyId=<last-8-chars-of-key-id>
signingInMemoryKey=<paste-entire-contents-of-private-key.asc>
signingInMemoryKeyPassword=<your-gpg-passphrase>
```

**Important:**
- Replace `<your-sonatype-username>` with your Sonatype username
- Replace `<your-sonatype-token>` with the token password from Sonatype
- Replace `<last-8-chars-of-key-id>` with the last 8 characters of your GPG key ID (e.g., `34567890`)
- For `signingInMemoryKey`, copy the **entire contents** of `private-key.asc` (including `-----BEGIN PGP PRIVATE KEY BLOCK-----` and `-----END PGP PRIVATE KEY BLOCK-----`)
- Replace `<your-gpg-passphrase>` with your GPG key passphrase

**Security Note:** The `~/.gradle/gradle.properties` file is in your home directory and should never be committed to version control.

## Publishing Commands

### Test Locally

```bash
# Publish to Maven Local for testing
./gradlew :plugin:publishToMavenLocal

# Plugin will be available at:
# ~/.m2/repository/com/hyeons-lab/gradle-tailwind/0.3.0/
```

### Publish to Maven Central (Staging)

```bash
# Publish to Maven Central staging repository
./gradlew :plugin:publishToMavenCentral

# This will:
# 1. Build and test the plugin
# 2. Sign all artifacts with your GPG key
# 3. Upload to Sonatype Central Portal (staging)
# 4. NOT automatically release (MAVEN_PUBLISH_AUTOMATIC_RELEASE=false)
```

### Manual Release via Web UI

1. Go to https://central.sonatype.com/
2. Navigate to "Deployments"
3. Find your deployment
4. Review the artifacts
5. Click "Publish" to release to Maven Central

### Automatic Release (Advanced)

To enable automatic release after successful upload:

1. Update `gradle.properties`:
   ```properties
   MAVEN_PUBLISH_AUTOMATIC_RELEASE=true
   ```

2. Publish with automatic release:
   ```bash
   ./gradlew :plugin:publishAndReleaseToMavenCentral
   ```

**Warning:** Automatic release cannot be undone. Use manual release for your first few publishes.

## Verification

After publishing, your artifact will be available at:
- **Maven Central**: https://central.sonatype.com/artifact/com.hyeons-lab/gradle-tailwind
- **Search**: https://search.maven.org/artifact/com.hyeons-lab/gradle-tailwind

**Note:** It may take 15-30 minutes for artifacts to appear on Maven Central after release.

## Troubleshooting

### "Cannot perform signing task"
- Ensure your GPG credentials are correctly configured in `~/.gradle/gradle.properties`
- Verify the `signingInMemoryKey` contains the complete private key

### "401 Unauthorized"
- Check your `mavenCentralUsername` and `mavenCentralPassword` are correct
- Regenerate your Sonatype token if needed

### "403 Forbidden"
- Verify you have verified ownership of the `com.hyeons-lab` namespace on Sonatype
- Contact Sonatype support if namespace verification is pending

### "Artifact already exists"
- You cannot republish the same version to Maven Central
- Increment the version number in `plugin/build.gradle.kts`

## Version Management

To publish a new version:

1. Update version in `plugin/build.gradle.kts`:
   ```kotlin
   version = "0.3.1"  // or "0.4.0", etc.
   ```

2. Commit the version change:
   ```bash
   git add plugin/build.gradle.kts
   git commit -m "Bump version to 0.3.1"
   git tag v0.3.1
   git push && git push --tags
   ```

3. Publish:
   ```bash
   ./gradlew :plugin:publishToMavenCentral
   ```

## Resources

- [Sonatype Central Portal Documentation](https://central.sonatype.org/publish/publish-portal-gradle/)
- [Vanniktech Maven Publish Plugin](https://github.com/vanniktech/gradle-maven-publish-plugin)
- [GPG Signing Guide](https://central.sonatype.org/publish/requirements/gpg/)
