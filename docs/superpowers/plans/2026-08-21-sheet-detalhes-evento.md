# Sheet de detalhes do evento — Plano de implementação

**Goal:** Tirar o "Fechar" do corpo do sheet, redesenhar as ações como pills com
cor semântica, e enriquecer o sheet com próximas ocorrências, descrição,
prioridade, horário completo, lembretes, checklist e estado da ocorrência.

**Architecture:** O backend ganha um `SeriesPreview` que expande a série para
frente reusando o `RecurrenceExpander`, exposto num campo novo do
`CalendarPresenter::event()` que só o `show` preenche. O app passa a buscar
`GET /events/{id}` ao focar uma entrada, guarda o resultado num estado
`focusedDetail` separado, e o `EntrySheet` desenha os blocos que existirem —
com os pills ancorados logo abaixo do cabeçalho, para que o alvo de toque não se
mova quando a resposta da rede chega.

**Tech Stack:** Laravel + PHPUnit (`../back`); Kotlin + Jetpack Compose,
Material3 1.4.0, kotlinx.serialization (`.`).

**Spec:** `docs/superpowers/specs/2026-08-21-sheet-detalhes-evento-design.md`

## Global Constraints

- Sem comentários explicativos em nenhum arquivo (regra global do usuário).
- Todo identificador em inglês; texto de UI em português.
- Sem escrever nem rodar testes neste plano (pedido explícito do usuário).
- Sem commits (pedido explícito do usuário).
- `Space`/`Stroke` de `design/Spacing.kt` para toda medida; nenhum `.dp` solto.
- Nenhum `TextButton` novo — usar `TextAction` ou `Pill`.
- Todo bloco vazio desaparece inteiro, header Eyebrow incluído.

---

## File Structure

**`../back`**
- Create: `app/Calendar/Support/SeriesPreview.php` — expande a série para frente
- Modify: `app/Services/Calendar/CalendarPresenter.php` — campo `nextOccurrences`
- Modify: `app/Http/Controllers/Calendar/CalendarEventController.php` — `show` preenche

**`.` (app)**
- Modify: `app/src/main/java/com/archieapps/calendar/design/Palette.kt` — vermelho do escuro
- Modify: `app/src/main/java/com/archieapps/calendar/design/Theme.kt` — `destructive`
- Modify: `app/src/main/java/com/archieapps/calendar/design/components/Pill.kt` — variante de comando
- Modify: `app/src/main/java/com/archieapps/calendar/core/net/Dto.kt` — DTOs novos
- Modify: `app/src/main/java/com/archieapps/calendar/feature/calendar/Recurrence.kt` — `shortDate`, `relativeDays`
- Modify: `app/src/main/java/com/archieapps/calendar/feature/calendar/CalendarModel.kt` — campos que o feed já manda
- Create: `app/src/main/java/com/archieapps/calendar/feature/calendar/EntryDetail.kt` — estado e mapeamento
- Modify: `app/src/main/java/com/archieapps/calendar/feature/calendar/CalendarViewModel.kt` — busca no focus
- Modify: `app/src/main/java/com/archieapps/calendar/feature/calendar/EntrySheet.kt` — reescrito
- Modify: `app/src/main/java/com/archieapps/calendar/MainActivity.kt` — call site

Ordem: backend primeiro (define o contrato), depois design system, depois
dados, depois estado, depois layout. Cada tarefa compila sozinha.

---

### Task 1: `SeriesPreview` no backend

**Files:** Create `../back/app/Calendar/Support/SeriesPreview.php`

**Produces:** `next(CalendarEvent $event, int $take = 3, int $horizonDays = 365): array`
devolvendo `[['seriesDate' => string, 'date' => string, 'time' => ?string, 'allDay' => bool], ...]`

- [ ] Construtor recebe `RecurrenceExpander $expander`, como o `StoredEventExpander`.
- [ ] `next()` devolve `[]` quando `! $event->isRecurring()` ou `$event->recurrence() === null`.
- [ ] `$from` = amanhã no fuso de `config('calendar.timezone')`; `$to` = `$from + $horizonDays`.
      Recorta `$to` por `$event->recurrence_ends_at` quando ele for menor.
