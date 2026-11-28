package com.usama.dailytodo

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import kotlin.concurrent.thread

// pri: 0 high, 1 medium, 2 low.  hidden: 0 visible, 1 deleted, 2 cleared (kept in History)
data class Task(
    val id: String,
    val text: String,
    val done: Boolean,
    val pri: Int,
    val created: Long = 0L,
    val doneAt: Long = 0L,
    val hidden: Int = 0
)

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("todo", Context.MODE_PRIVATE)
    private val dayKey = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    // 0 = follow system, 1 = light, 2 = dark
    var theme: Int
        get() = sp.getInt("theme", 0).coerceIn(0, 2)
        set(v) { sp.edit().putInt("theme", v).apply() }

    fun load(): Map<String, List<Task>> {
        val raw = sp.getString("data", null) ?: return emptyMap()
        val m = mutableMapOf<String, List<Task>>()
        try {
            val o = JSONObject(raw)
            o.keys().forEach { k ->
                if (!dayKey.matches(k)) return@forEach
                val a = o.optJSONArray(k) ?: return@forEach
                val list = mutableListOf<Task>()
                for (i in 0 until minOf(a.length(), 2000)) {
                    val t = a.optJSONObject(i) ?: continue
                    val text = t.optString("text", "")
                    if (text.isEmpty()) continue
                    list.add(
                        Task(
                            id = t.optString("id", "").ifEmpty { UUID.randomUUID().toString() },
                            text = text.take(200),
                            done = t.optBoolean("done", false),
                            pri = t.optInt("pri", 1).coerceIn(0, 2),
                            created = t.optLong("created", 0L),
                            doneAt = t.optLong("doneAt", 0L),
                            hidden = t.optInt("hidden", 0).coerceIn(0, 2)
                        )
                    )
                }
                m[k] = list
            }
        } catch (e: Exception) {
            // damaged data is ignored instead of crashing the app
        }
        return m
    }

    fun save(m: Map<String, List<Task>>) {
        val o = JSONObject()
        m.forEach { (k, v) ->
            o.put(k, JSONArray(v.map {
                JSONObject()
                    .put("id", it.id).put("text", it.text).put("done", it.done).put("pri", it.pri)
                    .put("created", it.created).put("doneAt", it.doneAt).put("hidden", it.hidden)
            }))
        }
        sp.edit().putString("data", o.toString()).apply()
    }
}

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0E7466), onPrimary = Color.White,
    secondaryContainer = Color(0xFFCDEBE5), onSecondaryContainer = Color(0xFF0B3B34),
    background = Color(0xFFE9EEF3), onBackground = Color(0xFF14202C),
    surface = Color.White, onSurface = Color(0xFF14202C), outline = Color(0xFF7A8794)
)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFF3CC4AE), onPrimary = Color(0xFF06211C),
    secondaryContainer = Color(0xFF1F4A44), onSecondaryContainer = Color(0xFFCDEBE5),
    background = Color(0xFF0F1720), onBackground = Color(0xFFE8EEF4),
    surface = Color(0xFF17222E), onSurface = Color(0xFFE8EEF4), outline = Color(0xFF5E7183)
)

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = if (dark) DarkScheme else LightScheme) {
        Surface(color = MaterialTheme.colorScheme.background, content = content)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            var mode by remember { mutableIntStateOf(store.theme) }
            val dark = when (mode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            AppTheme(dark) {
                TodoApp(store, mode) { m -> mode = m; store.theme = m }
            }
        }
    }
}

private val priLabels = listOf("High", "Medium", "Low")
private val priColors = listOf(Color(0xFFC2410C), Color(0xFFA16207), Color(0xFF2563A8))

@Composable
fun ThemeMenu(mode: Int, onTheme: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) { Text("Theme") }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            listOf("System default", "Light", "Dark").forEachIndexed { i, label ->
                DropdownMenuItem(
                    text = { Text((if (mode == i) "\u2713  " else "     ") + label) },
                    onClick = { onTheme(i); open = false }
                )
            }
        }
    }
}

