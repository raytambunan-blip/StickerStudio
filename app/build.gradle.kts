plugins { id("com.android.application") }
android {
 namespace = "com.personal.stickerstudio"
 compileSdk = 35
 defaultConfig {
  applicationId = "com.personal.stickerstudio"
  minSdk = 28
  targetSdk = 34
  versionCode = 3
  versionName = "1.2"
  ndk { abiFilters += listOf("arm64-v8a") }
 }
 compileOptions {
  sourceCompatibility = JavaVersion.VERSION_17
  targetCompatibility = JavaVersion.VERSION_17
 }
}
dependencies {
  implementation("com.microsoft.onnxruntime:onnxruntime-android:1.28.0")
}
