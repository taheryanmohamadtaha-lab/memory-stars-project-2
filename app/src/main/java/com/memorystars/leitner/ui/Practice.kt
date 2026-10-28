@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.memorystars.leitner.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
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
import com.memorystars.leitner.data.CardEntity
import com.memorystars.leitner.domain.Practice
import kotlinx.coroutines.delay

private val GREEN = Color(0xFF2E7D32)
private val RED = Color(0xFFC62828)

private fun qImg(c: CardEntity, rev: Boolean) = if (rev) c.backImage else c.frontImage
private fun aImg(c: CardEntity, rev: Boolean) = if (rev) c.frontImage else c.backImage

private val modeList = listOf(
    Triple(Practice.CLASSIC, "🃏 کلاسیک", "سؤال را ببین، جواب را بخوان و «بلدم» یا «هنوز یاد نگرفتم» را بزن"),
    Triple(Practice.CHOICE, "🔘 چهارگزینه‌ای", "پاسخ درست را از بین چند گزینه پیدا کن"),
    Triple(Practice.TYPING, "⌨️ تایپ پاسخ", "جواب را بنویس؛ برنامه خودش بررسی می‌کند"),
    Triple(Practice.MATCH, "🧩 جفت‌کردن", "هر سؤال را به پاسخ خودش وصل کن")
)

