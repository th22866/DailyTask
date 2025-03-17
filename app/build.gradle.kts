import com.android.build.gradle.internal.api.ApkVariantOutputImpl
import java.text.SimpleDateFormat
import java.util.Locale

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pengxh.daily.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pengxh.daily.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 2051
        versionName = "2.0.5.1"
        flavorDimensions += "versionCode"
    }

    signingConfigs {
        create("release") {
            storeFile = file("DailyTask.jks")
            keyAlias = "key0"
            storePassword = "123456789"
            keyPassword = "123456789"
        }
    }

    buildTypes {
        val signConfig = signingConfigs.getByName("release")
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signConfig
        }
    }

    val id = "com.alibaba.android.${createRandomCode()}"
    productFlavors {
        create("daily") {
            dimension = "versionCode"
            applicationId = id
        }
    }

    val excludedFiles = listOf(
        "META-INF/NOTICE.md",
        "META-INF/LICENSE.md"
    )
    packaging {
        resources.excludes.addAll(excludedFiles)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    applicationVariants.all {
        outputs.all {
            if (this is ApkVariantOutputImpl) {
                outputFileName = "DT_${getBuildDate()}_${android.defaultConfig.versionName}.apk"
            }
        }
    }
}

fun createRandomCode(): String {
    val alphabetsInLowerCase = "abcdefghijklmnopqrstuvwxyz"
    var randomString = ""
    for (i in 0..5) {
        val randomIndex = (Math.random() * alphabetsInLowerCase.length).toInt()
        randomString += alphabetsInLowerCase[randomIndex]
    }
    return randomString
}

fun getBuildDate(): String {
    val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.CHINA)
    return dateFormat.format(System.currentTimeMillis())
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("com.github.AndroidCoderPeng:Kotlin-lite-lib:1.1.4")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    //异步响应式
    implementation("io.reactivex.rxjava2:rxjava:2.2.19")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.squareup.okhttp:okhttp:2.4.0")
    //上拉加载下拉刷新
    implementation("com.scwang.smartrefresh:SmartRefreshLayout:1.1.0")
    //沉浸式状态栏
    implementation("com.geyifeng.immersionbar:immersionbar:3.2.2")
    //数据库框架
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.5.1")
    //邮件
    implementation("com.sun.mail:android-mail:1.6.6")
    implementation("com.sun.mail:android-activation:1.6.6")
    //日期、时间选择器
    implementation("com.github.gzu-liyujiang.AndroidPicker:WheelPicker:4.1.13")
    //Kotlin协程
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    //官方Json解析库
    implementation("com.google.code.gson:gson:2.10.1")
    //异常日志记录
    implementation("com.tencent.bugly:crashreport:latest.release")
    //消息总线
    implementation("org.greenrobot:eventbus:3.3.1")
}
