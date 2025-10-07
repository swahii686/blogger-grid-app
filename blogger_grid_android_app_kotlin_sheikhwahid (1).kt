# Project: Blogger Grid Android App (Kotlin)

---

# README

This project fetches posts from a Blogger blog's JSON feed and displays them in a Pinterest-style grid. Tapping a card opens the post inside the app using a WebView.

Blog used: https://www.sheikhwahid.in

Default feed URL (Blogger JSON): `https://www.sheikhwahid.in/feeds/posts/default?alt=json&max-results=20`

If your blog uses a different feed or you want more posts, change the feedUrl constant in `MainActivity.kt`.

---

=== File: app/build.gradle (Module: app) ===
```gradle
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
}

android {
    namespace 'com.example.bloggergrid'
    compileSdk 34

    defaultConfig {
        applicationId 'com.example.bloggergrid'
        minSdk 21
        targetSdk 34
        versionCode 1
        versionName '1.0'

        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = '17'
    }
}

dependencies {
    implementation 'androidx.core:core-ktx:1.12.0'
    implementation 'androidx.appcompat:appcompat:1.7.0'
    implementation 'com.google.android.material:material:1.10.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.4.1'
    implementation 'androidx.recyclerview:recyclerview:1.3.1'
    implementation 'androidx.swiperefreshlayout:swiperefreshlayout:1.2.0-alpha01'

    // Networking
    implementation 'com.squareup.okhttp3:okhttp:4.11.0'

    // Image loading
    implementation 'com.github.bumptech.glide:glide:4.16.0'
    kapt 'com.github.bumptech.glide:compiler:4.16.0'

    // WebView + lifecycle
    implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.6.2'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.5'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.5.1'
}
```

---

=== File: AndroidManifest.xml ===
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.bloggergrid">

    <uses-permission android:name="android.permission.INTERNET" />

    <application
        android:allowBackup="true"
        android:label="SheikhWahid"
        android:theme="@style/Theme.BloggerGrid">
        <activity android:name="com.example.bloggergrid.PostWebActivity" />
        <activity android:name="com.example.bloggergrid.MainActivity">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

---

=== File: app/src/main/res/layout/activity_main.xml ===
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.swiperefreshlayout.widget.SwipeRefreshLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:id="@+id/swipeRefresh"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    tools:context=".MainActivity">

    <androidx.recyclerview.widget.RecyclerView
        android:id="@+id/recyclerView"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</androidx.swiperefreshlayout.widget.SwipeRefreshLayout>
```

---

=== File: app/src/main/res/layout/item_post.xml ===
```xml
<?xml version="1.0" encoding="utf-8"?>
<androidx.cardview.widget.CardView xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:card_view="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    card_view:cardCornerRadius="12dp"
    android:layout_margin="8dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical">

        <ImageView
            android:id="@+id/postImage"
            android:layout_width="match_parent"
            android:layout_height="180dp"
            android:scaleType="centerCrop" />

        <TextView
            android:id="@+id/postTitle"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:padding="8dp"
            android:textSize="16sp"
            android:maxLines="2"
            android:ellipsize="end" />

    </LinearLayout>

</androidx.cardview.widget.CardView>
```

---

=== File: app/src/main/java/com/example/bloggergrid/Post.kt ===
```kotlin
package com.example.bloggergrid

