#!/data/data/com.termux/files/usr/bin/bash
# ============================================================
# patch-mirrors.sh — 一键将 Gradle 仓库替换为镜像源
# 适用项目: ~/SmartMusicPlayer
# 功能:
#   1. 备份原 settings.gradle.kts
#   2. 重写为阿里云 + 伊朗镜像 + 官方回退的配置
#   3. 可选: 修改 gradle-wrapper.properties 使用腾讯云 Gradle 镜像
# ============================================================
set -e

PROJECT_DIR="$HOME/SmartMusicPlayer"
SETTINGS="$PROJECT_DIR/settings.gradle.kts"
WRAPPER="$PROJECT_DIR/gradle/wrapper/gradle-wrapper.properties"

if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ 项目目录不存在: $PROJECT_DIR"
    exit 1
fi

# ---------- 备份 ----------
if [ -f "$SETTINGS" ]; then
    cp "$SETTINGS" "$SETTINGS.bak.$(date +%s)"
    echo "✅ 已备份 settings.gradle.kts"
fi

# ---------- 写入镜像配置 ----------
cat > "$SETTINGS" << 'EOF'
pluginManagement {
    repositories {
        // ── 伊朗镜像 (优先尝试) ──────────────────────
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.devneeds.ir") }
        maven { url = uri("https://gradle.iranrepo.ir") }
        maven { url = uri("https://gradle.jamko.ir") }
        // ── 阿里云镜像 (中国) ────────────────────────
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // ── 官方仓库 (回退) ──────────────────────────
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // ── 伊朗镜像 (优先尝试) ──────────────────────
        maven { url = uri("https://maven.myket.ir") }
        maven { url = uri("https://maven.devneeds.ir") }
        maven { url = uri("https://gradle.iranrepo.ir") }
        maven { url = uri("https://gradle.jamko.ir") }
        // ── 阿里云镜像 (中国) ────────────────────────
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        // ── 官方仓库 (回退) ──────────────────────────
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SmartMusic"
include(":app")
EOF

echo "✅ settings.gradle.kts 已更新为镜像配置"

# ---------- 可选: 修改 Gradle Wrapper 下载地址 ----------
if [ -f "$WRAPPER" ]; then
    # 先备份
    cp "$WRAPPER" "$WRAPPER.bak.$(date +%s)"

    # 检测当前 distributionUrl 并尝试替换域名
    if grep -q "services.gradle.org" "$WRAPPER"; then
        # 使用腾讯云 Gradle 镜像
        sed -i 's#https\\://services.gradle.org/distributions/#https\\://mirrors.cloud.tencent.com/gradle/#' "$WRAPPER"
        echo "✅ Gradle Wrapper 已切换为腾讯云镜像"
    else
        echo "ℹ️  Gradle Wrapper 已使用非官方源，跳过修改"
    fi
fi

echo ""
echo "============================================================"
echo "🎉 镜像补丁完成！"
echo "============================================================"
echo ""
echo "下一步:"
echo "  1. 清理旧缓存 (可选，但建议)"
echo "     rm -rf ~/.gradle/caches/modules-2/files-2.1"
echo ""
echo "  2. 重新构建"
echo "     cd ~/SmartMusicPlayer"
echo "     ./gradlew assembleDebug --no-daemon --stacktrace 2>&1 | tee build.log"
echo ""
echo "============================================================"
