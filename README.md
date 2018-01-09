# Workflow Analyzer

Kotlin framework to represent simple workflows and compute probabilities, execution duration, etc.

## Usage

### Modeling workflows

A `Workflow` is a set of nodes, which may either be tasks, decisions, or forks. A `Decision` represents conditional branches in a workflow, whereas a probability of taking this branch can be specified. Decisions are always merged back together by a `Merge` node. Forks denote parallel branches and are joined back together by a join node. Fork and join nodes are both represented by the same class `ForkOrJoin`.

A `Task` has a duration indicating the effort to complete the task, as well as a `Performer` which is an actor or component that performs the task.

Consider for instance the following workflow, with four tasks, `Task1`, `Task2`, `Task3`, `Task4`, whereas `Task2` and `Task3` run in parallel.

```
          |--> Task2 -->|
Task1 --> |             |--> Task4
          |--> Task3 -->|
```

Let's say all tasks are performed by a performer called `A` and the durations of those tasks is `1` for `Task1`, `2` for `Task2`, etc. This workflow is represented as follows in the Workflow Analyzer.

```kotlin
val task1 = Task("Task1", Performer("A"), 1)
val task2 = Task("Task2", Performer("A"), 2)
val task3 = Task("Task3", Performer("A"), 3)
val task4 = Task("Task4", Performer("A"), 4)
val fork = ForkOrJoin("fork")
val join = ForkOrJoin("join")

task1.connectTo(fork).connectTo(task2, task3).connectTo(join).connectTo(task4)
```

Conditional branches instead of parallel branches are defined using decisions. For each branch, a probability of going this branch can be specified, as shown below.

```kotlin
val task1 = Task("Task1", Performer("A"), 1)
val task2 = Task("Task2", Performer("A"), 2)
val task3 = Task("Task3", Performer("A"), 3)
val task4 = Task("Task4", Performer("A"), 4)
val decision = Decision("decision")
val merge = Merge("merge")

task1.connectTo(decision).connectTo(task2, task3).connectTo(merge).connectTo(task4)
decision.probabilities[task2] = 0.2f
decision.probabilities[task3] = 0.8f
```

Of course, decisions and parallel branches can be arbitrarily combined. However, each decision or fork must be closed by a merge or a join node, respectively.

```kotlin
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
val fork = ForkOrJoin("fork")
val join = ForkOrJoin("join")

task1.connectTo(decision).connectTo(task2, task3)
task2.connectTo(decision2).connectTo(task4, task5).connectTo(merge2).connectTo(merge)
task3.connectTo(fork).connectTo(task6, task7).connectTo(join).connectTo(merge).connectTo(task8)

decision.probabilities[task2] = 0.4f
decision.probabilities[task3] = 0.6f
decision2.probabilities[task4] = 0.2f
decision2.probabilities[task5] = 0.8f
```

### Analyzing workflows

Once you specified a workflow, there are a couple of metrics you can compute from them:
- Overall probability of a node to be executed combining all probabilities of decisions before the node (see `Node.overAllProbability`)
- Probability of a node in the current branch (see `Node.probabilityInBranch`)
- Point in time of the execution of a node which is a sum of all nodes that need to be performed before the node (see `Node.executionPointInTime`). The result is a `PointInTime`, which denotes an earliest, latest, and average point in time, taking into account the conditional branches of the workflow.
  - The earliest point in time is the path with the minimal durations from the first node to the node in question. This is the minimal duration before the node can ever be executed (best case).
  - The latest point in time is the path with the maximal durations from the first node to the node in question. This is the maximal duration before the node is executed (worst case).
  - The average point in time takes the probabilities of decisions into account and specifies the average case for executing the workflow.

### Examples

Please have a look at the [workflow tests](./workflow-analyzer.core/src/test/workflowanalyzer/WorkflowTest.kt) for more examples.
  
### Limitations

Currently, no loops are supported. Each decision and fork must be terminated by a merge and a join. We haven't done any performance optimization, so performance for large workflows may be suboptimal. This is especially true as intermedeate results, that is, the propabilities and points in time for single nodes are computed over and over again when asked for each node in the workflow.

Use this framework at your own risk. This project is mostly intended to be used for demo purposes.

## Information for developers and contributors

Contributions and feedback is always very welcome. Feel free to open issues or pull requests.

### Building the workflow analyzer

This project uses maven. Run `mvn clean install` either in the directory `workflow-analyzer.core` to build for the use on a JVM (Kotlin or Java or any other language that runs on the JVM) or in the directory `workflow-analyzer.js` to build the framework for JavaScript. This will create a JAR or JS file to be used in your application.
