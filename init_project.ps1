$ProjectDir = $PWD

function Write-File {
    param([string]$Path, [string]$Content)
    $fullPath = Join-Path $ProjectDir $Path
    $dir = Split-Path $fullPath
    if (-not (Test-Path $dir)) {
        New-Item -ItemType Directory -Path $dir | Out-Null
    }
    Set-Content -Path $fullPath -Value $Content -Encoding UTF8
    Write-Host "Created $Path"
}

Write-File "settings.gradle.kts" @"
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = `"agentShell`"
include(`":app`")
"@

Write-File "build.gradle.kts" @"
plugins {
    id(`"com.android.application`") version `"8.5.0`" apply false
    id(`"org.jetbrains.kotlin.android`") version `"2.0.0`" apply false
    id(`"com.google.dagger.hilt.android`") version `"2.51.1`" apply false
}
"@

Write-File "gradle.properties" @"
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.nonTransitiveRClass=true
"@

Write-File "app/build.gradle.kts" @"
plugins {
    id(`"com.android.application`")
    id(`"org.jetbrains.kotlin.android`")
    id(`"kotlin-kapt`")
    id(`"com.google.dagger.hilt.android`")
}

android {
    namespace = `"dev.agentshell.app`"
    compileSdk = 35

    defaultConfig {
        applicationId = `"dev.agentshell.app`"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = `"1.0`"

        testInstrumentationRunner = `"androidx.test.runner.AndroidJUnitRunner`"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile(`"proguard-android-optimize.txt`"), `"proguard-rules.pro`")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = `"17`"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = `"1.5.14`"
    }
    packaging {
        resources {
            excludes += `"/META-INF/{AL2.0,LGPL2.1}`"
        }
    }
}

dependencies {
    implementation(`"androidx.core:core-ktx:1.13.1`")
    implementation(`"androidx.lifecycle:lifecycle-runtime-ktx:2.8.3`")
    implementation(`"androidx.activity:activity-compose:1.9.0`")
    implementation(platform(`"androidx.compose:compose-bom:2024.06.00`"))
    implementation(`"androidx.compose.ui:ui`")
    implementation(`"androidx.compose.ui:ui-graphics`")
    implementation(`"androidx.compose.ui:ui-tooling-preview`")
    implementation(`"androidx.compose.material3:material3`")
    
    implementation(`"com.google.dagger:hilt-android:2.51.1`")
    kapt(`"com.google.dagger:hilt-android-compiler:2.51.1`")

    testImplementation(`"junit:junit:4.13.2`")
    androidTestImplementation(`"androidx.test.ext:junit:1.2.1`")
    androidTestImplementation(`"androidx.test.espresso:espresso-core:3.6.1`")
    androidTestImplementation(platform(`"androidx.compose:compose-bom:2024.06.00`"))
    androidTestImplementation(`"androidx.compose.ui:ui-test-junit4`")
    debugImplementation(`"androidx.compose.ui:ui-tooling`")
    debugImplementation(`"androidx.compose.ui:ui-test-manifest`")
}
"@

Write-File "app/src/main/AndroidManifest.xml" @"
<?xml version=`"1.0`" encoding=`"utf-8`"?>
<manifest xmlns:android=`"http://schemas.android.com/apk/res/android`"
    package=`"dev.agentshell.app`">

    <uses-permission android:name=`"android.permission.FOREGROUND_SERVICE`"/>
    <uses-permission android:name=`"android.permission.POST_NOTIFICATIONS`"/>
    <uses-permission android:name=`"android.permission.INTERNET`"/>

    <application
        android:name=`".App`"
        android:allowBackup=`"true`"
        android:icon=`"@mipmap/ic_launcher`"
        android:label=`"agentShell`"
        android:roundIcon=`"@mipmap/ic_launcher_round`"
        android:supportsRtl=`"true`"
        android:theme=`"@style/Theme.AgentShell`">
        <activity
            android:name=`".MainActivity`"
            android:exported=`"true`"
            android:theme=`"@style/Theme.AgentShell`">
            <intent-filter>
                <action android:name=`"android.intent.action.MAIN`" />
                <category android:name=`"android.intent.category.LAUNCHER`" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"@

Write-File "app/src/main/res/values/themes.xml" @"
<?xml version=`"1.0`" encoding=`"utf-8`"?>
<resources>
    <style name=`"Theme.AgentShell`" parent=`"android:Theme.Material.Light.NoActionBar`" />
</resources>
"@

Write-File "app/src/main/kotlin/dev/agentshell/app/App.kt" @"
package dev.agentshell.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
"@

Write-File "app/src/main/kotlin/dev/agentshell/app/ui/theme/Color.kt" @"
package dev.agentshell.app.ui.theme

import androidx.compose.ui.graphics.Color

object AgentShellColors {
    val Shell0 = Color(0xFF1C0F09)
    val Shell1 = Color(0xFF2D1810)
    val Shell2 = Color(0xFF4A2C1E)
    val Shell3 = Color(0xFF6B4030)
    val Shell4 = Color(0xFF8C5A3C)

    val Text0 = Color(0xFFF2E6CC)
    val Text1 = Color(0xFFE8D4B0)
    val Text2 = Color(0xFFC4A882)
    val Text3 = Color(0xFF9A7A5A)
    val Text4 = Color(0xFF6B5540)

    val Amber = Color(0xFFB89450)
    val AmberLow = Color(0xFF7A5C28)

    val Success = Color(0xFF6A9A6A)
    val Error = Color(0xFFC45040)
    val Info = Color(0xFF5A7A9A)
    val Warning = Color(0xFFB87830)

    val TermBg = Color(0xFF0F0704)
    val TermFg = Color(0xFFE8D4B0)
    val TermCmd = Color(0xFFB89450)
    val TermOut = Color(0xFFC4A882)
    val TermErr = Color(0xFFC45040)
    val TermSys = Color(0xFF6A9A6A)
    val TermCur = Color(0xFFF2E6CC)
    val TermSel = Color(0xFF4A2C1E)
}
"@

Write-File "app/src/main/kotlin/dev/agentshell/app/ui/theme/Shape.kt" @"
package dev.agentshell.app.ui.theme

import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.material3.Shapes

val AgentShellShapes = Shapes(
    small = RectangleShape,
    medium = RectangleShape,
    large = RectangleShape
)
"@

Write-File "app/src/main/kotlin/dev/agentshell/app/ui/theme/Theme.kt" @"
package dev.agentshell.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    background = AgentShellColors.Shell0,
    surface = AgentShellColors.Shell1,
    primary = AgentShellColors.Amber,
    onPrimary = AgentShellColors.Shell0,
    onBackground = AgentShellColors.Text1,
    onSurface = AgentShellColors.Text1,
    error = AgentShellColors.Error
)

@Composable
fun AgentShellTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        shapes = AgentShellShapes,
        content = content
    )
}
"@

Write-File "app/src/main/kotlin/dev/agentshell/app/MainActivity.kt" @"
package dev.agentshell.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import dev.agentshell.app.ui.theme.AgentShellTheme
import dev.agentshell.app.ui.theme.AgentShellColors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AgentShellTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AgentShellColors.Shell0),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = `"[SYS] / INIT ···`",
                        color = AgentShellColors.Amber
                    )
                }
            }
        }
    }
}
"@

Write-Host "Project initialization complete."
