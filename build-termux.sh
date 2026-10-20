#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# build-termux.sh — One-click Android APK build on Termux
# - Installs required packages (JDK, Gradle, aapt2)
# - Overrides Gradle's x86_64 aapt2 with Termux's ARM64 version
# - Configures Android SDK path
# - Builds debug APK
# ============================================================
set -e

echo "==> Updating Termux packages..."
pkg update -y && pkg upgrade -y

echo "==> Installing required packages..."
pkg install -y openjdk-21 gradle android-tools aapt2 git wget unzip

echo "==> Configuring aapt2 override for ARM64..."
mkdir -p ~/.gradle
if ! grep -q "android.aapt2FromMavenOverride" ~/.gradle/gradle.properties 2>/dev/null; then
    echo "android.aapt2FromMavenOverride=/data/data/com.termux/files/usr/bin/aapt2" >> ~/.gradle/gradle.properties
    echo "    Added aapt2 override to ~/.gradle/gradle.properties"
else
    echo "    aapt2 override already present"
fi

echo "==> Cleaning old aapt2 cache (if any)..."
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.android.tools.build/aapt2 2>/dev/null || true

echo "==> Setting up Android SDK..."
SDK_DIR="$HOME/android-sdk"
if [ ! -d "$SDK_DIR/platforms" ]; then
    mkdir -p "$SDK_DIR"
    echo "    Android SDK not found at $SDK_DIR. Please install it first."
    echo "    You can use:"
    echo "      wget -O ~/install-android-sdk.sh https://raw.githubusercontent.com/Sohil876/termux-sdk-installer/main/installer.sh"
    echo "      chmod +x ~/install-android-sdk.sh && bash ~/install-android-sdk.sh -i"
    echo "    Then re-run this script."
    exit 1
fi

echo "==> Locating project..."
PROJECT_DIR="$HOME/SmartMusicPlayer"
if [ ! -d "$PROJECT_DIR" ]; then
    echo "    Project not found at $PROJECT_DIR. Cloning..."
    git clone https://github.com/mablue/SmartMusicPlayer.git "$PROJECT_DIR"
fi
cd "$PROJECT_DIR"

echo "==> Writing local.properties..."
echo "sdk.dir=$SDK_DIR" > local.properties

echo "==> Making gradlew executable..."
chmod +x gradlew

echo "==> Building debug APK (this may take a while)..."
./gradlew assembleDebug --no-daemon --stacktrace 2>&1 | tee build.log

echo ""
echo "=============================================================="
echo " Build finished. Check for APK at:"
echo "   $PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
echo "=============================================================="