// Small month calendar shown as a popover under the date (like the Windows date picker)
@Composable
fun CompactCalendar(selected: LocalDate, onPick: (LocalDate) -> Unit) {
    var month by remember { mutableStateOf(selected.withDayOfMonth(1)) }
    val today = LocalDate.now()
    val start = month.minusDays((month.dayOfWeek.value % 7).toLong())
    val primary = MaterialTheme.colorScheme.primary
    Column(Modifier.width(264.dp).padding(horizontal = 6.dp, vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                month.format(DateTimeFormatter.ofPattern("MMMM yyyy")), fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 6.dp)
            )
            TextButton(onClick = { month = month.minusMonths(1) }) { Text("\u2039") }
            TextButton(onClick = { month = month.plusMonths(1) }) { Text("\u203A") }
        }
        Row {
            listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa").forEach {
                Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                    Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .6f))
                }
            }
        }
        for (row in 0 until 6) {
            Row {
                for (col in 0 until 7) {
                    val d = start.plusDays((row * 7 + col).toLong())
                    val inMonth = d.month == month.month
                    val isSel = d == selected
                    Box(
                        Modifier.size(36.dp).padding(2.dp).clip(CircleShape)
                            .then(
                                if (isSel) Modifier.background(primary)
                                else if (d == today) Modifier.border(1.dp, primary, CircleShape)
                                else Modifier
                            )
                            .clickable { onPick(d) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "${d.dayOfMonth}", fontSize = 13.sp,
                            color = when {
                                isSel -> MaterialTheme.colorScheme.onPrimary
                                inMonth -> MaterialTheme.colorScheme.onSurface
                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = .35f)
                            }
                        )
                    }
                }
            }
        }
        TextButton(onClick = { onPick(today) }, modifier = Modifier.align(Alignment.End)) { Text("Today") }
    }
}

// ---------- Update check (GitHub Releases) ----------
private const val RELEASES_API = "https://api.github.com/repos/usamabhatti7/daily-todo/releases/latest"
private const val RELEASES_PREFIX = "https://github.com/usamabhatti7/daily-todo/releases/"

sealed class UpdateState {
    object Checking : UpdateState()
    object UpToDate : UpdateState()
    data class Available(val version: String, val apkUrl: String?, val pageUrl: String) : UpdateState()
    data class Failed(val message: String) : UpdateState()
}

fun versionParts(v: String): List<Int> =
    v.trim().removePrefix("v").split(".").map { p -> p.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

fun isNewer(latest: String, current: String): Boolean {
    val a = versionParts(latest)
    val b = versionParts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

// Runs on a background thread. Only talks to api.github.com over HTTPS and only accepts
// download links that point into this repository's releases.
fun checkForUpdate(current: String, onResult: (UpdateState) -> Unit) {
    thread {
        val result: UpdateState = try {
            val conn = URL(RELEASES_API).openConnection() as HttpsURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "DailyTodo-Android")
            if (conn.responseCode != 200) {
                UpdateState.Failed("GitHub answered with code ${conn.responseCode}.")
            } else {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val o = JSONObject(body)
                val tag = o.optString("tag_name", "")
                var apk: String? = null
                val assets = o.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val item = assets.optJSONObject(i) ?: continue
                        val url = item.optString("browser_download_url", "")
                        if (item.optString("name") == "DailyTodo.apk" && url.startsWith(RELEASES_PREFIX)) apk = url
                    }
                }
                val htmlUrl = o.optString("html_url", "")
                val page = if (htmlUrl.startsWith(RELEASES_PREFIX)) htmlUrl else RELEASES_PREFIX + "latest"
                if (tag.isNotEmpty() && isNewer(tag, current)) UpdateState.Available(tag.removePrefix("v"), apk, page)
                else UpdateState.UpToDate
            }
        } catch (e: Exception) {
            UpdateState.Failed("Could not reach GitHub. Check your internet connection and try again.")
        }
        onResult(result)
    }
}

fun openUrl(ctx: Context, url: String) {
    try {
        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: Exception) {
        // no browser available
    }
}

// Reads the version from the installed app, so it always matches versionName in build.gradle.kts
@Composable
fun appVersion(): String {
    val ctx = LocalContext.current
    return remember {
        try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (e: Exception) { "" }
    }
}

