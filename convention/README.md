# `org.spongepowered.gradle.convention`

Conventional configuration for Sponge's own projects.

- Applies indra
- Configures publication to Sponge repository (but does not enable it)
- Configures standard location for license headers
- When code signing is enabled, configures code signing to sign artifacts with Sponge's key
- Adds a 'sign jar' task to distribute signed jars

## Publishing

Because Sponge projects have a variety of needs for their publication, we do not directly apply any publishing option, only setting up options 
that will configure any publications that are added.

For various types of publishing, apply the appropriate indra plugin:

- `net.kyori.indra.publishing` - Just Sponge's repository
- `net.kyori.indra.publishing.sonatype` - also to Maven Central
- `net.kyori.indra.publishing.gradle-plugin` - also to Gradle Plugin Portal

## Properties used

The convention plugin uses several properties for configuring the build.

Property                | Description
----------------------- | ------------
`spongeSigningKey`      | The GPG key to sign artifacts
`spongeSigningPassword` | Password to decrypt the GPG key
`spongeSnapshotRepo`    | The repository URL to publish snapshots to
`spongeReleaseRepo`     | The repository URL to publish releases to
`spongeUsername`        | Username for publishing
`spongePassword`        | Password for publishing