- [ ] Expande com `$this->expander->between($rule, $event->anchor(), $from, $to)`.
- [ ] Carrega os overrides do evento em `CalendarEventOccurrence::where('event_id', $event->id)`,
      chaveados por `occurrenceDateString()`, igual ao `StoredEventExpander::overridesFor`.
- [ ] Para cada `seriesDate`: pula se o override `isCancelled()`; a data efetiva é
      `$override?->movedDateString() ?? $seriesDate`; pula se a data efetiva for `< $from`.
- [ ] Acrescenta as ocorrências movidas PARA dentro da janela cuja `seriesDate` ficou fora
      dela, espelhando o laço de overrides do `StoredEventExpander::seriesDates`.
- [ ] Ordena pela data efetiva, corta em `$take`.
- [ ] `time` e `allDay` saem do override quando houver, senão do evento —
      `$override?->startTimeString() ?? $event->startTimeString()`.

### Task 2: expor no presenter e no controller

**Files:** Modify `../back/app/Services/Calendar/CalendarPresenter.php`,
`../back/app/Http/Controllers/Calendar/CalendarEventController.php`

**Consumes:** `SeriesPreview::next` (Task 1)
**Produces:** campo JSON `nextOccurrences` em `GET /api/calendar/events/{id}`

- [ ] Assinatura vira `public function event(CalendarEvent $event, array $nextOccurrences = []): array`.
- [ ] Novo `'nextOccurrences' => $nextOccurrences` no segundo `array_merge`, ao lado de `items`.
- [ ] `CalendarEventController` recebe `SeriesPreview $seriesPreview` no construtor.
- [ ] Só `show()` passa: `$this->presenter->event($event, $this->seriesPreview->next($event))`.
      `store()` e `update()` seguem chamando com um argumento.

### Task 3: cor destrutiva no tema

**Files:** Modify `design/Palette.kt`, `design/Theme.kt`

**Produces:** `ChronicleColors.destructive: Color`

- [ ] `Palette.kt` ganha `internal val TokenDestructiveDark = Color(0xFFFF6B6B)` —
      `TokenDestructive` (`E62B34`) sobre `GroundDark` (`0A131A`) fica pesado.
- [ ] `ChronicleColors` ganha `val destructive: Color` depois de `brandSoft`.
- [ ] `lightChronicle`: `destructive = TokenDestructive`.
- [ ] `darkChronicle`: `destructive = TokenDestructiveDark`.

### Task 4: variante de comando no `Pill`

**Files:** Modify `design/components/Pill.kt`

**Consumes:** `ChronicleColors.destructive` (Task 3)
**Produces:** `Pill(label, onClick, tint, dot, modifier)` — sobrecarga de comando

- [ ] Mantém o `Pill(label, selected, onClick, dot, modifier)` atual intocado;
      é ele que a tela de editor usa para seleção.
- [ ] Nova sobrecarga sem `selected`, com `tint: Color`: rótulo em `tint`, borda em
      `tint.copy(alpha = 0.55f)`, fundo sempre `Color.Transparent`. Nunca `brandSoft` —
      o preenchimento fica reservado para seleção.
- [ ] Extrai o corpo compartilhado num privado `PillBody(label, labelColor, borderColor, background, dot, onClick, modifier)`
      para as duas sobrecargas não duplicarem o layout.

### Task 5: DTOs

**Files:** Modify `core/net/Dto.kt`

**Produces:** `SeriesDateDto`, `EventItemDto`, campos novos em `EventDto` e `OccurrenceDto`

- [ ] `SeriesDateDto(seriesDate: String, date: String, time: String? = null, allDay: Boolean = true)`.
- [ ] `EventItemDto(id: Int, title: String, durationMinutes: Int? = null, position: Int = 0)`.
- [ ] `EventDto` ganha `recurrenceEndsAt: String? = null`,
      `items: List<EventItemDto> = emptyList()`,
      `nextOccurrences: List<SeriesDateDto> = emptyList()`.
