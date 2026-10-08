import io

# =============================================== 4. ServerCard: glass + green active frame.
p = "shared/src/commonMain/kotlin/dev/cluvex/zedsecure/ui/servers/ServerCardItem.kt"
s = io.open(p, encoding="utf-8").read()

s = s.replace("import androidx.compose.foundation.background\n",
              "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\n", 1)
s = s.replace("import dev.cluvex.zedsecure.ui.theme.Personalization\n",
              "import dev.cluvex.zedsecure.ui.theme.Personalization\nimport dev.cluvex.zedsecure.ui.theme.ZedGreen\n", 1)

old = """    val container = if (isSelectedCard) {
        MaterialTheme.colorScheme.secondaryContainer
    } else if (active) {
        personalization.serverActiveColor ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        personalization.serverCardColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    }"""
new = """    val container = if (isSelectedCard) {
        MaterialTheme.colorScheme.secondaryContainer
    } else if (active) {
        personalization.serverActiveColor ?: ZedGreen
    } else {
        personalization.serverCardColor
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }
    val frame = when {
        isSelectedCard -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)
        active -> ZedGreen.copy(alpha = 0.95f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    }"""
assert old in s, "container anchor"
s = s.replace(old, new, 1)

old = """            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp))
                .background(container)
                .clickable(onClick = onClick),"""
new = """            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .clip(RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp))
                .background(container)
                .border(1.dp, frame, RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp))
                .clickable(onClick = onClick),"""
assert old in s, "card bg anchor"
s = s.replace(old, new, 1)

io.open(p, "w", encoding="utf-8", newline="").write(s)

# =============================================== 5. AutoSelectCard: same glass language.
p = "shared/src/commonMain/kotlin/dev/cluvex/zedsecure/ui/servers/AutoSelectUi.kt"
s = io.open(p, encoding="utf-8").read()

s = s.replace("import androidx.compose.foundation.background\n",
              "import androidx.compose.foundation.background\nimport androidx.compose.foundation.border\n", 1)
s = s.replace("import dev.cluvex.zedsecure.ui.theme.Personalization\n",
              "import dev.cluvex.zedsecure.ui.theme.Personalization\nimport dev.cluvex.zedsecure.ui.theme.ZedGreen\n", 1)

old = """    val container = if (active) {
        personalization.serverActiveColor ?: MaterialTheme.colorScheme.primaryContainer
    } else {
        personalization.serverCardColor ?: MaterialTheme.colorScheme.surfaceContainerHigh
    }"""
new = """    val container = if (active) {
        personalization.serverActiveColor ?: ZedGreen
    } else {
        personalization.serverCardColor
            ?: MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    }"""
assert old in s, "auto container anchor"
s = s.replace(old, new, 1)

old = """    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp),
        color = container,
        contentColor = onContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {"""
new = """    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp),
        color = Color.Transparent,
        contentColor = onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                if (active) ZedGreen.copy(alpha = 0.95f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp),
            ),
    ) {
        val glassBackground = Modifier
            .background(
                container,
                RoundedCornerShape(personalization.cornerStyle.serverCardDp.dp),
            )
        // the glass wash draws behind the row via the first Box below
"""
assert old in s, "auto surface anchor"
s = s.replace(old, new, 1)

# wrap the Row with the glass background: simplest is applying background to the Row.
old = """        // the glass wash draws behind the row via the first Box below
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {"""
new = """        Row(
            glassBackground
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {"""
assert old in s, "auto row anchor"
s = s.replace(old, new, 1)

if "import androidx.compose.ui.graphics.Color\n" not in s:
    s = s.replace("import androidx.compose.ui.graphics\n",
                  "import androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.graphics\n", 1)
io.open(p, "w", encoding="utf-8", newline="").write(s)
print("cards patched")
