package workflowanalyzer

class Workflow(val nodes: List<Node> = listOf<Node>())

sealed class Node(val id: String?) {

	private val outgoingNodes: MutableList<Node> = mutableListOf();
	private val incomingNodes: MutableList<Node> = mutableListOf();

	val outgoing: List<Node> get() = outgoingNodes.toList()
	val incoming: List<Node> get() = incomingNodes.toList()

	fun connectTo(vararg nodes: Node): Array<out Node> {
		for (node in nodes) {
			outgoingNodes += node
			node.incomingNodes += this
		}
		return nodes
	}

	val directlyPrecedingDecision: Decision?
		get() {
			val predecessorSequence = predecessorsSkippingDecisionGroups(this.incoming.firstOrNull())
			return predecessorSequence.firstOrNull() { it is Decision } as? Decision
		}

	/**
	 * Returns a sequence of nodes following the nodes first incoming node starting from [node].
	 *
	 * If the sequence hits a merge, the next incoming node will be the incoming node of its
	 * direct decision, thus, skipping all nodes within the decision of the merge.
	 *
	 * In a valid workflow diagram, we'll find the decision node by following any path backwards.
	 * We might visit other merge nodes before, but we'll jump over them to reach the decision
	 * node that corresponds to this decision node.
	 *
	 * @param node the starting node
	 */
	private fun predecessorsSkippingDecisionGroups(node: Node?): Sequence<Node> {
		return generateUniqueNodeSequence(node) {
			when (it) {
				is Merge -> it.decision?.incoming?.firstOrNull()
				else -> it.incoming.firstOrNull()
			}
		}
	}

	/**
	 * Generates a sequence using the function [next].
	 * The sequence ends as soon as [next] returns the same node twice in the entire sequence so far
	 * or if [next] doesn't provide a value anymore.
	 *
	 * @param node the starting node
	 * @param next the function to determine the next node in the sequence
	 */
	private fun generateUniqueNodeSequence(node: Node?, next: (Node) -> Node?): Sequence<Node> {
		val seenNodes: MutableSet<Node> = mutableSetOf()
		return generateSequence(node) {
			when (it.takeIf { seenNodes.add(it) }) {
				is Node -> next(it)
				else -> null
			}
		}
	}

	val probability: Float
		get() {
			val predecessorSequence = predecessorsSkippingDecisionGroups(this.incoming.firstOrNull())
			val result = predecessorSequence.fold(Pair(1f, this)) { previousResult, nextNode ->
				when (nextNode) {
					is Decision -> {
						val probabilitySoFar = previousResult.first
						val nextProbability = nextNode.probabilities[previousResult.second] ?: 1f
						val combinedProbability = probabilitySoFar * nextProbability
						Pair(combinedProbability, nextNode)
					}
					else -> Pair(previousResult.first, nextNode)
				}
			}
			return result.first
		}

	val executionPointInTime: PointInTime
		get() {
			val previousNode = this.incoming.firstOrNull()
			return when (previousNode) {
				is Task -> {
					val previousNodeDuration = previousNode.duration ?: 0
					val previousPointInTime = previousNode.executionPointInTime
					previousPointInTime.add(PointInTime(previousNodeDuration))
				}
				else -> {
					PointInTime()
				}
			}
		}
}

data class Task(
		val name: String,
		val performer: Performer,
		val duration: Int?
) : Node(name)

data class ForkOrJoin(
		val name: String? = null
) : Node(name)

data class Decision(
		val name: String? = null,
		val probabilities: MutableMap<Node, Float> = mutableMapOf()
) : Node(name)

data class Merge(val name: String? = null) : Node(name) {
	val decision: Decision?
		get() = directlyPrecedingDecision
}

data class Performer(val name: String)

data class PointInTime(val earliest: Int, val atLatest: Int, val onAverage: Float) {
	
	constructor() : this(0, 0, 0f)
	constructor(time: Int) : this(time, time, time.toFloat())

	fun add(other: PointInTime): PointInTime = PointInTime(
			this.earliest + other.earliest,
			this.atLatest + other.atLatest,
			this.onAverage + other.onAverage)

}

fun Array<out Node>.connectTo(vararg nodes: Node): Array<out Node> {
	for (sourceNode in this) {
		for (targetNode in nodes) {
			sourceNode.connectTo(targetNode)
		}
	}
	return nodes
}

fun main(args: Array<String>) {
}