data class Post(
    val title: String,
    val url: String,
    val imageUrl: String?
)
```

---

=== File: app/src/main/java/com/example/bloggergrid/PostAdapter.kt ===
```kotlin
package com.example.bloggergrid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class PostAdapter(private val items: List<Post>, private val onClick: (Post) -> Unit) :
    RecyclerView.Adapter<PostAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val image: ImageView = view.findViewById(R.id.postImage)
        val title: TextView = view.findViewById(R.id.postTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_post, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val post = items[position]
        holder.title.text = post.title
        val img = post.imageUrl
        if (!img.isNullOrEmpty()) {
            Glide.with(holder.image.context).load(img).centerCrop().into(holder.image)
        } else {
            holder.image.setImageResource(android.R.color.darker_gray)
        }

        holder.itemView.setOnClickListener { onClick(post) }
    }

    override fun getItemCount(): Int = items.size
}
```

---

=== File: app/src/main/java/com/example/bloggergrid/MainActivity.kt ===
```kotlin
package com.example.bloggergrid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private val client = OkHttpClient()

    // Change feed URL if needed
    private val feedUrl = "https://www.sheikhwahid.in/feeds/posts/default?alt=json&max-results=40"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        recyclerView = findViewById(R.id.recyclerView)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        recyclerView.layoutManager = GridLayoutManager(this, 2)

        swipeRefresh.setOnRefreshListener {
            fetchPosts()
        }

        swipeRefresh.isRefreshing = true
        fetchPosts()
    }

    private fun fetchPosts() {
        val request = Request.Builder().url(feedUrl).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    swipeRefresh.isRefreshing = false
                    // show simple error (could add Snackbar)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val body = it.body?.string() ?: ""
                    val posts = parseBloggerJson(body)
                    runOnUiThread {
                        swipeRefresh.isRefreshing = false
                        recyclerView.adapter = PostAdapter(posts) { post ->
                            val intent = Intent(this@MainActivity, PostWebActivity::class.java)
                            intent.putExtra("url", post.url)
                            startActivity(intent)
                        }
                    }
                }
            }
        })
    }

    private fun parseBloggerJson(jsonStr: String): List<Post> {
        val results = mutableListOf<Post>()
        try {
            val root = JSONObject(jsonStr)
            val feed = root.optJSONObject("feed")
            val entries = feed?.optJSONArray("entry") ?: return results

            for (i in 0 until entries.length()) {
                val e = entries.getJSONObject(i)
                val title = e.optJSONObject("title")?.optString("$t") ?: e.optString("title")

                // best attempt to get link with rel='alternate'
                var url = ""
                val links = e.optJSONArray("link")
                if (links != null) {
                    for (j in 0 until links.length()) {
                        val l = links.getJSONObject(j)
                        if (l.optString("rel") == "alternate") {
                            url = l.optString("href")
                            break
                        }
                    }
                }

                // get media:thumbnail or parse content for first image
                var imageUrl: String? = null
                val media = e.optJSONObject("media$thumbnail")
                if (media != null) {
                    imageUrl = media.optString("url")
                }

                // try y:content or content
                if (imageUrl.isNullOrEmpty()) {
                    val contentObj = e.optJSONObject("content")
                    val contentStr = contentObj?.optString("$t") ?: e.optString("content")
                    if (!contentStr.isNullOrEmpty()) {
                        // simple regex to find first img src
                        val regex = Regex("<img[^>]+src=\\\"([^\\\"]+)\\\"", RegexOption.IGNORE_CASE)
                        val match = regex.find(contentStr)
                        if (match != null && match.groupValues.size > 1) {
                            imageUrl = match.groupValues[1]
                        }
                    }
                }

                // Fallback: get thumbnail in media$thumbnail alternative name
                if (imageUrl.isNullOrEmpty()) {
                    val thumbnails = e.optJSONArray("media$thumbnail")
                    if (thumbnails != null && thumbnails.length() > 0) {
                        imageUrl = thumbnails.getJSONObject(0).optString("url")
                    }
                }

                val cleanTitle = e.optJSONObject("title")?.optString("$t") ?: e.optString("title")

                if (cleanTitle.isNullOrEmpty().not() && url.isNotEmpty()) {
                    results.add(Post(cleanTitle, url, imageUrl))
                }
            }
        } catch (ex: Exception) {
            Log.e("MainActivity", "parse error", ex)
        }
        return results
    }
}
```

---

=== File: app/src/main/java/com/example/bloggergrid/PostWebActivity.kt ===
```kotlin
package com.example.bloggergrid

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class PostWebActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)

        val url = intent.getStringExtra("url") ?: ""

        val ws: WebSettings = webView.settings
        ws.javaScriptEnabled = true
        ws.domStorageEnabled = true

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
```

---

# Notes & Next steps

1. Open Android Studio, choose "Import project" or create new project and replace files.
2. Replace `feedUrl` in `MainActivity.kt` if you want more posts or different parameters.
3. The JSON from Blogger has nested keys like `title.$t` or `content.$t`; the parser tries to read both. If your blog's JSON differs, tweak `parseBloggerJson` accordingly.
4. For better parsing, you can switch to Retrofit + Moshi / GSON.
5. To make grid spacing prettier, add `ItemDecoration` or use `StaggeredGridLayoutManager` for staggered heights.
6. To enable caching and offline, configure OkHttp cache and enable Glide caching.

---

# Credits
- Uses OkHttp and Glide libraries.

---

End of project files.

---

=== File: codemagic.yaml ===
```yaml
workflows:
  android-workflow:
    name: Build Android APK
    max_build_duration: 60
    environment:
      vars:
        PACKAGE_NAME: "com.example.bloggergrid"
    scripts:
      - name: Install Java
        script: |
          echo "Using default environment"
      - name: Build APK
        script: |
          chmod +x ./gradlew
          ./gradlew assembleRelease --stacktrace
    artifacts:
      - app/build/outputs/**/*.apk
    publishing:
      email:
        recipients:
          - your-email@example.com
```

---

# Upload & Build instructions (copy into your repo README)
1. Create a new GitHub repository (e.g., `sheikhwahid-android`).
2. Upload the entire project folder contents (all files and folders) to the repository root.
   - Recommended: zip the project locally and then use GitHub web "Upload files", or push via Git.
3. Ensure `gradlew`, `gradlew.bat`, and the `gradle/` wrapper are included (they are required by Codemagic).
4. Connect Codemagic to your GitHub account and grant access to the repository.
5. In Codemagic, select the repository and the `android-workflow` (it will detect gradle). Start the build.
6. After build completes, download the generated APK from the Artifacts.

---

Note: Replace `your-email@example.com` in `codemagic.yaml` with your actual email if you want build notifications.

