package workflowanalyzer.tests

import org.junit.Test
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import workflowanalyzer.*
import org.junit.Assert

class WorkflowTest {

	@Test
	fun testIncomingAndOutgoingEdges() {
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

	@Test
	fun testObtainingDirectDecisionFromMerge() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val decision = Decision()
		val merge = Merge()

		task1.connectTo(decision).connectTo(task2, task3).connectTo(merge).connectTo(task4)

		assertSame(decision, merge.decision)
	}

	@Test
	fun testObtainingDecisionFromMergeWithADecisionInbetween() {
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

	@Test
	fun testObtainingDecisionFromMergeDoesntRunForeverWithCycles() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val decision = Decision()
		val merge = Merge()

		task3.connectTo(task2)
		task1.connectTo(decision).connectTo(task2).connectTo(task3).connectTo(merge)

		assertNull(merge.decision)
	}

	@Test
	fun testDirectlyPreceedingDecision() {
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

	@Test
	fun testProbabilityOfNodeWithoutPreceedingDecision() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		task1.connectTo(task2)

		assertEquals(1f, task1.overAllProbability)
		assertEquals(1f, task2.overAllProbability)
	}

	@Test
	fun testProbabilityOfNodeWithOnePreceedingDecision() {
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

		assertEquals(1f, task1.probabilityInBranch)
		assertEquals(0.2f, task2.probabilityInBranch)
		assertEquals(0.3f, task3.probabilityInBranch)
		assertEquals(0.5f, task4.probabilityInBranch)
		assertEquals(1f, task5.probabilityInBranch)

		assertEquals(1f, task1.overAllProbability)
		assertEquals(0.2f, task2.overAllProbability)
		assertEquals(0.3f, task3.overAllProbability)
		assertEquals(0.5f, task4.overAllProbability)
		assertEquals(1f, task5.overAllProbability)
	}

	@Test
	fun testCombinedProbabilitiesWithMultipleDecisions() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val task7 = Task("Task7", Performer("A"), 7)
		val decision = Decision()
		val merge = Merge()
		val decision2 = Decision()
		val merge2 = Merge()

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
		task5.connectTo(task6)
		arrayOf(task4, task5).connectTo(merge2).connectTo(merge)
		task3.connectTo(merge).connectTo(task7)
		decision.probabilities[task2] = 0.5f
		decision.probabilities[task3] = 0.5f
		decision2.probabilities[task4] = 0.2f
		decision2.probabilities[task5] = 0.8f

		assertEquals(1f, task1.probabilityInBranch)
		assertEquals(0.5f, task2.probabilityInBranch)
		assertEquals(0.5f, task3.probabilityInBranch)
		assertEquals(0.2f, task4.probabilityInBranch)
		assertEquals(0.8f, task5.probabilityInBranch)
		assertEquals(0.8f, task6.probabilityInBranch)
		assertEquals(1f, task7.probabilityInBranch)