- [ ] `OccurrenceDto` já tem tudo que o sheet precisa; nada a acrescentar.
      Todos os campos novos têm default, então `EventDraft.from` continua compilando.

### Task 6: formatadores de data em `Recurrence.kt`

**Files:** Modify `feature/calendar/Recurrence.kt`

**Produces:** `shortDate(date: LocalDate): String`, `relativeDays(date: LocalDate): String`

- [ ] `shortDate`: `"ter, 2 set"` usando `weekdayShort` e o `monthShort` privado que já
      existem. Anexa `" de {ano}"` quando `date.year != LocalDate.now().year`.
- [ ] `relativeDays`: dias entre hoje e a data via `ChronoUnit.DAYS`.
      `0` → `"hoje"`; `1` → `"amanhã"`; `n > 1` → `"em N dias"`;
      `-1` → `"ontem"`; `n < -1` → `"há N dias"`.

### Task 7: `CalendarEntry` para de descartar o feed

**Files:** Modify `feature/calendar/CalendarModel.kt`

**Produces:** `CalendarEntry` com `description`, `endTime`, `durationMinutes`,
`priority`, `completedAt`, `overridden`, `remindersMuted`

- [ ] Sete campos novos no `data class`, todos vindos direto do `OccurrenceDto`.
- [ ] `toEntry()` mapeia cada um. `description` é campo próprio; `note` continua
      saindo de `meta.age`, que é o que dá "faz N anos" no aniversário.
- [ ] `completedAt` guarda só a data: `completedAt?.take(10)`, porque o backend manda
      ISO-8601 completo e o sheet só mostra o dia.

### Task 8: `EntryDetail`

**Files:** Create `feature/calendar/EntryDetail.kt`

**Consumes:** `EventDto`, `SeriesDateDto`, `EventItemDto` (Task 5); `relativeDays`, `shortDate` (Task 6)
**Produces:** `DetailState` (`Loading`/`Ready`/`Absent`), `EntryDetail`, `NextOccurrence`, `reminderLabel`

- [ ] `sealed interface DetailState` com `Loading`, `Absent` (objects) e
      `Ready(val detail: EntryDetail)`.
- [ ] `data class NextOccurrence(val date: LocalDate, val time: String?, val allDay: Boolean)`
      com `val label get() = shortDate(date)` e `val relative get() = relativeDays(date)`.
- [ ] `data class EntryDetail(next: List<NextOccurrence>, repeatsUntil: LocalDate?, reminders: List<String>, items: List<ChecklistItem>)`.
- [ ] `data class ChecklistItem(val title: String, val durationMinutes: Int?)`.
- [ ] `fun EventDto.toDetail(): EntryDetail` — `next` a partir de `nextOccurrences`
      (parse de `date`), `repeatsUntil` de `recurrenceEndsAt`, `reminders` filtrando
      `active` e mapeando por `reminderLabel`, `items` ordenados por `position`.
- [ ] `fun reminderLabel(minutes: Int): String` — casa exato com `reminderChoices`
      (`EventDraft.kt:11`) primeiro; senão deriva: `< 60` → `"N min antes"`;
      múltiplo de `1440` → `"N dia(s) antes"`; múltiplo de `60` → `"N h antes"`;
      resto → `"N min antes"`. O campo é livre no banco, então não dá para
      depender só da lista de escolhas do editor.

### Task 9: busca no `focus`

**Files:** Modify `feature/calendar/CalendarViewModel.kt`

**Consumes:** `DetailState`, `toDetail()` (Task 8)
**Produces:** `CalendarState.focusedDetail: DetailState`

- [ ] `CalendarState` ganha `val focusedDetail: DetailState = DetailState.Absent`.
- [ ] Campo `private var detailJob: Job? = null` na classe, como o padrão de job que o
      ViewModel já usa.
- [ ] `focus(entry)` reescrito: cancela `detailJob`; define `focused = entry`;
      se `entry?.eventId == null`, define `focusedDetail = Absent` e retorna;
      senão define `Loading` e lança a busca.
- [ ] Na volta, aplica só se `_state.value.focused?.eventId == eventId` — resposta de
      uma entrada que não é mais a focada é descartada.
