package com.usama.dailytodo

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayPickerDialog(day: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) { DatePicker(state = state) }
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

    fun put(changes: Map<String, List<Task>>) { data = data + changes; store.save(data) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TabRow(selectedTabIndex = tab, modifier = Modifier.weight(1f), containerColor = Color.Transparent) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Tasks") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("History") })
            }
            ThemeMenu(themeMode, onTheme)
        }
        Spacer(Modifier.height(8.dp))
        if (tab == 0) {
            DayScreen(data, day, { day = it }, ::put)
        } else {
            HistoryScreen(data, ::put) { d -> day = d; tab = 0 }
        }
        val ver = appVersion()
        Text(
            if (ver.isEmpty()) "Daily To-Do \u00B7 UB Labs" else "Daily To-Do v$ver \u00B7 UB Labs",
            fontSize = 12.sp, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = .55f),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
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
    Text(
        day.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")) + "  \u25BE",
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f),
        modifier = Modifier.clickable { showPicker = true }
    )
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
    if (showPicker) DayPickerDialog(day, onPick = onDay, onDismiss = { showPicker = false })
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
