package com.archieapps.calendar.feature.calendar

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.archieapps.calendar.design.EntryMeta
import com.archieapps.calendar.design.LocalChronicle
import com.archieapps.calendar.design.Space
import com.archieapps.calendar.design.components.DateStepper
import com.archieapps.calendar.design.components.Pill
import com.archieapps.calendar.design.components.ScrollingPills
import com.archieapps.calendar.design.components.Section
import com.archieapps.calendar.design.components.Stepper
import java.time.LocalDate

@Composable
fun RepeatFields(
    date: LocalDate,
    rule: Recurrence,
    onDate: (LocalDate) -> Unit,
    onRule: ((Recurrence) -> Recurrence) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalChronicle.current
    val today = LocalDate.now()
    val preset = presetOf(rule, date, today)

    Column(modifier = modifier.fillMaxWidth()) {
        Section("repetir")

        ScrollingPills {
            RecurrencePreset.entries.forEach { candidate ->
                Pill(
                    label = candidate.label,
                    selected = preset == candidate,
                    onClick = {
                        if (candidate == RecurrencePreset.Today) onDate(today)

                        onRule { current -> candidate.ruleFor(date, current) }
                    },
                )
            }
        }

        if (rule.unit == RepeatUnit.Week) {
            Spacer(Modifier.height(Space.lg))
            Section("nos dias")

            ScrollingPills {
                weekOrder.forEach { day ->
                    Pill(
                        label = weekdayShort(day),
                        selected = day in rule.weekdays,
                        onClick = {
                            onRule { current ->
                                val days = if (day in current.weekdays) {
                                    current.weekdays - day
                                } else {
                                    current.weekdays + day
                                }

                                current.copy(weekdays = days.ifEmpty { setOf(date.dayOfWeek) })
                            }
                        },
                    )
                }
            }
        }

        val unit = rule.unit

        if (unit != null && preset == RecurrencePreset.Custom) {
            Spacer(Modifier.height(Space.lg))
            Section("a cada")

            Stepper(
                value = rule.interval,
                onChange = { value -> onRule { it.copy(interval = value) } },
                unit = if (rule.interval == 1) unit.one else unit.many,
                min = 1,
                max = Recurrence.MAX_INTERVAL,
            )
        }

        if (unit == RepeatUnit.Month) {
            Spacer(Modifier.height(Space.lg))
            Section("no mês")

            ScrollingPills {
                Pill(
                    label = "todo dia ${date.dayOfMonth}",
                    selected = rule.monthlyMode == MonthlyMode.DayOfMonth,
                    onClick = { onRule { it.copy(monthlyMode = MonthlyMode.DayOfMonth) } },
                )
                Pill(
                    label = "toda ${ordinalLabel(ordinalOf(date))} ${weekdayName(date.dayOfWeek)}",
                    selected = rule.monthlyMode == MonthlyMode.WeekdayOfMonth,
                    onClick = { onRule { it.copy(monthlyMode = MonthlyMode.WeekdayOfMonth) } },
                )
            }

            if (rule.monthlyMode == MonthlyMode.DayOfMonth && date.dayOfMonth > 28) {
                Spacer(Modifier.height(Space.sm))
                Text(
                    text = "meses sem o dia ${date.dayOfMonth} são pulados",
                    style = EntryMeta,
                    color = colors.slate,
                )
            }
        }

        if (rule.repeats) {
            Spacer(Modifier.height(Space.lg))
            Section("termina")

            ScrollingPills {
                Pill("nunca", rule.ending == RepeatEnding.Never, { onRule { it.copy(ending = RepeatEnding.Never) } })
                Pill("após", rule.ending == RepeatEnding.Count, { onRule { it.copy(ending = RepeatEnding.Count) } })
                Pill("em", rule.ending == RepeatEnding.Until, { onRule { it.copy(ending = RepeatEnding.Until) } })
            }

            when (rule.ending) {
                RepeatEnding.Count -> {
                    Spacer(Modifier.height(Space.md))
                    Stepper(
                        value = rule.count,
                        onChange = { value -> onRule { it.copy(count = value) } },
                        unit = if (rule.count == 1) "vez" else "vezes",
                        min = 1,
                        max = Recurrence.MAX_COUNT,
                    )
                }

                RepeatEnding.Until -> {
                    Spacer(Modifier.height(Space.md))
                    DateStepper(
                        date = rule.until ?: Recurrence.defaultUntil(date),
                        onPick = { picked -> onRule { it.copy(until = picked) } },
                        months = true,
                    )
                }

                RepeatEnding.Never -> Unit
            }
        }

        Spacer(Modifier.height(Space.md))

        Text(rule.summary(date), style = EntryMeta, color = colors.brand)
    }
}
