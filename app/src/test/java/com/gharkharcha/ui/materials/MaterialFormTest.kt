package com.gharkharcha.ui.materials

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MaterialFormTest {
    @Test fun `itemised total is qty times rate`() {
        val f = MaterialForm(qty = "2.5", rate = "420")
        assertEquals(1_050_00L, f.autoTotalPaise)
        assertEquals(1_050_00L, f.totalPaise)
    }

    @Test fun `missing qty or rate gives no total`() {
        assertNull(MaterialForm(qty = "", rate = "420").totalPaise)
        assertNull(MaterialForm(qty = "3", rate = "abc").totalPaise)
    }

    @Test fun `typed total overrides qty times rate`() {
        val f = MaterialForm(qty = "10", rate = "18.75", total = "180", totalOverridden = true)
        assertEquals(187_50L, f.autoTotalPaise)
        assertEquals(180_00L, f.totalPaise)
    }

    @Test fun `total only entry uses the typed amount`() {
        val f = MaterialForm(itemised = false, item = "Raw material", total = "4,500")
        assertEquals(4_500_00L, f.totalPaise)
    }
}
