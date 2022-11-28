package dev.storozhenko.familybot.suits

import org.junit.jupiter.api.Test
import dev.storozhenko.familybot.infrastructure.TestSender

abstract class ExecutorTest : FamilybotApplicationTest() {

    protected val testSender = TestSender()
    protected val sender = TestSender().sender

    @Test
    fun runPriorityTest() {
        priorityTest()
    }

    @Test
    fun runCanExecuteTest() {
        canExecuteTest()
    }

    @Test
    fun runExecuteTest() {
        executeTest()
    }

    abstract fun priorityTest()

    abstract fun canExecuteTest()

    abstract fun executeTest()
}
