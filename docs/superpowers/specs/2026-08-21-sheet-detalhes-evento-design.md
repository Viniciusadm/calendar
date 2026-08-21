# Sheet de detalhes do evento: tirar o "Fechar" do corpo e enriquecer

Data: 2026-08-21
Repos: `manage/calendar` (app Android) e `manage/back` (API Laravel)

## Problema

O sheet que abre ao tocar num evento (`EntrySheet.kt`) mostra três coisas:
título, uma linha de meta, e uma lista vertical de ações de texto. Isso gera
dois defeitos:

1. **"Fechar" é um item de lista.** Ele fica no mesmo bloco, com o mesmo peso
   visual, das ações reais — inclusive vizinho de "Remover a série inteira".
   Uma ação inócua ocupando uma linha de 48dp no fim de tudo, e o
   `ModalBottomSheet` já fecha por arraste no handle, toque no scrim e botão
   voltar.
2. **"Editar evento" e "Remover" leem como texto solto.** Sem hierarquia entre
   editar e apagar a série inteira, e sem o vocabulário visual do resto do app
   (que é feito de `Pill`, `CircleButton`, hairline e acento brand).

Ao mesmo tempo o sheet é pobre de informação, embora o dado já exista:

- `OccurrenceDto` (feed) já traz `description`, `priority`, `endTime`,
  `durationMinutes`, `overridden`, `completedAt`, `remindersMuted` — e
  `CalendarModel.kt:49 toEntry()` descarta tudo. Hoje o `note` só sai de
  `meta.age`, e a `description` do evento é jogada fora.
- `GET /api/calendar/events/{id}` já devolve `description`, `priority`,
  `reminders[]`, `items[]` (checklist) e `recurrenceEndsAt`
  (`CalendarPresenter::event`). O app só chama isso para abrir o editor.
- **Próximas ocorrências é o único item que não existe em lugar nenhum.** O
  `Recurrence.kt` do app só faz parse/emit de RRULE, não expande.

## Decisões tomadas

| Questão | Decisão |
|---|---|
| Como fechar | Nada no corpo. Handle, scrim e voltar — os três já funcionam |
| Ações | `Pill` numa linha, com assinatura de cor para distinguir comando de seleção |
| Onde ficam as ações | Logo após o cabeçalho, **acima** dos blocos de informação |
| Blocos | Próximas ocorrências, descrição/prioridade/horário, lembretes/checklist, estado da ocorrência |

A ordem das ações é contra-intuitiva de propósito. O detalhe chega por rede
depois do sheet abrir, então os blocos crescem. Título e meta vêm do feed e
nunca mudam; ancorar os pills logo abaixo deles é a única ordem em que o alvo
de toque não se move quando a resposta chega. Reservar altura no rodapé exigiria
chutar quanto espaço as próximas, os lembretes e os itens vão ocupar — o mesmo
tipo de altura fixa adivinhada que quebrou a linha do tempo do `DayAgenda`.

## Backend (`manage/back`)

### Novo: `app/Calendar/Support/SeriesPreview.php`

```php
public function next(CalendarEvent $event, int $take = 3, int $horizonDays = 365): array
```

- Evento não recorrente devolve `[]`.
- Expande com `RecurrenceExpander::between($rule, $event->anchor(), $from, $to)`,
  onde `$from` é amanhã e `$to` é `$from + $horizonDays`. O `between` já vem
  capado em `config('calendar.window.max_occurrences_per_series')` (500).
- Carrega os `CalendarEventOccurrence` **desse** evento, pula os cancelados,
  aplica `movedDateString()`, reordena pela data efetiva e corta em `$take`.
- Espelha de propósito a lógica de `StoredEventExpander::seriesDates`, escopada
  a um evento, para a prévia nunca discordar do feed.

Não reusar `StoredEventExpander::expand()` é deliberado: ele varre todos os
eventos candidatos da janela, e expandiria o calendário inteiro por um ano para
preencher três linhas.

Cada item do array:

```php
['seriesDate' => '2026-09-02', 'date' => '2026-09-02', 'time' => '14:30', 'allDay' => false]
```

`seriesDate` e `date` divergem quando a ocorrência foi movida.

### `app/Services/Calendar/CalendarPresenter.php`

```php
public function event(CalendarEvent $event, array $nextOccurrences = []): array
```

Novo campo `'nextOccurrences' => $nextOccurrences` no retorno. **Só o `show`
preenche.** `store` e `update` chamam o mesmo presenter e não faz sentido pagar
a expansão em cada escrita.

### `app/Http/Controllers/Calendar/CalendarEventController.php`

`show()` injeta `SeriesPreview` e passa o resultado ao presenter. Nada mais muda
— o 404 de `requires_code` sem código validado continua como está.

### Testes

`tests/` (PHPUnit, `phpunit.xml` na raiz). Casos para `SeriesPreview`:

