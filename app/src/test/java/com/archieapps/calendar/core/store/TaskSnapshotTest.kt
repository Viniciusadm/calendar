package com.archieapps.calendar.core.store

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskSnapshotTest {
    private fun row(id: String, completed: Boolean = false) =
        TaskSnapshotRow(occurrenceId = id, title = "Tarefa $id", completed = completed)

    @Test
    fun `pending counts only the unchecked rows`() {
        val snapshot = TaskSnapshot(
            date = "2026-08-21",
            rows = listOf(row("a"), row("b", completed = true), row("c")),
        )

        assertEquals(2, snapshot.pending)
    }

    @Test
    fun `an empty snapshot has nothing pending`() {
        assertEquals(0, TaskSnapshot().pending)
        assertEquals("", TaskSnapshot().date)
    }
}
