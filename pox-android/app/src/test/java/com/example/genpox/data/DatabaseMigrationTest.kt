package com.example.genpox.data

import org.junit.Assert.assertNotNull
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testPoxDatabaseClassExists() {
        val dbClass = PoxDatabase::class.java
        assertNotNull(dbClass)
    }
}
