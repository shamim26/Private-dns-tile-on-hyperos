$ErrorActionPreference = "Stop"

$sdkBuildTools = "C:\Android\sdk\build-tools\36.0.0"
$androidJar = "C:\Android\sdk\platforms\android-34\android.jar"
$projectDir = "C:\Users\User\Desktop\PrivateDnsTile"

Set-Location $projectDir

# Clean build artifacts
$buildDir = "$projectDir\build"
if (Test-Path $buildDir) {
    Remove-Item -Recurse -Force $buildDir
}
New-Item -ItemType Directory -Path "$buildDir\res_compiled" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\gen" | Out-Null
New-Item -ItemType Directory -Path "$buildDir\classes" | Out-Null

Write-Host "1. Compiling resources with aapt2..."
& "$sdkBuildTools\aapt2.exe" compile --dir "$projectDir\res" -o "$buildDir\res_compiled.zip"

Write-Host "2. Linking resources and generating R.java..."
& "$sdkBuildTools\aapt2.exe" link -I $androidJar `
    --manifest "$projectDir\AndroidManifest.xml" `
    --java "$buildDir\gen" `
    -o "$buildDir\app-unsigned.apk" `
    --auto-add-overlay `
    "$buildDir\res_compiled.zip"

Write-Host "3. Compiling Java sources with javac..."
$javaFiles = Get-ChildItem -Path "$buildDir\gen", "$projectDir\src" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName }
& javac -classpath $androidJar -d "$buildDir\classes" -source 1.8 -target 1.8 $javaFiles

Write-Host "4. Converting to DEX with d8..."
$classFiles = Get-ChildItem -Path "$buildDir\classes" -Recurse -Filter "*.class" | ForEach-Object { $_.FullName }
& "$sdkBuildTools\d8.bat" --output "$buildDir" --lib $androidJar $classFiles

Write-Host "5. Adding classes.dex to APK..."
# Use jar to add classes.dex into app-unsigned.apk
Set-Location $buildDir
& jar -uf "$buildDir\app-unsigned.apk" "classes.dex"
Set-Location $projectDir

Write-Host "6. Aligning APK with zipalign..."
& "$sdkBuildTools\zipalign.exe" -f -p 4 "$buildDir\app-unsigned.apk" "$buildDir\app-aligned.apk"

Write-Host "7. Signing APK with debug keystore..."
$keystore = "$projectDir\debug.keystore"
if (-not (Test-Path $keystore)) {
    Write-Host "Generating debug keystore..."
    & keytool -genkey -v -keystore $keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
}

& "$sdkBuildTools\apksigner.bat" sign --ks $keystore --ks-pass pass:android --ks-key-alias androiddebugkey --key-pass pass:android --out "$projectDir\PrivateDnsTile.apk" "$buildDir\app-aligned.apk"

Write-Host "Build complete! APK generated at: $projectDir\PrivateDnsTile.apk"
