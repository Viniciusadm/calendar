package com.archieapps.calendar.feature.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.ImageProvider
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.CheckboxDefaults
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity as actionStartIntent
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.archieapps.calendar.MainActivity
import com.archieapps.calendar.R
import com.archieapps.calendar.core.store.Settings
import com.archieapps.calendar.core.store.TaskSnapshot
import com.archieapps.calendar.core.store.TaskSnapshotRow
import com.archieapps.calendar.core.store.TaskSnapshotStore
import com.archieapps.calendar.core.store.WidgetRevision
import com.archieapps.calendar.design.ThemeMode
import com.archieapps.calendar.design.TokenSuccess
import com.archieapps.calendar.design.TokenWarning
import com.archieapps.calendar.design.chronicleColors
import com.archieapps.calendar.design.colorFromValue
import com.archieapps.calendar.feature.settings.readPreferences
import com.archieapps.calendar.feature.settings.theme

const val EXTRA_OCCURRENCE = "occurrenceId"

private val focusKey = androidx.glance.action.ActionParameters.Key<String>(EXTRA_OCCURRENCE)

private val stripeWidth = 3.dp

class TodayTasksWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val widgetId = runCatching { GlanceAppWidgetManager(context).getAppWidgetId(id) }.getOrNull()

        provideContent {
            val revision by WidgetRevision.value.collectAsState()

            val skin = remember(revision) { WidgetSkin.of(Settings(context).readPreferences().theme()) }
            val config = remember(revision) {
                widgetId?.let { WidgetConfigStore(context).load(it) } ?: WidgetConfig()
            }
            val snapshot = remember(revision) {
                widgetId?.let { TaskSnapshotStore(context).load(it) } ?: TaskSnapshot()
            }

            Body(context = context, snapshot = snapshot, config = config, skin = skin)
        }
    }
}

class TodayTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayTasksWidget()

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)

        val configs = WidgetConfigStore(context)
        val snapshots = TaskSnapshotStore(context)

        appWidgetIds.forEach { id ->
            configs.forget(id)
            snapshots.forget(id)
        }
    }
}

@Composable
private fun Body(
    context: Context,
    snapshot: TaskSnapshot,
    config: WidgetConfig,
    skin: WidgetSkin,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ImageProvider(skin.ground))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Header(snapshot = snapshot, config = config, skin = skin)

        Spacer(GlanceModifier.height(8.dp))

        if (snapshot.rows.isEmpty()) {
            Empty(config = config, skin = skin)

            return@Column
        }

        val fits = rowsThatFit(LocalSize.current.height, snapshot.rows.size)
        val shown = snapshot.rows.take(fits)
        val hidden = snapshot.total - shown.size

        Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            shown.forEachIndexed { index, row ->
                TaskCard(
                    context = context,
                    row = row,
                    config = config,
                    skin = skin,
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .defaultWeight()
                        .padding(bottom = if (index == shown.lastIndex) 0.dp else cardGap),
                )
            }
        }

        Footer(hidden = hidden, stale = snapshot.stale, skin = skin)
    }
}

private val cardHeight = 44.dp
private val cardGap = 6.dp
private val chromeHeight = 48.dp
private val footerHeight = 16.dp

private fun rowsThatFit(available: Dp, rows: Int): Int {
    val forList = available - chromeHeight

    if (forList <= cardHeight) return 1

    val whole = ((forList + cardGap) / (cardHeight + cardGap)).toInt().coerceAtLeast(1)

    if (whole >= rows) return rows

    val withFooter = ((forList - footerHeight + cardGap) / (cardHeight + cardGap)).toInt()

    return withFooter.coerceIn(1, rows)
}

@Composable
private fun Header(snapshot: TaskSnapshot, config: WidgetConfig, skin: WidgetSkin) {
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = config.filterLabel.uppercase(),
            style = TextStyle(color = skin.slate, fontSize = 11.sp, fontWeight = FontWeight.Medium),
        )

        Spacer(GlanceModifier.width(8.dp))

        Text(
            text = headline(snapshot),
            style = TextStyle(color = skin.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
        )

        if (snapshot.overdue > 0 && config.filter != "overdue") {
            Spacer(GlanceModifier.width(8.dp))

            Text(
                text = overdueLine(snapshot.overdue),
                style = TextStyle(color = skin.destructive, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun Empty(config: WidgetConfig, skin: WidgetSkin) {
    Text(
        text = when (config.filter) {
            "overdue" -> "nada atrasado"
            "upcoming" -> "nada a caminho"
            else -> "nada para ${config.filterLabel}"
        },
        style = TextStyle(color = skin.slate, fontSize = 14.sp),
    )
}

@Composable
private fun TaskCard(
    context: Context,
    row: TaskSnapshotRow,
    config: WidgetConfig,
    skin: WidgetSkin,
    modifier: GlanceModifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(ImageProvider(skin.card))
                .padding(end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Stripe(row = row, skin = skin)

            CheckBox(
                checked = row.completed,
                onCheckedChange = if (row.togglable) {
                    actionRunCallback<ToggleTaskAction>(
                        actionParametersOf(
                            ToggleTaskAction.occurrenceId to row.occurrenceId,
                            ToggleTaskAction.completed to row.completed,
                        )
                    )
                } else {
                    null
                },
                text = "",
                colors = CheckboxDefaults.checkBoxColors(
                    checkedColor = ColorProvider(TokenSuccess),
                    uncheckedColor = if (row.togglable) skin.brand else skin.hairline,
                ),
            )

            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(focusKey to row.occurrenceId)
                        )
                    ),
            ) {
                Text(
                    text = row.title,
                    maxLines = 1,
                    style = TextStyle(
                        color = if (row.completed) skin.slate else skin.ink,
                        fontSize = 14.sp,
                        textDecoration = if (row.completed) TextDecoration.LineThrough else TextDecoration.None,
                    ),
                )

                meta(row)?.let { caption ->
                    Text(
                        text = caption,
                        maxLines = 1,
                        style = TextStyle(
                            color = if (row.overdue) skin.destructive else skin.slate,
                            fontSize = 11.sp,
                        ),
                    )
                }
            }

            if (config.showActions) {
                ActionGlyph(context = context, row = row, skin = skin)
            }
        }
    }
}

