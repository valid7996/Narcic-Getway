package dev.cluvex.zedsecure.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.cluvex.zedsecure.domain.model.AppLanguage
import dev.cluvex.zedsecure.domain.model.AppSettings
import dev.cluvex.zedsecure.domain.model.ThemeMode
import dev.cluvex.zedsecure.shared.resources.Res
import dev.cluvex.zedsecure.shared.resources.*
import dev.cluvex.zedsecure.ui.platform.BackHandler
import dev.cluvex.zedsecure.ui.theme.ThemeProfiles
import dev.cluvex.zedsecure.ui.theme.applyThemeProfile
import dev.cluvex.zedsecure.ui.theme.resetPersonalization
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun OnboardingFlow(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
    onLanguage: (AppLanguage) -> Unit,
    onFinish: () -> Unit,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val totalSteps = 4

    BackHandler(enabled = step > 0) {
        step--
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 44.dp, bottom = 28.dp),
        ) {
            StepIndicator(currentStep = step, totalSteps = totalSteps)
            Spacer(Modifier.height(20.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally(tween(350)) { it } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { -it } + fadeOut(tween(350)))
                    } else {
                        (slideInHorizontally(tween(350)) { -it } + fadeIn(tween(350))) togetherWith
                            (slideOutHorizontally(tween(350)) { it } + fadeOut(tween(350)))
                    }
                },
                label = "onboarding-step-transition",
                modifier = Modifier.weight(1f),
            ) { current ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                ) {
                    when (current) {
                        0 -> WelcomeStep()
                        1 -> LanguageStep(settings, onLanguage)
                        2 -> ThemeProfileStep(settings, onUpdate)
                        3 -> ThemeModeStep(settings, onUpdate)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = { step-- },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.onb_back),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                if (step < totalSteps - 1) {
                    Button(
                        onClick = { step++ },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.onb_next),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                } else {
                    Button(
                        onClick = onFinish,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                        ),
                        modifier = Modifier.height(52.dp),
                    ) {
                        Text(
                            text = stringResource(Res.string.onb_get_started),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(currentStep: Int, totalSteps: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalSteps) { idx ->
            val isCurrent = idx == currentStep
            val isDone = idx < currentStep
            val width by animateDpAsState(
                targetValue = if (isCurrent) 32.dp else 12.dp,
                animationSpec = tween(300),
                label = "indicator-width",
            )
            val color by animateColorAsState(
                targetValue = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isDone -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHighest
                },
                animationSpec = tween(300),
                label = "indicator-color",
            )
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(color),
            )
        }
    }
}

@Composable
private fun WelcomeStep() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                        ),
                    ),
                )
                .border(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary,
                        ),
                    ),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_zed_mark),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(54.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(Res.string.onb_welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.onb_welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        Spacer(Modifier.height(32.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                WelcomeFeatureRow(
                    icon = "🌐",
                    title = "زبان برنامه (Language)",
                    subtitle = "پشتیبانی کامل از فارسی، انگلیسی و سایر زبان‌ها",
                )
                WelcomeFeatureRow(
                    icon = "🎨",
                    title = "انتخاب طرح ظاهری (Themes)",
                    subtitle = "چهار تم اختصاصی: پیش‌فرض، آزکابان، سیاه و سفید، و دخترونه صورتی",
                )
                WelcomeFeatureRow(
                    icon = "🌓",
                    title = "حالت تیره و روشن (Dark Mode)",
                    subtitle = "امکان هماهنگی خودکار با سیستم یا انتخاب حالت شب/روز",
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeatureRow(icon: String, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(icon, fontSize = 22.sp)
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LanguageStep(
    settings: AppSettings,
    onLanguage: (AppLanguage) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.onb_lang_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.onb_lang_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        val languages = listOf(
            AppLanguage.Persian to ("فارسی" to "Persian"),
            AppLanguage.English to ("English" to "انگلیسی"),
            AppLanguage.System to (stringResource(Res.string.settings_language_system) to "System Default"),
            AppLanguage.Chinese to ("中文" to "Chinese"),
            AppLanguage.Russian to ("Русский" to "Russian"),
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            languages.forEach { (lang, labels) ->
                val (primaryLabel, subLabel) = labels
                val isSelected = settings.language == lang
                ChoiceCard(
                    title = primaryLabel,
                    subtitle = subLabel,
                    selected = isSelected,
                    onClick = {
                        if (settings.language != lang) onLanguage(lang)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeProfileStep(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.onb_preset_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.onb_preset_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // 1. Default (پیش‌فرض)
            val isStockSelected = settings.themeProfileId == null
            ChoiceCard(
                title = stringResource(Res.string.onb_preset_stock),
                subtitle = "سبز زمردی کلاسیک نارسیس",
                selected = isStockSelected,
                onClick = { onUpdate { it.resetPersonalization() } },
                trailing = {
                    SwatchDots(listOf(Color(0xFF0F9D6C), Color(0xFF3FD898), Color(0xFFFFCE54)))
                },
            )

            // 2, 3, 4. The 3 Ready ThemeProfiles (Azkaban, Noir, Blossom)
            ThemeProfiles.forEach { profile ->
                val isSelected = settings.themeProfileId == profile.id
                val description = when (profile.id) {
                    "azkaban" -> "آبی سایبرنتیک نئونی و های‌تک"
                    "noir" -> "مشکی مونوکروم دارک و مینیمال"
                    "blossom" -> "صورتی و رز پاستلی دخترونه"
                    else -> null
                }
                ChoiceCard(
                    title = profile.name,
                    subtitle = description,
                    selected = isSelected,
                    onClick = { onUpdate { it.applyThemeProfile(profile) } },
                    trailing = {
                        SwatchDots(profile.swatches)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThemeModeStep(
    settings: AppSettings,
    onUpdate: ((AppSettings) -> AppSettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.onb_theme_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(Res.string.onb_theme_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(20.dp))

        val modes = listOf(
            Triple(
                ThemeMode.System,
                stringResource(Res.string.settings_theme_system),
                "هماهنگ با حالت تیره یا روشن تنظیمات گوشی شما",
            ),
            Triple(
                ThemeMode.Dark,
                stringResource(Res.string.settings_theme_dark),
                "رنگ‌های تیره برای راحتی چشم و مصرف باتری کمتر",
            ),
            Triple(
                ThemeMode.Light,
                stringResource(Res.string.settings_theme_light),
                "رنگ‌های روشن برای دید واضح در نور روز",
            ),
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            modes.forEach { (mode, title, desc) ->
                val isSelected = settings.themeMode == mode
                ChoiceCard(
                    title = title,
                    subtitle = desc,
                    selected = isSelected,
                    onClick = { onUpdate { it.copy(themeMode = mode) } },
                )
            }
        }
    }
}

@Composable
private fun SwatchDots(colors: List<Color>) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        colors.forEach { swatch ->
            Box(
                Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    subtitle: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
) {
    val container by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        label = "choice-container-color",
    )
    val borderColor by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else Color.Transparent,
        label = "choice-border-color",
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = container,
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(20.dp)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f)
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (trailing != null) {
                trailing()
            } else if (selected) {
                Icon(
                    painter = painterResource(Res.drawable.ic_check_circle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
    }
}
