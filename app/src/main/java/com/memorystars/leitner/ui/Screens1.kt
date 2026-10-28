@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.memorystars.leitner.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.datastore.preferences.core.edit
import androidx.navigation.NavHostController
import com.memorystars.leitner.data.CardEntity
import com.memorystars.leitner.data.DeckEntity
import com.memorystars.leitner.data.K
import com.memorystars.leitner.domain.Rewards
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

fun fa(n: Int): String = n.toString().map { if (it in '0'..'9') ('۰' + (it - '0')) else it }.joinToString("")

@Composable
fun Screen(
    title: String, onBack: (() -> Unit)?,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold) },
            navigationIcon = {
                if (onBack != null) IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
        )
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
            Column(
                Modifier.widthIn(max = 640.dp).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), content = content
            )
        }
    }
}

@Composable
fun BigButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    container: Color = MaterialTheme.colorScheme.primary, content: Color = MaterialTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick, modifier = modifier.fillMaxWidth().heightIn(min = 60.dp), enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
    ) { Text(text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
}

@Composable
fun Confirm(text: String, onYes: () -> Unit, onNo: () -> Unit) {
    AlertDialog(
        onDismissRequest = onNo, title = { Text("مطمئنی؟") }, text = { Text(text) },
        confirmButton = { TextButton(onClick = onYes) { Text("بله") } },
        dismissButton = { TextButton(onClick = onNo) { Text("نه") } }
    )
}

@Composable
fun StatBox(value: String, label: String, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Column(Modifier.fillMaxWidth().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun CardImage(vm: AppViewModel, name: String?, modifier: Modifier = Modifier) {
    if (name == null) return
    val bmp by produceState<Bitmap?>(null, name) {
        value = withContext(Dispatchers.IO) { vm.images.bitmap(name, 1024) }
    }
    bmp?.let { Image(it.asImageBitmap(), null, modifier, contentScale = ContentScale.Fit) }
}

// ---------------- خوشامدگویی ----------------
@Composable
fun WelcomeScreen(vm: AppViewModel, nav: NavHostController) {
    var step by remember { mutableIntStateOf(0) }
    var name by remember { mutableStateOf("") }
    val tips = listOf(
        "🃏" to "اول یک دسته می‌سازی، مثلاً «جدول ضرب» یا «حیوانات».",
        "📷" to "بعد فلش‌کارت می‌سازی: با نوشتن، عکس گرفتن یا انتخاب عکس از گالری.",
        "✅" to "هر روز کارت‌ها را مرور کن. «بلدم» یعنی کارت یک جعبه جلو می‌رود و «هنوز یاد نگرفتم» یعنی دوباره تمرین می‌کنیم."
    )
    Screen("به ستاره‌های حافظه خوش آمدی", null) {
        Text("⭐", fontSize = 80.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        if (step == 0) {
            Text("سلام! من ستاره‌ی کوچولو هستم. اسمت چیه؟", fontSize = 20.sp)
            OutlinedTextField(name, { name = it.take(20) }, Modifier.fillMaxWidth(), label = { Text("نام (اختیاری)") }, singleLine = true)
        } else {
            val t = tips[step - 1]
            Text(t.first, fontSize = 56.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            Text(t.second, fontSize = 20.sp, lineHeight = 32.sp)
        }
        BigButton(if (step < tips.size) "بعدی" else "شروع کنیم!", {
            if (step < tips.size) step++ else {
                vm.edit { it[K.name] = name.trim(); it[K.onboarded] = true }
                nav.navigate("home") { popUpTo("welcome") { inclusive = true } }
            }
        })
    }
}

// ---------------- صفحه اصلی ----------------
@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController, theme: AppTheme) {
    val s by vm.settings.collectAsState()
    val st by vm.stats.collectAsState()
    val set = s ?: return
    var gate by remember { mutableStateOf(false) }
    var pickDeck by remember { mutableStateOf(false) }
    val level = Rewards.levels[st.level]
    val bounce by rememberInfiniteTransition(label = "b").animateFloat(
        0f, if (set.reduceMotion) 0f else -12f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "bounce"
    )
    Screen("ستاره‌های حافظه", null, actions = {
        IconButton({ nav.navigate("settings") }) { Icon(Icons.Filled.Settings, "تنظیمات") }
    }) {
        Text(theme.decor, fontSize = 20.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(84.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center) {
                Text(level.emoji, fontSize = 44.sp, modifier = Modifier.offset(y = bounce.dp))
            }
            Column {
                Text(if (set.name.isBlank()) "سلام دوست من!" else "سلام ${set.name}!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text("سطح ${fa(st.level + 1)}: ${level.name}", fontSize = 15.sp)
                Text("⭐ ${fa(st.xp)} امتیاز  •  🔥 ${fa(st.streak)} روز پیاپی", fontSize = 15.sp)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatBox(fa(st.due), "آماده مرور امروز", Modifier.weight(1f))
            StatBox(fa(st.learned), "یادگرفته‌شده", Modifier.weight(1f))
            StatBox(fa(st.totalCards), "کل کارت‌ها", Modifier.weight(1f))
        }
        Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.fillMaxWidth().padding(14.dp)) {
                Text("هدف امروز: ${fa(minOf(st.reviewsToday, set.goal))} از ${fa(set.goal)} کارت", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (st.reviewsToday.toFloat() / set.goal).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
        BigButton(if (st.due > 0) "شروع مرور (${fa(st.due)} کارت)" else "امروز کارتی برای مرور نیست",
            { nav.navigate("review/0") }, enabled = st.due > 0)
        BigButton("ساخت فلش‌کارت", { if (vm.decks.value.isEmpty()) nav.navigate("decks") else pickDeck = true },
            container = MaterialTheme.colorScheme.secondary, content = MaterialTheme.colorScheme.onSecondary)
        BigButton("🎮 مرور و تمرین", { nav.navigate("practice/0") }, container = MaterialTheme.colorScheme.tertiary, content = MaterialTheme.colorScheme.onTertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ nav.navigate("decks") }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("📚 دسته‌های من") }
            OutlinedButton({ nav.navigate("rewards") }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("🏅 جایزه‌ها") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton({ nav.navigate("progress") }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("📈 گزارش پیشرفت") }
            OutlinedButton({ gate = true }, Modifier.weight(1f).heightIn(min = 52.dp)) { Text("👨‍👩‍👧 والدین") }
        }
    }
    if (pickDeck) {
        val decks by vm.decks.collectAsState()
        AlertDialog(onDismissRequest = { pickDeck = false }, title = { Text("کارت در کدام دسته ساخته شود؟") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    decks.forEach { d ->
                        TextButton({ pickDeck = false; nav.navigate("editor/${d.id}/0") }) { Text("${d.icon} ${d.name}", fontSize = 18.sp) }
                    }
                }
            }, confirmButton = { TextButton({ pickDeck = false }) { Text("بستن") } })
    }
    if (gate) ParentGate({ gate = false }) { gate = false; nav.navigate("parent") }
}

@Composable
fun ParentGate(onDismiss: () -> Unit, onPass: () -> Unit) {
    val a = remember { Random.nextInt(3, 10) }
    val b = remember { Random.nextInt(3, 10) }
    var ans by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("بخش والدین") },
        text = {
            Column {
                Text("برای ورود، این سؤال را جواب بده:\n${fa(a)} × ${fa(b)} = ؟")
                OutlinedTextField(ans, { ans = it.filter { c -> c.isDigit() }.take(3); wrong = false },
                    singleLine = true, isError = wrong, label = { Text("پاسخ (با اعداد انگلیسی)") })
                if (wrong) Text("پاسخ درست نبود", color = MaterialTheme.colorScheme.error)
                Text("این قفل فقط برای جلوگیری از ورود تصادفی است و حفاظت امنیتی نیست.", fontSize = 12.sp)
            }
        },
        confirmButton = { TextButton({ if (ans.toIntOrNull() == a * b) onPass() else wrong = true }) { Text("ورود") } },
        dismissButton = { TextButton(onDismiss) { Text("انصراف") } })
}

// ---------------- دسته‌ها ----------------
private val deckIcons = listOf("📚", "🔢", "🔤", "🧪", "🐾", "🏰", "📝", "💡", "🌍", "🎨")
private val deckColors = listOf(0xFF7C4DFF, 0xFFE91E63, 0xFF009688, 0xFFFF9800, 0xFF2196F3, 0xFF4CAF50)

@Composable
fun DecksScreen(vm: AppViewModel, nav: NavHostController) {
    val decks by vm.decks.collectAsState()
    val cards by vm.cards.collectAsState()
    var editing by remember { mutableStateOf<DeckEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DeckEntity?>(null) }
    val now = System.currentTimeMillis()
    Screen("دسته‌های من", { nav.popBackStack() }) {
        BigButton("➕ دسته جدید", { creating = true })
        if (decks.isEmpty()) Text("هنوز دسته‌ای نداری. اولین دسته‌ات را بساز!", fontSize = 18.sp)
        decks.forEach { d ->
            val cs = cards.filter { it.deckId == d.id }
            Card(Modifier.fillMaxWidth().clickable { nav.navigate("deck/${d.id}") }, shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(Color(d.color).copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        Text(d.icon, fontSize = 30.sp)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        Text("${fa(cs.size)} کارت • ${fa(cs.count { it.dueAt <= now })} آماده مرور", fontSize = 13.sp)
                    }
                    IconButton({ editing = d }) { Icon(Icons.Filled.Edit, "ویرایش دسته") }
                    IconButton({ deleting = d }) { Icon(Icons.Filled.Delete, "حذف دسته") }
                }
            }
        }
    }
    if (creating || editing != null) {
        val e = editing
        var name by remember(e) { mutableStateOf(e?.name ?: "") }
        var icon by remember(e) { mutableStateOf(e?.icon ?: deckIcons[0]) }
        var color by remember(e) { mutableLongStateOf(e?.color ?: deckColors[0]) }
        val close = { creating = false; editing = null }
        AlertDialog(onDismissRequest = close, title = { Text(if (e == null) "دسته جدید" else "ویرایش دسته") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it.take(30) }, singleLine = true, label = { Text("نام دسته") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        deckIcons.forEach { i ->
                            FilterChip(icon == i, { icon = i }, label = { Text(i, fontSize = 22.sp) })
                        }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        deckColors.forEach { c ->
                            Box(Modifier.size(if (color == c) 40.dp else 32.dp).clip(CircleShape).background(Color(c)).clickable { color = c })
                        }
                    }
                }
            },
            confirmButton = {
                TextButton({ if (name.isNotBlank()) { vm.saveDeck(e, name.trim(), icon, color); close() } }) { Text("ذخیره") }
            }, dismissButton = { TextButton(close) { Text("انصراف") } })
    }
    deleting?.let { d ->
        Confirm("دسته «${d.name}» و همه‌ی کارت‌ها و عکس‌هایش حذف می‌شود.", { vm.deleteDeck(d); deleting = null }, { deleting = null })
    }
}