@Composable
private fun Stripe(row: TaskSnapshotRow, skin: WidgetSkin) {
    val tint = colorFromValue(row.color) ?: priorityTint(row.priority) ?: skin.accentColor

    Box(
        modifier = GlanceModifier
            .width(stripeWidth)
            .fillMaxHeight()
            .background(if (row.completed) tint.copy(alpha = 0.35f) else tint),
    ) {}
}

@Composable
private fun ActionGlyph(context: Context, row: TaskSnapshotRow, skin: WidgetSkin) {
    val intent = actionIntent(context, row) ?: return

    Box(
        modifier = GlanceModifier.padding(horizontal = 6.dp).clickable(actionStartIntent(intent)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (row.actionType == "app") "▣" else "↗",
            style = TextStyle(color = skin.brand, fontSize = 15.sp),
        )
    }
}

private fun actionIntent(context: Context, row: TaskSnapshotRow): Intent? {
    val type = row.actionType ?: return null
    val target = row.actionTarget?.takeIf { it.isNotBlank() } ?: return null

    val intent = if (type == "app") {
        context.packageManager.getLaunchIntentForPackage(target)
    } else {
        runCatching { Intent(Intent.ACTION_VIEW, android.net.Uri.parse(target)) }.getOrNull()
    }

    return intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}

private fun meta(row: TaskSnapshotRow): String? {
    val parts = buildList {
        row.caption?.let { add(it) }
        row.clock?.let { add(it) }
        if (row.recurring) add("↻")
    }

    return parts.joinToString(" · ").takeIf { it.isNotBlank() }
}

private fun priorityTint(priority: String): Color? = when (priority) {
    "high" -> Color(0xFFE62B34)
    "medium" -> TokenWarning
    else -> null
}

private fun headline(snapshot: TaskSnapshot): String {
    val pending = snapshot.pending

    return when {
        snapshot.rows.isEmpty() -> "livre"
        pending == 0 -> "tudo feito"
        pending == 1 -> "1 pendente"
        else -> "$pending pendentes"
    }
}

private fun overdueLine(count: Int): String =
    if (count == 1) "1 atrasada" else "$count atrasadas"

@Composable
private fun Footer(hidden: Int, stale: Boolean, skin: WidgetSkin) {
    if (hidden <= 0 && !stale) {
        return
    }

    Text(
        text = when {
            stale -> "atualizando…"
            hidden == 1 -> "e mais 1"
            else -> "e mais $hidden"
        },
        style = TextStyle(color = skin.slate, fontSize = 11.sp),
    )
}

class WidgetSkin(
    val ground: Int,
    val card: Int,
    val ink: ColorProvider,
    val slate: ColorProvider,
    val brand: ColorProvider,
    val hairline: ColorProvider,
    val destructive: ColorProvider,
    val accentColor: Color,
) {
    companion object {
        fun of(mode: ThemeMode): WidgetSkin {
            val light = chronicleColors(dark = false)
            val dark = chronicleColors(dark = true)

            fun pick(pickLight: Color, pickDark: Color): ColorProvider = when (mode) {
                ThemeMode.Light -> ColorProvider(pickLight)
                ThemeMode.Dark -> ColorProvider(pickDark)
                ThemeMode.System -> androidx.glance.color.ColorProvider(day = pickLight, night = pickDark)
            }

            return WidgetSkin(
                ground = when (mode) {
                    ThemeMode.Light -> R.drawable.widget_ground_light
                    ThemeMode.Dark -> R.drawable.widget_ground_dark
                    ThemeMode.System -> R.drawable.widget_ground
                },
                card = when (mode) {
                    ThemeMode.Light -> R.drawable.widget_card_light
                    ThemeMode.Dark -> R.drawable.widget_card_dark
                    ThemeMode.System -> R.drawable.widget_card
                },
                ink = pick(light.ink, dark.ink),
                slate = pick(light.slate, dark.slate),
                brand = pick(light.brand, dark.brand),
                hairline = pick(light.hairline, dark.hairline),
                destructive = pick(light.destructive, dark.destructive),
                accentColor = light.brand,
            )
        }
    }
}