@Composable
fun TodoApp(store: Store, themeMode: Int = 0, onTheme: (Int) -> Unit = {}) {
    var data by remember { mutableStateOf(store.load()) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var tab by remember { mutableIntStateOf(0) }
    var updateState by remember { mutableStateOf<UpdateState?>(null) }
    val ver = appVersion()
    val ctx = LocalContext.current

    fun put(changes: Map<String, List<Task>>) { data = data + changes; store.save(data) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(selectedTabIndex = tab, modifier = Modifier.weight(1f), containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tasks") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
            }
            ThemeMenu(themeMode, onTheme)
            TextButton(onClick = {
                updateState = UpdateState.Checking
                checkForUpdate(ver) { updateState = it }
            }) { Text("Updates") }
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            DayScreen(data, day, { day = it }, ::put)
        } else {
            HistoryScreen(data, ::put) { d -> day = d; tab = 0 }
        }
        Text(
            if (ver.isEmpty()) "Daily To-Do \u00B7 UB Labs" else "Daily To-Do v$ver \u00B7 UB Labs",
            fontSize = 12.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
        )
    }

    updateState?.let { st ->
        AlertDialog(
            onDismissRequest = { if (st !is UpdateState.Checking) updateState = null },
            title = {
                Text(
                    when (st) {
                        is UpdateState.Checking -> "Checking for updates..."
                        is UpdateState.UpToDate -> "You're up to date"
                        is UpdateState.Available -> "Update available"
                        is UpdateState.Failed -> "Couldn't check"
                    }
                )
            },
            text = {
                Text(
                    when (st) {
                        is UpdateState.Checking -> "Contacting GitHub..."
                        is UpdateState.UpToDate -> "Daily To-Do v$ver is the latest version."
                        is UpdateState.Available ->
                            "Version ${st.version} is available (you have v$ver). Tap Update now to download it, then open the " +
                                "downloaded file to install. Your tasks are kept." +
                                (if (st.apkUrl == null) "\n\nThe Android file isn't attached to this release yet, so Update now opens the release page." else "")
                        is UpdateState.Failed -> st.message
                    }
                )
            },
            confirmButton = {
                when (st) {
                    is UpdateState.Available -> TextButton(onClick = {
                        openUrl(ctx, st.apkUrl ?: st.pageUrl)
                        updateState = null
                    }) { Text("Update now") }
                    is UpdateState.Checking -> {}
                    else -> TextButton(onClick = { updateState = null }) { Text("OK") }
                }
            },
            dismissButton = {
                if (st is UpdateState.Available) TextButton(onClick = { updateState = null }) { Text("Later") }
            }
        )
    }
}

@Composable
fun ColumnScope.DayScreen(
    data: Map<String, List<Task>>,
    day: LocalDate,
    onDay: (LocalDate) -> Unit,
    put: (Map<String, List<Task>>) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var pri by remember { mutableIntStateOf(1) }
    var showPicker by remember { mutableStateOf(false) }
    val key = day.toString()
    val all = data[key] ?: emptyList()
    val tasks = all.filter { it.hidden == 0 }.sortedWith(compareBy({ it.done }, { it.pri }))
    val done = tasks.count { it.done }
    val frac = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val pk = day.minusDays(1).toString()
    val prevOpen = (data[pk] ?: emptyList()).filter { !it.done && it.hidden == 0 }

    fun addTask() {
        val v = text.trim()
        if (v.isEmpty()) return
        put(mapOf(key to all + Task(UUID.randomUUID().toString(), v.take(200), false, pri, System.currentTimeMillis())))
        text = ""
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { onDay(day.minusDays(1)) }) { Text("Prev") }
        OutlinedButton(onClick = { onDay(LocalDate.now()) }) { Text("Today") }
        OutlinedButton(onClick = { onDay(day.plusDays(1)) }) { Text("Next") }
    }
    Text(
        day.format(DateTimeFormatter.ofPattern("EEEE")), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.clickable { showPicker = true }.padding(top = 12.dp)
    )
    Box {
        Text(
            day.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + "  \u25BE",
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f),
            modifier = Modifier.clickable { showPicker = true }
        )
        DropdownMenu(expanded = showPicker, onDismissRequest = { showPicker = false }) {
            CompactCalendar(day) { picked -> onDay(picked); showPicker = false }
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("$done of ${tasks.size} done", fontWeight = FontWeight.SemiBold)
    LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp))
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            text, { text = it }, Modifier.weight(1f), singleLine = true,
            placeholder = { Text("Add a task for this day") }
        )
        Button(onClick = { addTask() }) { Text("Add") }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        priLabels.forEachIndexed { i, l ->
            FilterChip(selected = pri == i, onClick = { pri = i }, label = { Text(l) })
        }
    }
    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tasks.isEmpty()) item { Text("No tasks yet. Add your first task above.", modifier = Modifier.padding(24.dp)) }
        items(tasks, key = { it.id }) { t ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(5.dp).height(48.dp).background(priColors[t.pri]))
                    Checkbox(t.done, { c ->
                        put(mapOf(key to all.map {
                            if (it.id == t.id) it.copy(done = c, doneAt = if (c) System.currentTimeMillis() else 0L) else it
                        }))
                    })
                    Text(
                        t.text, Modifier.weight(1f),
                        textDecoration = if (t.done) TextDecoration.LineThrough else null,
                        color = if (t.done) MaterialTheme.colorScheme.onSurface.copy(alpha = .5f)
                        else MaterialTheme.colorScheme.onSurface
                    )
                    // Deleting keeps the task in History (marked Deleted)
                    TextButton(onClick = {
                        put(mapOf(key to all.map { if (it.id == t.id) it.copy(hidden = 1) else it }))
                    }) { Text("Delete") }
                }
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (prevOpen.isNotEmpty()) TextButton(onClick = {
            put(mapOf(
                key to all + prevOpen,
                pk to (data[pk] ?: emptyList()).filter { p -> prevOpen.none { it.id == p.id } }
            ))
        }) { Text("Move ${prevOpen.size} unfinished from yesterday") }
        if (done > 0) TextButton(onClick = {
            // Clearing keeps completed tasks in History (marked Cleared)
            put(mapOf(key to all.map { if (it.done && it.hidden == 0) it.copy(hidden = 2) else it }))
        }) { Text("Clear completed") }
    }
}

