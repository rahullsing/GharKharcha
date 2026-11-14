package com.gharkharcha.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactIntentsTest {
    @Test fun `whatsapp numbers get the Indian country code`() {
        assertEquals("919876543210", ContactIntents.whatsAppNumber("98765 43210"))
        assertEquals("919876543210", ContactIntents.whatsAppNumber("09876543210"))
        assertEquals("919876543210", ContactIntents.whatsAppNumber("+91 98765-43210"))
        assertEquals("14155550100", ContactIntents.whatsAppNumber("+1 415 555 0100"))
        assertNull(ContactIntents.whatsAppNumber("12345"))
        assertNull(ContactIntents.whatsAppNumber(""))
    }
}
