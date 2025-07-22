package com.squareup.workflow1.traceviewer.model

/**
 * Since the logic of Workflow is hierarchical (where each workflow may have parent workflows and/or
 * children workflows, a tree structure is most appropriate for representing the data rather than
 * using flat data structures like an array.
 *
 * TBD what more metadata should be involved with each node, e.g. (props, states, # of render passes)
 */
internal data class Node(
  val name: String,
  val id: String,
  val parent: String,
  val parentId: String,
  val props: String,
  val state: String,
  val rendering: String = "",
  @Transient val children: LinkedHashMap<String, Node> = LinkedHashMap()
) {

  override fun toString(): String {
    return "Node(name='$name', parent='$parent', children=$children)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Node) return false
    return this.id == other.id
  }

  override fun hashCode(): Int {
    return id.hashCode()
  }

  companion object {
    fun getNodeFields(): List<String> {
      return listOf("props", "state", "rendering")
    }

    fun getNodeData(node: Node, field: String): String {
      return when (field.lowercase()) {
        "props" -> node.props
        "state" -> node.state
        "rendering" -> node.rendering
        else -> throw IllegalArgumentException("Unknown field: $field")
      }
    }
  }
}

internal fun Node.addChild(child: Node): Node {
  return copy(children = LinkedHashMap(this.children.plus(child.id to child)))
}

internal fun Node.replaceChild(child: Node): Node {
  return addChild(child)
}