		assertEquals(1f, task1.overAllProbability)
		assertEquals(0.5f, task2.overAllProbability)
		assertEquals(0.5f, task3.overAllProbability)
		assertEquals(0.5f * 0.2f, task4.overAllProbability)
		assertEquals(0.5f * 0.8f, task5.overAllProbability)
		assertEquals(0.5f * 0.8f, task6.overAllProbability)
		assertEquals(1f, task7.overAllProbability)
	}

	@Test
	fun testExecutionPointInTimeWithSimpleSequence() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)

		task1.connectTo(task2).connectTo(task3).connectTo(task4)

		assertEquals(PointInTime(0, 0, 0f), task1.executionPointInTime)
		assertEquals(PointInTime(1, 1, 1f), task2.executionPointInTime)
		assertEquals(PointInTime(3, 3, 3f), task3.executionPointInTime)
		assertEquals(PointInTime(6, 6, 6f), task4.executionPointInTime)
	}


	@Test
	fun testExecutionPointInTimeWithParallelBranches() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val fork = ForkOrJoin()
		val join = ForkOrJoin()

		task1.connectTo(fork).connectTo(task2, task3).connectTo(join).connectTo(task4)

		assertEquals(PointInTime(0, 0, 0f), task1.executionPointInTime)
		// both parallel tasks can start directly after task1
		assertEquals(PointInTime(1, 1, 1f), task2.executionPointInTime)
		assertEquals(PointInTime(1, 1, 1f), task3.executionPointInTime)
		// task4 can only be executed after both task2 and task3 have been executed
		// so the point in time is the maximum among all parallel branches (task3 in this case)
		assertEquals(PointInTime(4, 4, 4f), task4.executionPointInTime)
	}

	@Test
	fun testExecutionPointInTimeWithADecision() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 5)
		val decision = Decision()
		val merge = Merge()

		task1.connectTo(decision).connectTo(task2, task3).connectTo(merge).connectTo(task4)
		decision.probabilities[task2] = 0.2f
		decision.probabilities[task3] = 0.8f

		assertEquals(PointInTime(0, 0, 0f), task1.executionPointInTime)
		// both conditional tasks can start directly after task1
		assertEquals(PointInTime(1, 1, 1f), task2.executionPointInTime)
		assertEquals(PointInTime(1, 1, 1f), task3.executionPointInTime)
		// task4 can only be executed after either task2 and task3 have been executed
		// so the point in time is atEarliest after task2, i.e. 3,
		// atLatest after task3, i.e. 4
		// and on average after 3.8: 1 (task1) + 0.2*2 (task2 in 20 % of the cases) + 0.8*3 (task3 in 80 % of the cases)
		assertEquals(PointInTime(3, 4, 3.8000002f), task4.executionPointInTime)
	}

	@Test
	fun testExecutionPointInTimeWithMultipleDecisions() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val task7 = Task("Task7", Performer("A"), 7)
		val decision = Decision()
		val merge = Merge()
		val decision2 = Decision()
		val merge2 = Merge()

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
				.connectTo(merge2).connectTo(task6).connectTo(merge)
		task3.connectTo(merge).connectTo(task7)

		decision.probabilities[task2] = 0.2f
		decision.probabilities[task3] = 0.8f
		decision2.probabilities[task4] = 0.7f
		decision2.probabilities[task5] = 0.3f

		assertEquals(PointInTime(0, 0, 0f), task1.executionPointInTime)
		// task2 or task3 can start directly after task1
		assertEquals(PointInTime(1, 1, 1f), task2.executionPointInTime)
		assertEquals(PointInTime(1, 1, 1f), task3.executionPointInTime)
		// task4 or task5 can only be executed after task2
		assertEquals(PointInTime(3, 3, 3f), task4.executionPointInTime)
		assertEquals(PointInTime(3, 3, 3f), task5.executionPointInTime)
		// task6 can only be executed after either task4 or task5 have been executed
		// so the point in time is atEarliest after task4, i.e. 3 + 4 = 7,
		// atLatest after task5, i.e. 3 + 5 = 8
		// and on average after 7.3: 1 + 2 + 0.7*4 (task4 in 70 % of the cases) + 0.3*5 (task5 in 30 % of the cases)
		assertEquals(PointInTime(7, 8, 7.3f), task6.executionPointInTime)
		// task7 can only be executed after either task3 or task6 has been executed
		// so the point in time is atEarliest after task 3, i.e. 1 + 3 = 4,
		// atLatest after task6, i.e. 8 + 6 = 14
		assertEquals(PointInTime(4, 14, 5.86f), task7.executionPointInTime)
	}

	@Test
	fun testExecutionPointInTimeWithMultipleDecisionsAndParallelBranches() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val task7 = Task("Task7", Performer("A"), 7)
		val task8 = Task("Task8", Performer("A"), 8)
		val decision = Decision("decision")
		val merge = Merge("merge")
		val decision2 = Decision("decision2")
		val merge2 = Merge("merge2")
		val fork = ForkOrJoin("test1") // we need names to prevent fork.equals(join) in set used in cycle detection
		val join = ForkOrJoin("test2") // we need names to prevent join.equals(fork) in set used in cycle detection

		task1.connectTo(decision).connectTo(task2, task3)
		task2.connectTo(decision2).connectTo(task4, task5)
				.connectTo(merge2).connectTo(merge)
		task3.connectTo(fork).connectTo(task6, task7).connectTo(join).connectTo(merge).connectTo(task8)

		decision.probabilities[task2] = 0.4f
		decision.probabilities[task3] = 0.6f
		decision2.probabilities[task4] = 0.2f
		decision2.probabilities[task5] = 0.8f

		// task8
		// 1
		// + 0.4 * (2 + 0.2*4 + 0.8*5)
		// + 0.6 * (3  + 7)
		// = 9,72 (Average)
		// 1 + 2 + 4 = 7 (Earliest)
		// 1 + 3 + 7 = 10 (At Latest)
		assertEquals(PointInTime(7, 11, 9.72f), task8.executionPointInTime)
	}

	@Test
	fun testNodeExecutionIterator() {
		val task1 = Task("Task1", Performer("A"), 1)
		val task2 = Task("Task2", Performer("A"), 2)
		val task3 = Task("Task3", Performer("A"), 3)
		val task4 = Task("Task4", Performer("A"), 4)
		val task5 = Task("Task5", Performer("A"), 5)
		val task6 = Task("Task6", Performer("A"), 6)
		val task7 = Task("Task7", Performer("A"), 7)
		val task8 = Task("Task8", Performer("A"), 8)
		val forkA = ForkOrJoin("forkA")
		val joinA = ForkOrJoin("joinA")
		val forkB = ForkOrJoin("forkB")
		val joinB = ForkOrJoin("joinB")
		val joinC = ForkOrJoin("joinC")

		task1.connectTo(forkA).connectTo(task2, task6, task7)
		task2.connectTo(forkB).connectTo(task3, task4).connectTo(joinB).connectTo(task5)
		arrayOf(task5, task6).connectTo(joinA)
		arrayOf(joinA, task7).connectTo(joinC).connectTo(task8)

		val iterator = task1.nodeExecutionIterator
		var counter = 1
		for (node in iterator) {
			when (node) {
				is Task -> {
					assertTrue(node.name.endsWith(counter.toString()))
					counter++
				}
			}
		}
	}

}