@Composable
fun PracticeSetupScreen(vm: AppViewModel, nav: NavHostController, deck0: Long) {
    val decks by vm.decks.collectAsState()
    val cards by vm.cards.collectAsState()
    var deckId by remember { mutableLongStateOf(deck0) }
    var mode by remember { mutableIntStateOf(Practice.CLASSIC) }
    var filter by remember { mutableIntStateOf(Practice.F_ALL) }
    var order by remember { mutableIntStateOf(Practice.ORDER_SEQ) }
    var rev by remember { mutableStateOf(false) }
    val usable = Practice.usable(Practice.pool(cards, deckId, filter), mode, rev)
    val need = if (mode == Practice.CHOICE || mode == Practice.MATCH) 2 else 1

    Screen("مرور و تمرین", { nav.popBackStack() }) {
        Text("تمرین آزاد است: هر وقت و هر چقدر خواستی. روی جعبه‌های لایتنر و امتیاز اثری ندارد.", fontSize = 14.sp)
        Text("نوع تمرین", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        modeList.forEach { (id, title, desc) ->
            Card(
                onClick = { mode = id }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(title + if (mode == id) "  ✓" else "", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(desc, fontSize = 13.sp)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(rev, { rev = it }); Spacer(Modifier.width(8.dp))
            Text("حالت معکوس (پشت کارت سؤال، روی کارت جواب)")
        }
        Text("کدام دسته؟", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(deckId == 0L, { deckId = 0L }, label = { Text("همه") })
            decks.forEach { d -> FilterChip(deckId == d.id, { deckId = d.id }, label = { Text("${d.icon} ${d.name}") }) }
        }
        Text("کدام کارت‌ها؟", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Practice.F_ALL to "همه کارت‌ها", Practice.F_WEAK to "ضعیف‌ها (جعبه ۱ و ۲)",
                Practice.F_LEARNED to "یادگرفته‌ها (جعبه ۴ و ۵)", Practice.F_HARD to "اشتباه‌شده‌ها"
            ).forEach { (v, l) -> FilterChip(filter == v, { filter = v }, label = { Text(l) }) }
        }
        Text("ترتیب کارت‌ها", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                Practice.ORDER_SEQ to "ترتیبی (به ترتیب ساخت)", Practice.ORDER_RANDOM to "اتفاقی 🎲",
                Practice.ORDER_HARD to "سخت‌ترین‌ها اول"
            ).forEach { (v, l) -> FilterChip(order == v, { order = v }, label = { Text(l) }) }
        }
        Text(
            if (usable.size >= need) "${fa(usable.size)} کارت برای این تمرین آماده است."
            else "برای این حالت حداقل ${fa(need)} کارت مناسب لازم است (الان ${fa(usable.size)} کارت).",
            fontWeight = FontWeight.Bold
        )
        BigButton("شروع تمرین 🚀", {
            nav.navigate("practiceRun/$mode/$deckId/$filter/$order/${if (rev) 1 else 0}")
        }, enabled = usable.size >= need)
    }
}

@Composable
fun PracticeRunScreen(
    vm: AppViewModel, nav: NavHostController,
    mode: Int, deckId: Long, filter: Int, order: Int, rev: Boolean
) {
    val set by vm.settings.collectAsState()
    val reduce = set?.reduceMotion ?: false
    val all = remember { vm.cards.value }
    var round by remember { mutableIntStateOf(0) }
    val ordered = remember(round) {
        Practice.order(Practice.usable(Practice.pool(all, deckId, filter), mode, rev), order)
    }
    val queue = remember(round) { mutableStateListOf<CardEntity>().apply { addAll(ordered) } }
    val wrongIds = remember(round) { mutableStateListOf<Long>() }
    var step by remember(round) { mutableIntStateOf(0) }
    var matchRes by remember(round) { mutableStateOf<Pair<Int, Int>?>(null) }
    var exit by remember { mutableStateOf(false) }
    val total = ordered.size
    val finished = if (mode == Practice.MATCH) matchRes != null else (total > 0 && queue.isEmpty())

    fun answer(ok: Boolean) {
        if (queue.isEmpty()) return
        val c = queue.removeAt(0)
        if (!ok) {
            if (c.id !in wrongIds) wrongIds.add(c.id)
            queue.add(minOf(3, queue.size), c) // کارت اشتباه تا درست شدن دوباره می‌آید
        }
        step++
    }

    val title = modeList.first { it.first == mode }.second
    Screen(title, { if (finished || total == 0) nav.popBackStack() else exit = true }) {
        when {
            total == 0 -> {
                Text("کارتی برای این تمرین پیدا نشد.", fontSize = 18.sp)
                BigButton("بازگشت", { nav.popBackStack() })
            }
            finished -> {
                val good = if (mode == Practice.MATCH) total else total - wrongIds.size
                val bad = if (mode == Practice.MATCH) (matchRes?.second ?: 0) else wrongIds.size
                Text("🎉 ⭐ 🎊", fontSize = 48.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Text("آفرین! تمرین تمام شد.", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(fa(total), "کارت", Modifier.weight(1f))
                    StatBox(fa(good), if (mode == Practice.MATCH) "جفت درست" else "درست در اولین تلاش", Modifier.weight(1f))
                    StatBox(fa(bad), if (mode == Practice.MATCH) "اشتباه" else "نیازمند تمرین", Modifier.weight(1f))
                }
                BigButton("تمرین دوباره 🔁", { round++ })
                OutlinedButton({ nav.popBackStack() }, Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("بازگشت") }
            }
            else -> {
                if (mode != Practice.MATCH) {
                    val remainingDistinct = queue.map { it.id }.distinct().size
                    Text("${fa(remainingDistinct)} کارت باقی‌مانده از ${fa(total)}", fontSize = 14.sp)
                    LinearProgressIndicator(
                        progress = { (total - remainingDistinct).toFloat() / total },
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp))
                    )
                }
                when (mode) {
                    Practice.CLASSIC -> queue.firstOrNull()?.let { c ->
                        PracticeFlip(vm, c, rev, reduce, step) { ok -> answer(ok) }
                    }
                    Practice.CHOICE -> queue.firstOrNull()?.let { c ->
                        val opts = remember(step) { Practice.options(c, ordered, all, rev) }
                        if (opts.size < 2) {
                            LaunchedEffect(step) { if (queue.isNotEmpty()) { queue.removeAt(0); step++ } }
                        } else {
                            ChoiceQuestion(vm, c, rev, opts, step) { ok -> answer(ok) }
                        }
                    }
                    Practice.TYPING -> queue.firstOrNull()?.let { c ->
                        TypingQuestion(vm, c, rev, step) { ok -> answer(ok) }
                    }
                    else -> MatchGame(vm, ordered, rev) { good, mistakes -> matchRes = good to mistakes }
                }
            }
        }
    }
    if (exit) Confirm("تمرین را متوقف کنیم؟", { exit = false; nav.popBackStack() }, { exit = false })
}