@Composable
fun ColumnScope.HistoryScreen(
    data: Map<String, List<Task>>,
    put: (Map<String, List<Task>>) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableIntStateOf(0) } // 0 all, 1 done, 2 open, 3 removed
    var pending by remember { mutableStateOf<Pair<String, Task>?>(null) }

    fun status(t: Task) = if (t.hidden != 0) 3 else if (t.done) 1 else 2

    val rows = data.flatMap { (d, list) -> list.map { d to it } }
    val stats = listOf(
        "Total" to rows.size,
        "Done" to rows.count { status(it.second) == 1 },
        "Open" to rows.count { status(it.second) == 2 },
        "Removed" to rows.count { status(it.second) == 3 }
    )
    val shown = rows
        .filter { (filter == 0 || status(it.second) == filter) && (query.isBlank() || it.second.text.contains(query.trim(), ignoreCase = true)) }
        .sortedByDescending { it.first }
    val grouped = shown.groupBy { it.first }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        stats.forEach { (label, n) ->
            Card(Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$n", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                    Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .65f))
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        query, { query = it }, Modifier.fillMaxWidth(), singleLine = true,
        placeholder = { Text("Search all tasks") }
    )
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("All", "Done", "Open", "Removed").forEachIndexed { i, l ->
            FilterChip(selected = filter == i, onClick = { filter = i }, label = { Text(l) })
        }
    }

    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (grouped.isEmpty()) item {
            Text(
                if (rows.isEmpty()) "No tasks recorded yet." else "No tasks match.",
                modifier = Modifier.fillMaxWidth().padding(24.dp), textAlign = TextAlign.Center
            )
        }
        grouped.forEach { (d, list) ->
            item(key = "h$d") {
                Text(
                    LocalDate.parse(d).format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = .7f),
                    modifier = Modifier.fillMaxWidth().clickable { onOpenDay(LocalDate.parse(d)) }.padding(vertical = 8.dp)
                )
            }
            items(list, key = { "$d:${it.second.id}" }) { (_, t) ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth()) {
                        Box(Modifier.width(5.dp).height(64.dp).background(priColors[t.pri]))
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text(
                                t.text,
                                textDecoration = if (t.done && t.hidden == 0) TextDecoration.LineThrough else null
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    when {
                                        t.hidden == 1 -> "Deleted"
                                        t.hidden == 2 -> "Cleared"
                                        t.done -> "Done"
                                        else -> "Open"
                                    },
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (t.hidden != 0) TextButton(onClick = {
                                    put(mapOf(d to (data[d] ?: emptyList()).map { if (it.id == t.id) it.copy(hidden = 0) else it }))
                                }) { Text("Restore") }
                                TextButton(onClick = { pending = d to t }) { Text("Delete forever") }
                            }
                        }
                    }
                }
            }
        }
    }

    pending?.let { (d, t) ->
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text("Delete forever?") },
            text = { Text("This removes \"${t.text}\" from the record permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    put(mapOf(d to (data[d] ?: emptyList()).filter { it.id != t.id }))
                    pending = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pending = null }) { Text("Cancel") } }
        )
    }
}

// Live preview: open this file in Android Studio and choose Split (top right) to see it update as you type.
@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Daily To-Do (light)")
@Composable
fun TodoPreview() {
    AppTheme(dark = false) { TodoApp(Store(LocalContext.current)) }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Daily To-Do (dark)")
@Composable
fun TodoPreviewDark() {
    AppTheme(dark = true) { TodoApp(Store(LocalContext.current)) }
}
