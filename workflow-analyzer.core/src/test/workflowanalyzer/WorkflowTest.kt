package workflowanalyzer.tests

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import workflowanalyzer.*
import org.junit.Assert

class WorkflowTest {

	@Test fun testIncomingAndOutgoingEdges() {
		// setup
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val fork = ForkOrJoin()
		val join = ForkOrJoin()

		task1.connectTo(fork).connectTo(task2, task3).connectTo(join).connectTo(task4)

		// assert outgoing
		task1.assertOutgoingOnly(fork)
		fork.assertOutgoingOnly(task2, task3)
		arrayOf(task2, task3).forEach { it.assertOutgoingOnly(join) }
		join.assertOutgoingOnly(task4)

		// assert incoming
		fork.assertIncomingOnly(task1)
		arrayOf(task2, task3).forEach { it.assertIncomingOnly(fork) }
		join.assertIncomingOnly(task2, task3)
		task4.assertIncomingOnly(join)
	}

	private fun Node.assertOutgoingOnly(vararg nodes: Node) = assertListContainsExactly(nodes, this.outgoing)
	private fun Node.assertIncomingOnly(vararg nodes: Node) = assertListContainsExactly(nodes, this.incoming)

	private fun assertListContainsExactly(nodes: Array<out Node>, containedNodes: Collection<Node>) {
		assertTrue(containedNodes.containsAll(nodes.toList()))
		assertTrue(containedNodes.none { !nodes.contains(it) })
	}

	@Test fun testObtainingDirectDecisionFromMerge() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val decision = Decision()
		val merge = Merge()

		task1.connectTo(decision).connectTo(task2, task3).connectTo(merge).connectTo(task4)

		assertSame(decision, merge.decision)
	}

	@Test fun testObtainingDecisionFromMergeWithADecisionInbetween() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val decision = Decision()
		val merge = Merge()
		val decision2 = Decision()
		val merge2 = Merge()

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
				.connectTo(merge2).connectTo(merge)
		task3.connectTo(merge).connectTo(task6)

		assertSame(decision, merge.decision)
		assertSame(decision2, merge2.decision)
	}

	@Test fun testObtainingDecisionFromMergeDoesntRunForeverWithCycles() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val decision = Decision()
		val merge = Merge()

		task3.connectTo(task2)
		task1.connectTo(decision).connectTo(task2).connectTo(task3).connectTo(merge)

		assertNull(merge.decision)
	}

	@Test fun testDirectlyPreceedingDecision() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val decision = Decision()
		val merge = Merge()
		val decision2 = Decision()
		val merge2 = Merge()

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
				.connectTo(merge2).connectTo(merge)
		task3.connectTo(merge).connectTo(task6)

		assertSame(decision, task2.directlyPrecedingDecision)
		assertSame(decision, task3.directlyPrecedingDecision)
		assertSame(decision2, task4.directlyPrecedingDecision)
		assertSame(decision2, task5.directlyPrecedingDecision)
		assertNull(task6.directlyPrecedingDecision)
	}

	@Test fun testProbabilityOfNodeWithoutPreceedingDecision() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		task1.connectTo(task2)

		assertEquals(1f, task1.probability)
		assertEquals(1f, task2.probability)
	}

	@Test fun testProbabilityOfNodeWithOnePreceedingDecision() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val decision = Decision()
		val merge = Merge()

		task1.connectTo(decision).connectTo(task2, task3, task4).connectTo(merge).connectTo(task5)
		decision.probabilities[task2] = 0.2f
		decision.probabilities[task3] = 0.3f
		decision.probabilities[task4] = 0.5f

		assertEquals(1f, task1.probability)
		assertEquals(0.2f, task2.probability)
		assertEquals(0.3f, task3.probability)
		assertEquals(0.5f, task4.probability)
		assertEquals(1f, task5.probability)
	}

	@Test fun testCombinedProbabilitiesWithMultipleDecisions() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val decision = Decision()
		val merge = Merge()
		val decision2 = Decision()
		val merge2 = Merge()

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
				.connectTo(merge2).connectTo(merge)
		task3.connectTo(merge).connectTo(task6)
		decision.probabilities[task2] = 0.5f
		decision.probabilities[task3] = 0.5f
		decision2.probabilities[task4] = 0.2f
		decision2.probabilities[task5] = 0.8f

		assertEquals(1f, task1.probability)
		assertEquals(0.5f, task2.probability)
		assertEquals(0.5f, task3.probability)
		assertEquals(0.5f * 0.2f, task4.probability)
		assertEquals(0.5f * 0.8f, task5.probability)
		assertEquals(1f, task6.probability)
	}

}