- [ ] `ApiResult.Ok` → `Ready(result.value.toDetail())`. `ApiResult.Failure` → `Absent`,
      **sem** mexer em `error`: falha de rede e o 404 de evento protegido degradam em
      silêncio para a versão enxuta.

### Task 10: `EntrySheet` reescrito

**Files:** Modify `feature/calendar/EntrySheet.kt`, `MainActivity.kt`

**Consumes:** tudo das tarefas 3 a 9

- [ ] Assinatura: `EntrySheet(entry, detail, onEdit, onToggleCompletion, onCancelOccurrence, onDeleteSeries, modifier)`.
      `onDismiss` sai — não há mais nada no corpo que dispense.
- [ ] Ordem: título; linha de meta; descrição; bloco de estado; pills; hairline ou
      progresso; PRÓXIMAS; LEMBRETE; CHECKLIST.
- [ ] Linha de meta via `buildAnnotatedString`, juntando com `" · "`: faixa de horário
      (`"14:30 – 15:30"` quando `endTime` difere do início, senão só o início) ou
      `"dia inteiro"`; categoria; `Recurrence.parse(rule).summary()`; prioridade;
      `note`. Prioridade aparece quando `!= "none"`, rótulo em português, e **só
      `"high"` recebe `SpanStyle(color = colors.brand)`.
- [ ] Bloco de estado, uma linha `EntryMeta` slate por condição verdadeira:
      `completed` → `"concluído em {shortDate}"` ou `"concluído"` sem `completedAt`;
      `overridden` → `"esta ocorrência foi editada"`;
      `remindersMuted` → `"lembretes silenciados nesta ocorrência"`.
- [ ] Pills numa `Row` com `horizontalScroll` e `Arrangement.spacedBy(Space.sm)`, igual
      ao `ScrollingPills` do editor. Ordem: `editar` (ink); `concluir`/`reabrir` (brand)
      quando `isTask`; `remover` (destructive); `remover série` (destructive) quando
      `recurring`. Nenhum pill quando `agency != Mine`.
- [ ] `LinearProgressIndicator` de `Stroke.hairline` quando `detail is Loading`, no lugar
      da `Hairline`; ambos suprimidos quando não há bloco nenhum a mostrar.
- [ ] `Hairline` privado no arquivo, mesmo desenho do `CategoriesScreen`.
- [ ] PRÓXIMAS: header `Eyebrow`; uma `Row` por ocorrência com o dot de `Stroke.node` na
      `entry.color`, `shortDate` à esquerda, `Spacer(weight(1f))`, `relativeDays` à
      direita em slate. `repeatsUntil` como nota `"repete até {shortDate}"`.
- [ ] LEMBRETE: header `Eyebrow`, uma linha por rótulo.
- [ ] CHECKLIST: header `Eyebrow`, `"· {title}"` e a duração à direita em slate quando
      houver.
- [ ] Cada bloco dentro de um `if (lista.isNotEmpty())` que engloba o header e o
      `Spacer` anterior — nenhum `Spacer` órfão.
- [ ] `MainActivity.kt` passa `detail = state.focusedDetail` e remove o `onDismiss`.

---

## Self-Review

**Cobertura da spec:** "Fechar" fora do corpo → Task 10. Pills com cor semântica →
Tasks 3, 4, 10. Próximas → Tasks 1, 2, 5, 8, 10. Descrição/prioridade/horário →
Tasks 7, 10. Lembretes/checklist → Tasks 5, 8, 9, 10. Estado da ocorrência →
Tasks 7, 10. Degradação silenciosa → Task 9. Projected sem `eventId` → Tasks 9, 10.
Ancoragem dos pills → Task 10. Testes: fora de escopo por pedido do usuário.

**Consistência de tipos:** `DetailState` nomeado igual nas Tasks 8, 9 e 10.
`nextOccurrences` é o mesmo nome no JSON (Task 2), no DTO (Task 5) e no mapeamento
(Task 8). `destructive` é o mesmo nome na Task 3 e na Task 10. `shortDate` e
`relativeDays` definidos na Task 6, consumidos nas Tasks 8 e 10.
