package com.archieapps.calendar.feature.tasks

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.archieapps.calendar.core.action.TaskActions
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.EntryTitle
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.Stroke
import com.archieapps.calendar.design.TokenSuccess
import com.archieapps.calendar.design.TokenWarning
import com.archieapps.calendar.design.components.Glyph
import com.archieapps.calendar.feature.calendar.CalendarEntry
import com.archieapps.calendar.feature.calendar.TaskAction
import com.archieapps.calendar.feature.calendar.togglable
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.ExternalLink
import com.composables.icons.lucide.Lucide
import java.time.LocalDate

private val cardRadius = Space.md
private val cardShape = RoundedCornerShape(cardRadius)
private val cardMinHeight = 64.dp
private val stripeWidth = 3.dp
private val nodeBox = 40.dp
private val nodeDiameter = 24.dp
private val ringWidth = 2.dp
private val chevronBox = 40.dp
private val actionIcon = 22.dp

private const val pressScale = 0.98f
private const val pressLift = 0.55f
private const val edgeAlpha = 0.55f
private const val dimAlpha = 0.35f

@Composable
fun TaskRowItem(
    entry: CalendarEntry,
    today: LocalDate,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onActionFailed: (TaskAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    val overdue = TaskBucket.of(entry.bucket) == TaskBucket.Overdue
    val togglable = entry.togglable(today)

    val interaction = remember { MutableInteractionSource() }
    val pressed = interaction.collectIsPressedAsState()

    val press = animateFloatAsState(
        targetValue = if (pressed.value) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "press",
    )

    val surface = animateColorAsState(
        targetValue = if (pressed.value) colors.muted else colors.surface,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "surface",
    )

    val edge = animateColorAsState(
        targetValue = if (pressed.value) colors.brand.copy(alpha = edgeAlpha) else colors.hairline,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "edge",
    )

    val settled = animateFloatAsState(
        targetValue = if (entry.completed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "settled",
    )

    val stripe = entry.color.copy(alpha = if (entry.completed) dimAlpha else 1f)

    val lift = when {
        !togglable -> 0f
        entry.completed -> -pressLift
        else -> pressLift
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = Space.sm)
            .heightIn(min = cardMinHeight)
            .graphicsLayer {
                val shrink = 1f - (1f - pressScale) * press.value

                scaleX = shrink
                scaleY = shrink
                clip = true
                shape = cardShape
            }
            .drawWithContent {
                drawRect(color = surface.value)
                drawRect(color = stripe, size = Size(stripeWidth.toPx(), size.height))

                drawContent()

                val line = Stroke.hairline.toPx()

                drawRoundRect(
                    color = edge.value,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = CornerRadius(cardRadius.toPx() - line / 2),
                    style = DrawStroke(width = line),
                )
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(color = colors.brand),
                onClickLabel = if (entry.completed) "reabrir" else "concluir",
                role = Role.Checkbox,
                onLongClickLabel = "abrir detalhes",
                onLongClick = onOpen,
                onClick = {
                    if (togglable) {
                        haptics.performHapticFeedback(
                            if (entry.completed) HapticFeedbackType.ContextClick else HapticFeedbackType.Confirm,
                        )
                        onToggle()
                    } else {
                        onOpen()
                    }
                },
            )
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(entry.completed)
                contentDescription = rowLabel(entry, today)
            }
            .padding(start = Space.sm, end = Space.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CheckDisc(
            ring = if (togglable) colors.brand else colors.hairline,
            fill = TokenSuccess,
            check = colors.ground,
            settled = settled,
            press = press,
            lift = lift,
        )

        Column(modifier = Modifier.weight(1f).padding(vertical = Space.sm)) {
            Text(
                text = entry.title,
                style = EntryTitle,
                color = if (entry.completed) colors.slate else colors.ink,
                textDecoration = if (entry.completed) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(Space.xxs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                PriorityDot(priority = entry.priority, hidden = entry.completed)

                Text(
                    text = anchorCaption(entry, today),
                    style = EntryMeta,
                    color = if (overdue) colors.destructive else colors.slate,
                    maxLines = 1,
                )

                val trail = buildList {
                    entry.clock?.let { add(it) }
                    recurrenceCaption(entry)?.let { add("↻ $it") }
                    dueCaption(entry, today)?.let { add(it) }
                    horizonCaption(entry, today)?.let { add(it) }
                    entry.categoryName?.let { add(it) }
                    streakCaption(entry)?.let { add(it) }
                }

                if (trail.isNotEmpty()) {
                    Text(
                        text = " · " + trail.joinToString(" · "),
                        style = EntryMeta,
                        color = colors.slate,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        entry.action?.let { action ->
            ActionTarget(
                action = action,
                dim = entry.completed,
                onClick = {
                    if (!TaskActions.launch(context, action)) {
                        onActionFailed(action)
                    }
                },
            )
        }

        DetailTarget(label = "Detalhes de ${entry.title}", onClick = onOpen)
    }
}

@Composable
private fun CheckDisc(
    ring: Color,
    fill: Color,
    check: Color,
    settled: State<Float>,
    press: State<Float>,
    lift: Float,
) {
    Box(modifier = Modifier.size(nodeBox), contentAlignment = Alignment.Center) {
        Spacer(
            Modifier.size(nodeDiameter).drawBehind {
                val filled = (settled.value + lift * press.value).coerceIn(0f, 1f)
                val stroke = ringWidth.toPx()
                val radius = size.minDimension / 2 - stroke / 2
                val center = Offset(size.width / 2, size.height / 2)

                drawCircle(
                    color = ring,
                    radius = radius,
                    center = center,
                    alpha = 1f - filled * 0.9f,
                    style = DrawStroke(width = stroke),
                )

                if (filled <= 0f) {
                    return@drawBehind
                }

                drawCircle(color = fill, radius = radius * (filled / 0.6f).coerceAtMost(1f), center = center)

                val reveal = ((filled - 0.35f) / 0.65f).coerceIn(0f, 1f)

                if (reveal <= 0f) {
                    return@drawBehind
                }

                val arm = radius * 0.52f
                val start = Offset(center.x - arm, center.y)
                val knee = Offset(center.x - arm * 0.15f, center.y + arm * 0.72f)
                val tip = Offset(center.x + arm, center.y - arm * 0.6f)
                val toKnee = (knee - start).getDistance()
                val toTip = (tip - knee).getDistance()
                val walked = (toKnee + toTip) * reveal
                val width = stroke * 1.3f

                drawLine(
                    color = check,
                    start = start,
                    end = lerp(start, knee, (walked / toKnee).coerceAtMost(1f)),
                    strokeWidth = width,
                    cap = StrokeCap.Round,
                )

                if (walked > toKnee) {
                    drawLine(
                        color = check,
                        start = knee,
                        end = lerp(knee, tip, ((walked - toKnee) / toTip).coerceAtMost(1f)),
                        strokeWidth = width,
                        cap = StrokeCap.Round,
                    )
                }
            }
        )
    }
}

@Composable
private fun PriorityDot(priority: String, hidden: Boolean) {
    val colors = LocalChronicle.current

    val tint = when {
        hidden -> null
        priority == "high" -> colors.destructive
        priority == "medium" -> TokenWarning
        else -> null
    }

    if (tint == null) {
        return
    }

    Spacer(Modifier.size(Stroke.node).clip(CircleShape).background(tint))
    Spacer(Modifier.width(Space.xs))
}

@Composable
private fun ActionTarget(action: TaskAction, dim: Boolean, onClick: () -> Unit) {
    val colors = LocalChronicle.current
    val context = LocalContext.current
    val iconPx = with(LocalDensity.current) { actionIcon.roundToPx() }
    val caption = remember(action) {
        action.caption(if (action.isApp) TaskActions.appLabel(context, action.target) else null)
    }
    val badge = remember(action, iconPx, dim) {
        if (action.isApp) TaskActions.roundIcon(context, action.target, iconPx, dim) else null
    }

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(chevronBox)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.brand),
                onClickLabel = "abrir",
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = "Abrir $caption" },
        contentAlignment = Alignment.Center,
    ) {
        if (badge == null) {
            Glyph(
                icon = Lucide.ExternalLink,
                tint = if (dim) colors.slate else colors.brand,
                size = 17.dp,
            )
        } else {
            Image(
                bitmap = badge.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(actionIcon),
            )
        }
    }
}

@Composable
private fun DetailTarget(label: String, onClick: () -> Unit) {
    val colors = LocalChronicle.current

    Box(
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .size(chevronBox)
            .clip(CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(color = colors.slate),
                onClickLabel = "abrir detalhes",
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(icon = Lucide.ChevronRight, tint = colors.slate, size = 18.dp)
    }
}