@Composable
private fun QuestionCard(vm: AppViewModel, text: String, image: String?, label: String) {
    Card(
        Modifier.fillMaxWidth().heightIn(min = 200.dp), shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 200.dp).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            CardImage(vm, image, Modifier.fillMaxWidth().heightIn(max = 220.dp))
            if (text.isNotBlank()) Text(text, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
        }
    }
}

@Composable
private fun PracticeFlip(vm: AppViewModel, card: CardEntity, rev: Boolean, reduce: Boolean, step: Int, onResult: (Boolean) -> Unit) {
    var revealed by remember(step) { mutableStateOf(false) }
    val rot by animateFloatAsState(if (revealed) 180f else 0f, tween(if (reduce) 0 else 450), label = "flip")
    val density = LocalDensity.current.density
    Card(
        Modifier.fillMaxWidth().heightIn(min = 280.dp).graphicsLayer { rotationY = rot; cameraDistance = 12f * density },
        shape = RoundedCornerShape(28.dp), elevation = CardDefaults.cardElevation(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        val front = rot <= 90f
        Column(
            Modifier.fillMaxWidth().heightIn(min = 280.dp).padding(20.dp).graphicsLayer { rotationY = if (front) 0f else 180f },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Text(if (front) "سؤال" else "پاسخ", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
            val txt = if (front) Practice.questionText(card, rev) else Practice.answerText(card, rev)
            val img = if (front) qImg(card, rev) else aImg(card, rev)
            CardImage(vm, img, Modifier.fillMaxWidth().heightIn(max = 240.dp))
            if (txt.isNotBlank()) Text(txt, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 40.sp)
        }
    }
    if (!revealed) {
        BigButton("نمایش پاسخ", { revealed = true })
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ onResult(true) }, Modifier.weight(1f).heightIn(min = 72.dp), shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GREEN, contentColor = Color.White)) {
                Icon(Icons.Filled.Check, null); Spacer(Modifier.width(6.dp))
                Text("بلدم! ✓", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Button({ onResult(false) }, Modifier.weight(1f).heightIn(min = 72.dp), shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RED, contentColor = Color.White)) {
                Icon(Icons.Filled.Close, null); Spacer(Modifier.width(6.dp))
                Text("هنوز یاد نگرفتم! ✕", fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ChoiceQuestion(
    vm: AppViewModel, card: CardEntity, rev: Boolean, options: List<CardEntity>, step: Int, onAnswer: (Boolean) -> Unit
) {
    var picked by remember(step) { mutableStateOf<Long?>(null) }
    QuestionCard(vm, Practice.questionText(card, rev), qImg(card, rev), "سؤال")
    options.forEach { o ->
        val isAns = o.id == card.id
        val sel = picked == o.id
        val p = picked
        val container = when {
            p == null -> MaterialTheme.colorScheme.surfaceVariant
            isAns -> GREEN
            sel -> RED
            else -> MaterialTheme.colorScheme.surfaceVariant
        }
        val content = if (p != null && (isAns || sel)) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        val mark = if (p != null && isAns) "✓ " else if (p != null && sel) "✕ " else ""
        Button(
            onClick = { if (picked == null) picked = o.id },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = content)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CardImage(vm, aImg(o, rev), Modifier.heightIn(max = 80.dp))
                val t = Practice.answerText(o, rev)
                if (t.isNotBlank() || mark.isNotEmpty()) Text(mark + t, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
    if (picked != null) {
        Text(if (picked == card.id) "آفرین! درسته 🎉" else "اشکال نداره، این کارت دوباره می‌آید 💛", fontWeight = FontWeight.Bold)
        BigButton("بعدی", { onAnswer(picked == card.id) })
    }
}

@Composable
private fun TypingQuestion(vm: AppViewModel, card: CardEntity, rev: Boolean, step: Int, onAnswer: (Boolean) -> Unit) {
    var text by remember(step) { mutableStateOf("") }
    var res by remember(step) { mutableStateOf<Boolean?>(null) }
    val answer = Practice.answerText(card, rev)
    QuestionCard(vm, Practice.questionText(card, rev), qImg(card, rev), "سؤال")
    OutlinedTextField(
        text, { text = it.take(200) }, Modifier.fillMaxWidth(), enabled = res == null,
        label = { Text("پاسخ را بنویس") }, singleLine = true
    )
    val r = res
    if (r == null) {
        BigButton("بررسی ✓", { res = Practice.isTypedCorrect(text, answer) }, enabled = text.isNotBlank())
        TextButton({ res = false }) { Text("نمی‌دونم") }
    } else {
        Text(
            if (r) "✓ آفرین! درسته 🎉" else "✕ پاسخ درست: $answer",
            color = if (r) GREEN else RED, fontWeight = FontWeight.Bold, fontSize = 20.sp
        )
        BigButton("بعدی", { onAnswer(r) })
    }
}

@Composable
private fun MatchGame(vm: AppViewModel, cards: List<CardEntity>, rev: Boolean, onDone: (Int, Int) -> Unit) {
    val batches = remember(cards) { cards.chunked(5) }
    var b by remember(cards) { mutableIntStateOf(0) }
    var good by remember(cards) { mutableIntStateOf(0) }
    var bad by remember(cards) { mutableIntStateOf(0) }
    val batch = batches[b]
    val rights = remember(cards, b) { batch.shuffled() }
    val matched = remember(cards, b) { mutableStateListOf<Long>() }
    var selected by remember(cards, b) { mutableStateOf<Long?>(null) }
    var wrongFlash by remember(cards, b) { mutableStateOf<Long?>(null) }

    LaunchedEffect(wrongFlash) {
        if (wrongFlash != null) { delay(600); wrongFlash = null }
    }
    Text("گروه ${fa(b + 1)} از ${fa(batches.size)}: اول یک سؤال، بعد پاسخش را بزن", fontSize = 14.sp)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            batch.forEach { c ->
                val done = c.id in matched
                MatchTile(
                    vm, Practice.questionText(c, rev), qImg(c, rev),
                    container = when {
                        done -> GREEN
                        selected == c.id -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    },
                    light = done
                ) { if (!done) selected = c.id }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rights.forEach { c ->
                val done = c.id in matched
                MatchTile(
                    vm, Practice.answerText(c, rev), aImg(c, rev),
                    container = when {
                        done -> GREEN
                        wrongFlash == c.id -> RED
                        else -> MaterialTheme.colorScheme.surface
                    },
                    light = done || wrongFlash == c.id
                ) {
                    val s = selected
                    if (!done && s != null) {
                        if (s == c.id) {
                            matched.add(c.id); good++; selected = null
                            if (matched.size == batch.size) {
                                if (b + 1 < batches.size) b++ else onDone(good, bad)
                            }
                        } else {
                            bad++; wrongFlash = c.id; selected = null
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchTile(vm: AppViewModel, text: String, image: String?, container: Color, light: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = container,
            contentColor = if (light) Color.White else MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            CardImage(vm, image, Modifier.heightIn(max = 60.dp))
            if (text.isNotBlank()) Text(text, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 3)
        }
    }
}