- evento não recorrente → `[]`
- semanal simples → as 3 próximas datas, todas depois de hoje
- ocorrência cancelada no meio → pulada, e a lista ainda devolve 3
- ocorrência movida → aparece na data movida e reordenada
- `recurrence_ends_at` antes do horizonte → devolve menos que `$take`
- série que termina no passado → `[]`

## App (`manage/calendar`)

### `core/net/Dto.kt`

- `EventDto` ganha `nextOccurrences: List<SeriesDateDto> = emptyList()`,
  `items: List<EventItemDto> = emptyList()`, `recurrenceEndsAt: String? = null`.
- Novo `SeriesDateDto(seriesDate, date, time, allDay)`.
- Novo `EventItemDto(id, title, durationMinutes, position)`.

### `feature/calendar/CalendarModel.kt`

`CalendarEntry` para de descartar o que o feed já manda:
`description`, `priority`, `endTime`, `durationMinutes`, `completedAt`,
`overridden`, `remindersMuted`.

`note` continua vindo de `meta.age` (é o que dá "faz N anos" no aniversário);
`description` passa a ser um campo próprio, separado.

### Novo: `feature/calendar/EntryDetail.kt`

O estado do detalhe buscado, e o mapeamento de `EventDto` para o que o sheet
desenha (lembretes formatados, itens, próximas com rótulo relativo). Mantém
`EntrySheet.kt` só com layout.

### `feature/calendar/CalendarViewModel.kt`

`focus(entry)` deixa de ser só um `_state.update`:

- sempre define `focused = entry` na hora (o sheet abre imediato);
- se `entry.eventId != null`, lança `api.event(id)` num novo campo de estado
  `focusedDetail: DetailState` — `Loading`, `Ready(EntryDetail)` ou `Absent`;
- `focus(null)` limpa os dois;
- resultado que chega para uma entrada que não é mais a focada é descartado
  (comparar `eventId` antes de aplicar).

**Degrada em silêncio.** Falha de rede, e o 404 de evento protegido, caem em
`Absent`: o sheet fica na versão enxuta, sem texto de erro. O usuário protegido
já vê o fluxo de código em outro lugar; erro aqui seria ruído.

**Duas entradas nunca buscam detalhe:** `Occurrence::projected()` deixa
`eventId` nulo, então aniversário, episódio e pergunta não têm
`GET /events/{id}`. Para elas o sheet mostra só cabeçalho e estado — e, como
não são editáveis, **nenhum pill**. Vira um sheet puramente informativo, e isso
é o correto.

### `design/Theme.kt`

`ChronicleColors` ganha `destructive`. Hoje o esquema só expõe `ground`,
`surface`, `ink`, `slate`, `hairline`, `muted`, `brand`, `brandSoft` e
`isDark` — não há cor de perigo, e é por isso que "Remover" é azul brand no
`EntrySheet` atual, indistinguível de "Editar". A paleta já tem
`TokenDestructive` (`0xFFE62B34`) para cor de categoria; o tema passa a expor
uma cor semântica própria: `TokenDestructive` no claro, e uma variante
clareada no escuro, porque `E62B34` sobre o `GroundDark` (`0A131A`) fica
pesado demais.

Sem isso o mapeamento de cor dos pills não fecha: `concluir` e `remover`
cairiam os dois em brand, dando a mesma cor para ação benigna e destrutiva.

### `design/components/Pill.kt`

Novo parâmetro para distinguir comando de seleção. O `Pill` atual é slate com
borda hairline quando não selecionado — um pill de ação com `selected = false`
leria como "opção não escolhida". O pill de comando leva rótulo e borda na cor
da ação (`ink`, `brand` ou `destructive`) e **nunca** o preenchimento
`brandSoft`, que fica reservado para seleção. Parâmetro no `Pill` existente, e
não componente novo, porque a forma é a mesma.

### `feature/calendar/Recurrence.kt`

Dois helpers públicos novos, ao lado do `dayLabel` que já existe:

- `shortDate(date): String` → `"ter, 2 set"`, com o ano anexado quando não for
  o ano corrente. Usa o `monthShort` e o `weekdayShort` que já existem no
  arquivo (`monthShort` é privado hoje; continua privado, só o helper é público).
- `relativeDays(date): String` → `1` vira `"amanhã"`, `n` vira `"em N dias"`.

### `MainActivity.kt`

O call site do `EntrySheet` (linha 308) passa o `focusedDetail` novo junto com
a entrada. O `onDismiss` sai da assinatura do sheet — não há mais nada no corpo
que dispense. O `onDismissRequest` do `ModalBottomSheet` continua como está.

### `feature/calendar/EntrySheet.kt`

Reescrito. Ordem:

