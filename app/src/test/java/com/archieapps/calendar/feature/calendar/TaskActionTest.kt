package com.archieapps.calendar.feature.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskActionTest {
    @Test
    fun `an explicit label wins over anything derived`() {
        val action = TaskAction(ActionKind.Link, "https://www.nubank.com.br/pagar", "Pagar boleto")

        assertEquals("Pagar boleto", action.caption())
    }

    @Test
    fun `a link falls back to the host without the www`() {
        val action = TaskAction(ActionKind.Link, "https://www.nubank.com.br/pagar", null)

        assertEquals("nubank.com.br", action.caption())
    }

    @Test
    fun `a blank label is treated as absent`() {
        val action = TaskAction(ActionKind.Link, "https://detran.sp.gov.br", "   ")

        assertEquals("detran.sp.gov.br", action.caption())
    }

    @Test
    fun `a custom scheme still yields the host`() {
        val action = TaskAction(ActionKind.Link, "whatsapp://send?phone=5511999999999", null)

        assertEquals("send", action.caption())
    }

    @Test
    fun `an app shows the resolved name`() {
        val action = TaskAction(ActionKind.App, "com.nu.production", null)

        assertEquals("Nubank", action.caption(appName = "Nubank"))
    }

    @Test
    fun `an app with no resolved name falls back to the package`() {
        val action = TaskAction(ActionKind.App, "com.nu.production", null)

        assertEquals("com.nu.production", action.caption())
    }
}
