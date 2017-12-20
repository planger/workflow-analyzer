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
			/* In a valid workflow diagram, we'll find the decision node by following any path backwards.
 			 * We might visit other merge nodes before, but we'll jump over them to reach the decision
 			 * node that corresponds to this decision node (see cycleSavePredecessorSequenceJumpingOverFullDecisionGroups). */
			val predecessorSequence = predecessorsSkippingDecisionGroups(this.incoming.firstOrNull())
			return predecessorSequence.firstOrNull() { it is Decision } as Decision?
		}

	private fun predecessorsSkippingDecisionGroups(node: Node?): Sequence<Node> {
		// remember and filter seen nodes to avoid infinite sequence for workflow cycles
		val seenNodes: MutableSet<Node> = mutableSetOf()
		return filterablePredecessorSequenceSkippingDecisionGroups(node) { seenNodes.add(it) }
	}

	private fun filterablePredecessorSequenceSkippingDecisionGroups(node: Node?, predicate: (Node) -> Boolean): Sequence<Node> {
		return generateSequence(node) {
			val filteredNode = it.takeIf(predicate)
			when (filteredNode) {
				is Merge -> filteredNode.decision?.incoming?.firstOrNull()
				else -> {
					filteredNode?.incoming?.firstOrNull()
				}
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
		get() {
			return directlyPrecedingDecision
		}
}

data class Performer(val name: String)

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