```
handle                          (do ModalBottomSheet)

Título                          SheetTitle, ink
linha de meta                   EntryMeta slate, com o token de prioridade
                                tingido via buildAnnotatedString
descrição                       EntryTitle ink, some quando vazia

[bloco de estado]               linhas curtas em slate

pills de ação                   Row com Arrangement.spacedBy(Space.sm) e
                                horizontalScroll, como o ScrollingPills do editor

Hairline  ou  LinearProgressIndicator(Stroke.hairline) enquanto carrega

PRÓXIMAS                        Eyebrow slate
● ter, 2 set        em 12 dias  dot Stroke.node na cor da entrada; data à
                                esquerda, relativo à direita, Spacer(weight(1f))
repete até 20 de dez            recurrenceEndsAt, como nota em slate

LEMBRETE                        Eyebrow
15 min antes

CHECKLIST                       Eyebrow
· Levar carteirinha
```

**Linha de meta**, nessa ordem, juntada com `" · "`:
faixa de horário (ou `"dia inteiro"`), categoria, resumo da recorrência,
prioridade, `note`. Faixa é `"14:30 – 15:30"` quando há `endTime` diferente do
início; senão só o início. Prioridade aparece quando não é `none`, com o rótulo
em português (`baixa`/`média`/`alta`), e **só `alta` é tingida de brand** — o
resto fica na cor da linha.

**Bloco de estado**, uma linha por condição verdadeira:
`completed` → `"concluído em {shortDate}"`, ou só `"concluído"` sem
`completedAt`; `overridden` → `"esta ocorrência foi editada"`;
`remindersMuted` → `"lembretes silenciados nesta ocorrência"`.

**Pills**, na ordem: `editar` (ink); `concluir`/`reabrir` (brand) quando é
tarefa; `remover` (destructive); `remover série` (destructive) quando é
recorrente. Em recorrente o `remover` remove só a ocorrência, como hoje. A cor
é o que separa os três papéis: neutro para editar, brand para a ação positiva,
destructive para o que apaga.

**Lembretes**: os `active` do `EventDto.reminders`. Rótulo por
`reminderChoices` (`EventDraft.kt:11`) quando `minutesBefore` casa exato;
senão derivado (`"N min antes"`, `"N h antes"`, `"N dias antes"`). O campo é
livre no banco, então não dá para depender só da lista de escolhas do editor.

**Checklist é somente leitura.** No `presenter->item()` o `done` passa por
`array_filter` e o `event()` não passa esse argumento — o payload não tem
estado de concluído por ocorrência. Lista com marcador, não checkbox: existe
`POST /items/{item}/check`, mas ligar interação é escopo novo, e checkbox sem
estado real seria mentira visual. `durationMinutes` do item, quando houver,
aparece à direita em slate.

**Todo bloco vazio desaparece inteiro**, header Eyebrow incluído. Sem
recorrência não há PRÓXIMAS; sem lembrete ativo não há LEMBRETE; sem item não
há CHECKLIST. Nenhum `Spacer` fica órfão — foi exatamente o defeito que a
limpeza de espaçamento do commit anterior corrigiu.

### Testes

`app/src/test/` (JUnit, sem Compose). O que dá para testar sem instrumentação:

- `shortDate` e `relativeDays`, incluindo virada de ano e `"amanhã"`
- a montagem da linha de meta: cada combinação de horário, categoria,
  recorrência, prioridade e note
- o bloco de estado: cada condição, e todas juntas
- o rótulo de lembrete: valores exatos da lista e valores arbitrários
- `EntryDetail` a partir de um `EventDto`: filtra lembrete inativo, ordena item
  por `position`, e o caso de payload vazio

O layout em si é verificado no aparelho.

## Verificação ponta a ponta

Backend:

```
cd ../back && ./vendor/bin/phpunit --filter SeriesPreview
```

App:

```
./gradlew :app:testDebugUnitTest
./gradlew :app:installDebug
```

No aparelho (há um device conectado, `RXCX901Y6JA`):

1. Evento recorrente com hora, categoria, prioridade alta, lembrete e descrição
   — todos os blocos presentes, PRÓXIMAS com 3 datas e "em N dias" corretos.
2. Evento simples sem nada além de título — só título e pills, nenhum header
   Eyebrow órfão, nenhum espaço sobrando.
3. Tarefa concluída — pill `reabrir`, bloco de estado com "concluído em".
4. Ocorrência editada e movida de uma série — "esta ocorrência foi editada", e
   a data movida aparecendo na posição cronológica certa em PRÓXIMAS.
5. Aniversário e episódio — sheet informativo, sem pill nenhum, sem tentativa
   de busca de detalhe.
6. Evento protegido sem código validado — cai na versão enxuta, sem erro.
7. Modo avião: abre o sheet, os pills ficam no lugar, os blocos nunca chegam e
   nada salta.
8. Série que termina no mês que vem — PRÓXIMAS mostra menos de 3 e a nota
   "repete até {data}".