@Composable
fun DeckDetailScreen(vm: AppViewModel, nav: NavHostController, deckId: Long) {
    val decks by vm.decks.collectAsState()
    val cards by vm.cards.collectAsState()
    val deck = decks.firstOrNull { it.id == deckId }
    var q by remember { mutableStateOf("") }
    var sort by remember { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<CardEntity?>(null) }
    val all = cards.filter { it.deckId == deckId }
    val list = all.filter { q.isBlank() || it.frontText.contains(q, true) || it.backText.contains(q, true) }
        .let { l -> if (sort == 0) l.sortedByDescending { it.createdAt } else l.sortedByDescending { it.lastReviewedAt ?: 0L } }
    val due = all.count { it.dueAt <= System.currentTimeMillis() }
    Scaffold(topBar = {
        TopAppBar(title = { Text("${deck?.icon ?: ""} ${deck?.name ?: ""}", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } })
    }) { pad ->
        Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.TopCenter) {
            LazyColumn(Modifier.widthIn(max = 640.dp).fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        BigButton("کارت جدید", { nav.navigate("editor/$deckId/0") })
                        BigButton(if (due > 0) "مرور این دسته (${fa(due)})" else "کارت موعددار نیست",
                            { nav.navigate("review/$deckId") }, enabled = due > 0,
                            container = MaterialTheme.colorScheme.secondary, content = MaterialTheme.colorScheme.onSecondary)
                        BigButton("🎮 تمرین این دسته", { nav.navigate("practice/$deckId") }, enabled = all.isNotEmpty(), container = MaterialTheme.colorScheme.tertiary, content = MaterialTheme.colorScheme.onTertiary)
                        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("جست‌وجو در کارت‌ها") })
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(sort == 0, { sort = 0 }, label = { Text("تاریخ ساخت") })
                            FilterChip(sort == 1, { sort = 1 }, label = { Text("آخرین مرور") })
                        }
                        if (all.isEmpty()) Text("این دسته هنوز کارتی ندارد.", fontSize = 17.sp)
                        else if (list.isEmpty()) Text("کارتی پیدا نشد.", fontSize = 17.sp)
                    }
                }
                items(list, key = { it.id }) { c ->
                    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text((if (c.frontImage != null) "🖼️ " else "") + c.frontText.ifBlank { "(تصویر)" }, fontWeight = FontWeight.Bold, maxLines = 2)
                                Text((if (c.backImage != null) "🖼️ " else "") + c.backText.ifBlank { "(تصویر)" }, fontSize = 14.sp, maxLines = 2)
                                Text("جعبه ${fa(c.box)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                            }
                            TextButton({ vm.duplicateCard(c) }) { Text("کپی") }
                            IconButton({ nav.navigate("editor/$deckId/${c.id}") }) { Icon(Icons.Filled.Edit, "ویرایش") }
                            IconButton({ deleting = c }) { Icon(Icons.Filled.Delete, "حذف") }
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
    deleting?.let { c -> Confirm("این کارت و تاریخچه‌ی مرور آن حذف می‌شود.", { vm.deleteCard(c); deleting = null }, { deleting = null }) }
}

// ---------------- ساخت و ویرایش کارت ----------------
@Composable
fun EditorScreen(vm: AppViewModel, nav: NavHostController, deckId0: Long, cardId: Long) {
    val decks by vm.decks.collectAsState()
    val ctx = LocalContext.current
    var deckId by remember { mutableLongStateOf(deckId0) }
    var front by remember { mutableStateOf("") }
    var back by remember { mutableStateOf("") }
    var fImg by remember { mutableStateOf<String?>(null) }
    var bImg by remember { mutableStateOf<String?>(null) }
    var orig by remember { mutableStateOf<CardEntity?>(null) }
    val created = remember { mutableStateListOf<String>() }
    var target by remember { mutableIntStateOf(0) }
    var camFile by remember { mutableStateOf<File?>(null) }

    LaunchedEffect(cardId) {
        if (cardId != 0L) vm.getCard(cardId)?.let {
            orig = it; deckId = it.deckId; front = it.frontText; back = it.backText; fImg = it.frontImage; bImg = it.backImage
        }
    }
    fun setImg(n: String) { created.add(n); if (target == 0) fImg = n else bImg = n }
    val cam = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = camFile
        if (ok && f != null) setImg(f.name) else f?.delete()
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val n = vm.images.importUri(uri)
            if (n != null) setImg(n) else Toast.makeText(ctx, "تصویر خوانده نشد", Toast.LENGTH_SHORT).show()
        }
    }
    fun launchCam(side: Int) {
        target = side
        val f = vm.images.newFile(); camFile = f
        try { cam.launch(FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)) }
        catch (e: Exception) { f.delete(); Toast.makeText(ctx, "برنامه‌ی دوربین پیدا نشد", Toast.LENGTH_LONG).show() }
    }
    fun launchPick(side: Int) {
        target = side
        pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
    val cancel = { created.forEach { vm.images.delete(it) }; nav.popBackStack(); Unit }
    BackHandler { cancel() }

    val canSave = deckId != 0L && (front.isNotBlank() || fImg != null) && (back.isNotBlank() || bImg != null)
    Screen(if (cardId == 0L) "کارت جدید" else "ویرایش کارت", cancel) {
        if (decks.isEmpty()) {
            Text("اول باید یک دسته بسازی.", fontSize = 18.sp)
            BigButton("ساخت دسته", { nav.navigate("decks") })
        } else {
            Text("دسته:", fontWeight = FontWeight.Bold)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                decks.forEach { d -> FilterChip(deckId == d.id, { deckId = d.id }, label = { Text("${d.icon} ${d.name}") }) }
            }
            SideEditor("روی سؤال (جلو)", front, { front = it }, fImg, vm, { launchCam(0) }, { launchPick(0) }, { fImg = null })
            SideEditor("روی پاسخ (پشت)", back, { back = it }, bImg, vm, { launchCam(1) }, { launchPick(1) }, { bImg = null })
            if (!canSave) Text("هر طرف کارت باید متن یا تصویر داشته باشد و یک دسته انتخاب شود.", fontSize = 13.sp)
            BigButton("ذخیره کارت", {
                vm.saveCard(orig, deckId, front.trim(), back.trim(), fImg, bImg) {
                    created.filter { it != fImg && it != bImg }.forEach { vm.images.delete(it) }
                    nav.popBackStack()
                }
            }, enabled = canSave)
        }
    }
}

@Composable
private fun SideEditor(
    title: String, text: String, onText: (String) -> Unit, img: String?, vm: AppViewModel,
    onCam: () -> Unit, onPick: () -> Unit, onRemove: () -> Unit
) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            OutlinedTextField(text, { onText(it.take(300)) }, Modifier.fillMaxWidth(), label = { Text("متن") }, minLines = 2)
            CardImage(vm, img, Modifier.fillMaxWidth().heightIn(max = 220.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onCam, Modifier.weight(1f)) { Text(if (img == null) "📷 دوربین" else "📷 دوباره") }
                OutlinedButton(onPick, Modifier.weight(1f)) { Text("🖼️ گالری") }
                if (img != null) OutlinedButton(onRemove, Modifier.weight(1f)) { Text("حذف") }
            }
        }
    }
}
