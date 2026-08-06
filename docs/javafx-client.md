# JavaFX fallback client

`apps/javafx-client` is the deprecated desktop client retained as a separately
packaged rollback fallback and for critical fixes. New desktop UI work belongs in
the primary Compose client in `apps/desktop-client`.

## Runtime

- JDK 25 is required for the repository toolchain.
- The module uses JavaFX 25.0.2 and the `javafx.controls` module.
- The client reuses the shared chat, file-transfer, crypto, realtime, audio,
  webcam, and steganography modules.

## Run locally

From the repository root:

```powershell
.\gradlew.bat :apps:javafx-client:run
```

Use this client only when validating the fallback or investigating a JavaFX-specific
regression.

## Portable package

Build the JavaFX application image and portable ZIP:

```powershell
.\gradlew.bat :apps:javafx-client:buildPortable
```

Outputs:

- `apps/javafx-client/build/distributions/SecureLanSuite-<version>-portable.zip`
- `apps/javafx-client/build/packaging/SecureLanSuite/`

The portable task uses `jpackage --type app-image`.

## Windows EXE package

Build the JavaFX Windows installer:

```powershell
.\gradlew.bat :apps:javafx-client:buildExe
```

The direct task is also available:

```powershell
.\gradlew.bat :apps:javafx-client:createExe
```

The installer is written to:

- `apps/javafx-client/build/packaging/SecureLanSuite-<version>.exe`

Windows installer prerequisites and troubleshooting are maintained in the
[Windows installer guide](wix-installation.md).
