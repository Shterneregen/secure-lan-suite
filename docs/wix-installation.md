# WiX Installation Guide for Secure LAN Suite

This project uses `jpackage` to build a Windows EXE installer. For the current Secure LAN Suite build, the verified working setup is:

- JDK 25
- Gradle 9.1+
- WiX 5.0.2
- WiX extensions:
  - `WixToolset.UI.wixext`
  - `WixToolset.Util.wixext`

> Important: do **not** use WiX 7 for this project packaging flow. The working `jpackage` setup was validated with WiX 5.0.2.

## Prerequisites
- Windows
- .NET SDK 8 or newer
- JDK 25 installed
- PowerShell or Command Prompt restarted after tool installation if needed

## 1. Install the .NET SDK

You can install it with `winget`:

```powershell
winget install Microsoft.DotNet.SDK.8
```

Verify the installation:

```powershell
dotnet --version
```

## 2. Make sure NuGet has an enabled source

If your machine has no NuGet sources configured, add the official `nuget.org` feed:

```powershell
dotnet nuget add source https://api.nuget.org/v3/index.json -n nuget.org
dotnet nuget list source
```

Expected result: `nuget.org` is listed as `Enabled`.

## 3. Install WiX 5.0.2

Install WiX as a global .NET tool:

```powershell
dotnet tool install --global wix --version 5.0.2
```

If WiX was already installed in another version, update or reinstall it explicitly:

```powershell
dotnet tool update --global wix --version 5.0.2
```

Verify the installed version:

```powershell
wix --version
```

Expected output should look like:

```text
5.0.2+...
```

## 4. Install the required WiX extensions

`jpackage` uses WiX extensions during EXE creation. Install them globally with the same version as WiX:

```powershell
wix extension add --global WixToolset.UI.wixext/5.0.2
wix extension add --global WixToolset.Util.wixext/5.0.2
wix extension list --global
```

Expected result:

```text
WixToolset.UI.wixext 5.0.2
WixToolset.Util.wixext 5.0.2
```

## 5. Fix PATH if `wix` is not recognized

If installation succeeded but PowerShell still says that `wix` is not recognized, either reopen the terminal or add the global tools directory to the user `PATH`.

Temporary fix for the current PowerShell session:

```powershell
$env:PATH += ";$env:USERPROFILE\.dotnet\tools"
```

Permanent user-level fix:

```powershell
[Environment]::SetEnvironmentVariable(
  "Path",
  [Environment]::GetEnvironmentVariable("Path", "User") + ";$env:USERPROFILE\.dotnet\tools",
  "User"
)
```

Then reopen PowerShell.

## 6. Verify the full packaging environment

```powershell
java --version
jpackage --version
wix --version
wix extension list --global
.\gradlew.bat :apps:desktop-client:printPackagingEnvironment
```

## 7. Build the Windows EXE installer

```powershell
.\gradlew.bat :apps:desktop-client:createExe
```

or

```powershell
.\gradlew.bat :apps:desktop-client:buildExe
```

The generated EXE installer is written to:

```text
apps/desktop-client/build/jpackage/
```

Expected output file:

```text
apps/desktop-client/build/jpackage/SecureLanSuite-0.1.0.exe
```

## 8. Build the portable ZIP package

```powershell
.\gradlew.bat :apps:desktop-client:buildPortable
```

The generated ZIP package is written to:

```text
apps/desktop-client/build/distributions/
```

## Troubleshooting

### `No NuGet sources are defined or enabled`
Add the official source again:

```powershell
dotnet nuget add source https://api.nuget.org/v3/index.json -n nuget.org
```

### `wix : The term 'wix' is not recognized`
WiX is installed, but the .NET global tools path is not visible in the current terminal session. Add `$env:USERPROFILE\.dotnet\tools` to `PATH` and reopen the shell.

### `WiX Toolset was not found in PATH`
Run:

```powershell
wix --version
.\gradlew.bat :apps:desktop-client:printPackagingEnvironment
```

If `wix --version` works but Gradle still fails, start a fresh terminal so Gradle inherits the updated `PATH`.

### `The configured main jar does not exist ... in the input directory`
This means the packaging task is pointing `jpackage` to an input directory that does not contain the built application JAR. Re-run a clean build and make sure the JAR exists before running `createExe`:

```powershell
.\gradlew.bat clean :apps:desktop-client:jar
Get-ChildItem .\apps\desktop-client\build\libs
```

### `wix.exe ... exited with 144 code`
For this project, that usually means one of the required WiX extensions is missing or installed in the wrong version. Verify that WiX 5.0.2 is installed and both extensions are present:

```powershell
wix --version
wix extension list --global
```

Expected:

```text
WixToolset.UI.wixext 5.0.2
WixToolset.Util.wixext 5.0.2
```

### `jpackage was not found`
Make sure JDK 25 is installed and active:

```powershell
java --version
jpackage --version
```

If needed, set `JAVA_HOME` to your JDK 25 installation and reopen the terminal.
