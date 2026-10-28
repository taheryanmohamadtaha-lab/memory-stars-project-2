@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.memorystars.leitner.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.memorystars.leitner.data.K
import com.memorystars.leitner.domain.Leitner
import com.memorystars.leitner.domain.Rewards

// ---------------- مرور ----------------
@Composable
fun ReviewScreen(vm: AppViewModel, nav: NavHostController, deckId: Long) {
    val ses by vm.session.collectAsState()
    val set by vm.settings.collectAsState()
    val reduce = set?.reduceMotion ?: false
    LaunchedEffect(deckId) { vm.startSession(deckId) }
    LaunchedEffect(ses?.finished) {
        if (ses?.finished == true) nav.navigate("result") { popUpTo("review/{deckId}") { inclusive = true } }
    }
    var exit by remember { mutableStateOf(false) }
    val s = ses
    Screen("مرور: ${s?.deckName ?: ""}", { exit = true }) {
        if (s == null) {
            Text("در حال آماده‌سازی…")
        } else if (s.total == 0) {
            Text("🎉 الان کارتی برای مرور نیست!", fontSize = 22.sp)
            BigButton("بازگشت", { vm.endSession(); nav.popBackStack() })
        } else {
            val card = s.current
            Text("کارت ${fa(minOf(s.done + 1, s.total))} از ${fa(s.total)} • ${fa(s.remaining)} باقی‌مانده", fontSize = 14.sp)
            LinearProgressIndicator(
                progress = { s.done.toFloat() / s.total }, color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
            )
            if (s.feedback.isNotEmpty()) Text(s.feedback, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
            if (card != null) {
                var revealed by remember(s.step) { mutableStateOf(false) }
                val rot by animateFloatAsState(if (revealed) 180f else 0f, tween(if (reduce) 0 else 450), label = "flip")
                val density = LocalDensity.current.density
                Card(
                    Modifier.fillMaxWidth().heightIn(min = 280.dp)
                        .graphicsLayer { rotationY = rot; cameraDistance = 12f * density },
                    shape = RoundedCornerShape(28.dp), elevation = CardDefaults.cardElevation(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    val front = rot <= 90f
                    Column(
                        Modifier.fillMaxWidth().heightIn(min = 280.dp).padding(20.dp)
                            .graphicsLayer { rotationY = if (front) 0f else 180f },
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(if (front) "سؤال" else "پاسخ", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        val txt = if (front) card.frontText else card.backText
                        val img = if (front) card.frontImage else card.backImage
                        CardImage(vm, img, Modifier.fillMaxWidth().heightIn(max = 240.dp))
                        if (txt.isNotBlank()) Text(txt, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
                    }
                }
                if (!revealed) {
                    BigButton("نمایش پاسخ", { revealed = true })
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button({ vm.answer(true) }, Modifier.weight(1f).heightIn(min = 72.dp), shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), contentColor = Color.White)) {
                            Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp))
                            Text("بلدم! ✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                        Button({ vm.answer(false) }, Modifier.weight(1f).heightIn(min = 72.dp), shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), contentColor = Color.White)) {
                            Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp))
                            Text("هنوز یاد نگرفتم! ✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
    if (exit) Confirm("مرور را متوقف کنیم؟ پاسخ‌هایی که تا اینجا داده‌ای ذخیره شده‌اند.",
        { exit = false; vm.endSession(); nav.popBackStack() }, { exit = false })
}

@Composable
fun ResultScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.session.collectAsState()
    val ses = s
    val goHome = { vm.endSession(); nav.navigate("home") { popUpTo("home") { inclusive = true } }; Unit }
    Screen("پایان مرور 🎉", null) {
        Text("🎊 ⭐ 🎉", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (ses == null) {
            Text("نتیجه‌ای برای نمایش نیست.")
        } else {
            Text("آفرین! جلسه‌ی مرور تمام شد.", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatBox(fa(ses.total), "کارت مرورشده", Modifier.weight(1f))
                StatBox(fa(ses.correct), "پاسخ درست", Modifier.weight(1f))
                StatBox(fa(ses.practice), "نیازمند تمرین", Modifier.weight(1f))
            }
            StatBox("+${fa(ses.xp)}", "امتیاز این جلسه")
            if (ses.goalReached) Text("🎈 به هدف امروزت رسیدی!", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        BigButton("بازگشت به صفحه اصلی", { goHome() })
    }
}

// ---------------- جایزه‌ها ----------------
@Composable
fun RewardsScreen(vm: AppViewModel, nav: NavHostController) {
    val st by vm.stats.collectAsState()
    val lv = st.level
    Screen("جایزه‌های من", { nav.popBackStack() }) {
        Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(Rewards.levels[lv].emoji, fontSize = 64.sp)
                Text("سطح ${fa(lv + 1)}: ${Rewards.levels[lv].name}", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("⭐ ${fa(st.xp)} امتیاز")
                if (lv < Rewards.levels.lastIndex) {
                    val n = Rewards.levels[lv + 1]
                    val from = Rewards.levels[lv].minXp
                    LinearProgressIndicator(
                        progress = { ((st.xp - from).toFloat() / (n.minXp - from)).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(10.dp).clip(RoundedCornerShape(5.dp))
                    )
                    Text("برای رسیدن به «${n.name}» ${fa(n.minXp - st.xp)} امتیاز دیگر لازم است")
                } else Text("به بالاترین سطح رسیدی! 👑")
            }
        }
        Text("سطح‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Rewards.levels.forEachIndexed { i, l ->
            Text("${l.emoji} ${l.name} — از ${fa(l.minXp)} امتیاز ${if (i <= lv) "✓" else "🔒"}")
        }
        Text("نشان‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Rewards.badges.forEach { b ->
            val ok = Rewards.unlocked(b.id, st)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(
                containerColor = if (ok) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (ok) b.emoji else "🔒", fontSize = 32.sp)
                    Column { Text(b.title, fontWeight = FontWeight.Bold); Text(b.desc, fontSize = 13.sp) }
                }
            }
        }
        Text("قوانین امتیاز: پاسخ درست ${fa(Rewards.XP_CORRECT)}، تلاش دوباره ${fa(Rewards.XP_TRY)}، پایان جلسه ${fa(Rewards.XP_SESSION)}، رسیدن به هدف روزانه ${fa(Rewards.XP_GOAL)}.", fontSize = 13.sp)
    }
}

// ---------------- گزارش پیشرفت ----------------
@Composable
fun ProgressScreen(vm: AppViewModel, nav: NavHostController) {
    val st by vm.stats.collectAsState()
    Screen("گزارش پیشرفت", { nav.popBackStack() }) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(fa(st.totalCards), "کل کارت‌ها", Modifier.weight(1f))
            StatBox(fa(st.learned), "یادگرفته‌شده", Modifier.weight(1f))
            StatBox(fa(st.practice), "نیازمند تمرین", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(fa(st.totalReviews), "کل مرورها", Modifier.weight(1f))
            StatBox(fa(st.reviewsToday), "کارت‌های امروز", Modifier.weight(1f))
            StatBox(st.accuracy?.let { "٪${fa(it)}" } ?: "—", "پاسخ درست", Modifier.weight(1f))
        }
        Text("جلسات کامل: ${fa(st.sessions)} • یادگرفته‌شده یعنی کارت در جعبه ۴ یا ۵ باشد. درصد درست بر پایه‌ی اولین پاسخ هر کارت در هر مرور است.", fontSize = 12.sp)
        if (st.accuracy == null) Text("هنوز مروری انجام نشده؛ بعد از اولین مرور آمار اینجا نمایش داده می‌شود.")
        Text("مرورهای ۷ روز اخیر", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        val mx = (st.perDay.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(140.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
            st.perDay.forEach { (d, n) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    Text(fa(n), fontSize = 11.sp)
                    Box(Modifier.fillMaxWidth().height((8 + 90f * n / mx).dp).clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.secondary))
                    Text(fa(d.dayOfMonth), fontSize = 11.sp)
                }
            }
        }
        Text("دسته‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        if (st.deckStats.isEmpty()) Text("هنوز دسته‌ای نداری.")
        st.deckStats.filter { it.total > 0 }.sortedByDescending { it.learned.toFloat() / it.total }.forEach { d ->
            val r = d.learned.toFloat() / d.total
            Column {
                Text("${d.deck.icon} ${d.deck.name} — ${fa(d.learned)} از ${fa(d.total)} ${if (r >= 0.7f) "(قوی 💪)" else if (r < 0.3f) "(نیازمند تمرین 🌱)" else ""}")
                LinearProgressIndicator(progress = { r }, modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)))
            }
        }
    }
}

// ---------------- تم‌ها و تنظیمات ----------------
@Composable
fun ThemesScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.settings.collectAsState()
    val st by vm.stats.collectAsState()
    Screen("انتخاب تم", { nav.popBackStack() }) {
        Themes.forEach { t ->
            val locked = st.level < t.minLevel
            val sel = s?.themeId == t.id
            Card(onClick = { if (!locked) vm.edit { it[K.themeId] = t.id } }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = if (sel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(44.dp).clip(CircleShape).background(t.primary), contentAlignment = Alignment.Center) { Text(t.emoji, fontSize = 22.sp) }
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text(if (locked) "🔒 در سطح ${fa(t.minLevel + 1)} (${Rewards.levels[t.minLevel].name}) باز می‌شود" else t.decor, fontSize = 13.sp)
                    }
                    if (sel) Text("✓", fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.settings.collectAsState()
    val set = s ?: return
    Screen("تنظیمات", { nav.popBackStack() }) {
        Text("حالت نمایش", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        listOf("پیروی از سیستم" to 0, "روشن ☀️" to 1, "تاریک 🌙" to 2).forEach { (label, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(set.themeMode == v, { vm.edit { it[K.themeMode] = v } })
                Text(label, fontSize = 17.sp)
            }
        }
        OutlinedButton({ nav.navigate("themes") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("🎨 انتخاب تم") }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(set.reduceMotion, { v -> vm.edit { it[K.reduceMotion] = v } }); Spacer(Modifier.width(8.dp))
            Text("کاهش انیمیشن‌ها")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(set.bigFont, { v -> vm.edit { it[K.bigFont] = v } }); Spacer(Modifier.width(8.dp))
            Text("فونت بزرگ‌تر")
        }
    }
}

@Composable
fun LeitnerSettingsScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.settings.collectAsState()
    val set = s ?: return
    Screen("تنظیمات مرور لایتنر", { nav.popBackStack() }) {
        Text("فاصله‌ی مرور بعدی هر جعبه (بر حسب روز). تغییر فقط روی مرورهای بعدی اثر می‌گذارد.", fontSize = 14.sp)
        set.intervals.forEachIndexed { i, d ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("جعبه ${fa(i + 1)}", Modifier.weight(1f), fontSize = 17.sp)
                OutlinedButton({ save(vm, set.intervals, i, d - 1) }, enabled = d > 1) { Text("−") }
                Text("${fa(d)} روز", fontWeight = FontWeight.Bold)
                OutlinedButton({ save(vm, set.intervals, i, d + 1) }, enabled = d < 60) { Text("+") }
            }
        }
        OutlinedButton({ vm.edit { it[K.intervals] = Leitner.DEFAULT_INTERVALS.joinToString(",") } }, Modifier.fillMaxWidth()) {
            Text("بازگشت به پیش‌فرض (۱، ۲، ۴، ۷، ۱۴)")
        }
    }
}

private fun save(vm: AppViewModel, cur: List<Int>, i: Int, v: Int) {
    val n = cur.toMutableList(); n[i] = v
    vm.edit { it[K.intervals] = n.joinToString(",") }
}

// ---------------- والدین ----------------
@Composable
fun ParentScreen(vm: AppViewModel, nav: NavHostController) {
    val s by vm.settings.collectAsState()
    val set = s ?: return
    var reset by remember { mutableStateOf(false) }
    var name by remember(set.name) { mutableStateOf(set.name) }
    Screen("بخش والدین", { nav.popBackStack() }) {
        OutlinedTextField(name, { name = it.take(20); vm.edit { p -> p[K.name] = it.take(20) } }, Modifier.fillMaxWidth(), label = { Text("نام کودک") }, singleLine = true)
        Text("هدف روزانه (تعداد کارت)", fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(5, 10, 15, 20).forEach { g -> FilterChip(set.goal == g, { vm.edit { it[K.goal] = g } }, label = { Text(fa(g)) }) }
        }
        OutlinedButton({ nav.navigate("themes") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("🎨 مدیریت تم‌ها") }
        OutlinedButton({ nav.navigate("leitner") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("📅 فاصله‌های مرور لایتنر") }
        OutlinedButton({ nav.navigate("progress") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("📈 گزارش پیشرفت") }
        OutlinedButton({ nav.navigate("decks") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("📚 مدیریت دسته‌ها و کارت‌ها") }
        OutlinedButton({ nav.navigate("backup") }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("💾 پشتیبان‌گیری و بازیابی") }
        BigButton("بازنشانی آمار", { reset = true }, container = MaterialTheme.colorScheme.error, content = MaterialTheme.colorScheme.onError)
    }
    if (reset) Confirm("امتیاز، سطح، تاریخچه و جعبه‌ی همه‌ی کارت‌ها صفر می‌شود (خود کارت‌ها می‌مانند). این کار برگشت‌پذیر نیست.",
        { vm.resetStats(); reset = false }, { reset = false })
}

@Composable
fun BackupScreen(vm: AppViewModel, nav: NavHostController) {
    var msg by remember { mutableStateOf("") }
    var pending by remember { mutableStateOf<android.net.Uri?>(null) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { u ->
        if (u != null) vm.export(u) { msg = it }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { u -> if (u != null) pending = u }
    Screen("پشتیبان‌گیری و بازیابی", { nav.popBackStack() }) {
        Text("پشتیبان شامل دسته‌ها، کارت‌ها، تصاویر، تاریخچه، امتیاز و هدف روزانه است (فایل ZIP).", fontSize = 15.sp)
        BigButton("ساخت فایل پشتیبان", { exp.launch("memory-stars-backup.zip") })
        BigButton("بازیابی از فایل پشتیبان", { imp.launch(arrayOf("application/zip", "application/octet-stream")) },
            container = MaterialTheme.colorScheme.secondary, content = MaterialTheme.colorScheme.onSecondary)
        if (msg.isNotEmpty()) Text(msg, fontWeight = FontWeight.Bold)
    }
    pending?.let { u ->
        Confirm("با بازیابی، همه‌ی داده‌های فعلی با محتوای فایل جایگزین می‌شوند.",
            { pending = null; vm.restore(u) { msg = it } }, { pending = null })
    }
}
