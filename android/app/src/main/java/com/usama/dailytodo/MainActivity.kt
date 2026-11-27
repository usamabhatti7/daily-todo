package com.usama.dailytodo

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

data class Task(val id: String, val text: String, val done: Boolean, val pri: Int) // 0 high, 1 medium, 2 low

class Store(ctx: Context) {
    private val sp = ctx.getSharedPreferences("todo", Context.MODE_PRIVATE)
    fun load(): Map<String, List<Task>> {
        val raw = sp.getString("data", null) ?: return emptyMap()
        val o = JSONObject(raw)
        val m = mutableMapOf<String, List<Task>>()
        o.keys().forEach { k ->
            val a = o.getJSONArray(k)
            m[k] = (0 until a.length()).map { i ->
                val t = a.getJSONObject(i)
                Task(t.getString("id"), t.getString("text"), t.getBoolean("done"), t.getInt("pri"))
            }
        }
        return m
    }
    fun save(m: Map<String, List<Task>>) {
        val o = JSONObject()
        m.forEach { (k, v) ->
            o.put(k, JSONArray(v.map { JSONObject().put("id", it.id).put("text", it.text).put("done", it.done).put("pri", it.pri) }))
        }
        sp.edit().putString("data", o.toString()).apply()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = Store(this)
        setContent {
            val scheme = if (isSystemInDarkTheme())
                darkColorScheme(primary = Color(0xFF3CC4AE), onPrimary = Color(0xFF06211C), background = Color(0xFF0F1720), surface = Color(0xFF17222E))
            else lightColorScheme(primary = Color(0xFF0E7466), background = Color(0xFFE9EEF3), surface = Color.White)
            MaterialTheme(colorScheme = scheme) { Surface(color = MaterialTheme.colorScheme.background) { TodoApp(store) } }
        }
    }
}

private val priLabels = listOf("High", "Medium", "Low")
private val priColors = listOf(Color(0xFFC2410C), Color(0xFFA16207), Color(0xFF2563A8))

@Composable
fun TodoApp(store: Store) {
    var data by remember { mutableStateOf(store.load()) }
    var day by remember { mutableStateOf(LocalDate.now()) }
    var text by remember { mutableStateOf("") }
    var pri by remember { mutableIntStateOf(1) }
    val key = day.toString()
    val tasks = (data[key] ?: emptyList()).sortedWith(compareBy({ it.done }, { it.pri }))
    val done = tasks.count { it.done }
    val frac = if (tasks.isEmpty()) 0f else done.toFloat() / tasks.size
    val prevOpen = (data[day.minusDays(1).toString()] ?: emptyList()).filter { !it.done }

    fun put(changes: Map<String, List<Task>>) { data = data + changes; store.save(data) }
    fun addTask() {
        val v = text.trim(); if (v.isEmpty()) return
        put(mapOf(key to (data[key] ?: emptyList()) + Task(UUID.randomUUID().toString(), v, false, pri)))
        text = ""
    }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { day = day.minusDays(1) }) { Text("Prev") }
            OutlinedButton(onClick = { day = LocalDate.now() }) { Text("Today") }
            OutlinedButton(onClick = { day = day.plusDays(1) }) { Text("Next") }
        }
        Text(day.format(DateTimeFormatter.ofPattern("EEEE")), fontSize = 40.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
        Text(day.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")), color = MaterialTheme.colorScheme.onBackground.copy(alpha = .65f))
        Spacer(Modifier.height(12.dp))
        Text("$done of ${tasks.size} done", fontWeight = FontWeight.SemiBold)
        LinearProgressIndicator(progress = { frac }, modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 4.dp))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(text, { text = it }, Modifier.weight(1f), singleLine = true, placeholder = { Text("Add a task for this day") })
            Button(onClick = { addTask() }) { Text("Add") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
            priLabels.forEachIndexed { i, l -> FilterChip(selected = pri == i, onClick = { pri = i }, label = { Text(l) }) }
        }
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (tasks.isEmpty()) item { Text("No tasks yet. Add your first task above.", modifier = Modifier.padding(24.dp)) }
            items(tasks, key = { it.id }) { t ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Row(Modifier.fillMaxWidth().padding(end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(5.dp).height(48.dp).padding(start = 0.dp).let { it }) { Surface(color = priColors[t.pri], modifier = Modifier.fillMaxSize()) {} }
                        Checkbox(t.done, { c -> put(mapOf(key to (data[key] ?: emptyList()).map { if (it.id == t.id) it.copy(done = c) else it })) })
                        Text(t.text, Modifier.weight(1f), textDecoration = if (t.done) TextDecoration.LineThrough else null,
                            color = if (t.done) MaterialTheme.colorScheme.onSurface.copy(alpha = .5f) else MaterialTheme.colorScheme.onSurface)
                        TextButton(onClick = { put(mapOf(key to (data[key] ?: emptyList()).filter { it.id != t.id })) }) { Text("Delete") }
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (prevOpen.isNotEmpty()) TextButton(onClick = {
                val pk = day.minusDays(1).toString()
                put(mapOf(
                    key to (data[key] ?: emptyList()) + prevOpen.map { it.copy(id = UUID.randomUUID().toString()) },
                    pk to (data[pk] ?: emptyList()).filter { it.done }))
            }) { Text("Move ${prevOpen.size} unfinished from yesterday") }
            if (done > 0) TextButton(onClick = { put(mapOf(key to (data[key] ?: emptyList()).filter { !it.done })) }) { Text("Clear completed") }
        }
    }
}

// Live preview: open this file in Android Studio and choose Split (top right) to see it update as you type.
@Preview(showBackground = true, widthDp = 360, heightDp = 740, name = "Daily To-Do (light)")
@Composable
fun TodoPreview() {
    MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF0E7466), background = Color(0xFFE9EEF3), surface = Color.White)) {
        Surface(color = MaterialTheme.colorScheme.background) { TodoApp(Store(LocalContext.current)) }
    }
}
