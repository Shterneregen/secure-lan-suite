package com.shterneregen.securelan.filetransfer.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.IOException

class FileReceiveBudgetTest {
    @Test fun completedFilesKeepBudgetWhileAbortedFilesReleaseIt() {
        val budget = FileReceiveBudget(10, 2)
        budget.reserve(8).close()
        budget.reserve(8).use { it.commit() }
        assertThrows(IOException::class.java) { budget.reserve(3) }
        budget.reserve(2).use { it.commit() }
        assertThrows(IOException::class.java) { budget.reserve(0) }
    }

    @Test fun emptyFilesCannotBypassFileCountAndClosingIsIdempotent() {
        val budget = FileReceiveBudget(1, 1)
        val aborted = budget.reserve(0)
        aborted.close(); aborted.close()
        budget.reserve(0).use { it.commit() }
        assertThrows(IOException::class.java) { budget.reserve(0) }
    }
}
