package com.example.gliphlights.editor.model

data class NodeState(
    val channelIndex: Int,
    val isOn: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

data class AnimationModel(
    val nodeStates: List<NodeState>
) {
    val activeChannels: Set<Int>
        get() = nodeStates.filter { it.isOn }.map { it.channelIndex }.toSet()

    val activeCount: Int
        get() = nodeStates.count { it.isOn }

    companion object {
        fun empty() = AnimationModel(emptyList())

        fun fromNodeStates(states: Map<Int, Boolean>, timestamp: Long = System.currentTimeMillis()): AnimationModel {
            val nodeStates = states.map { (channel, isOn) ->
                NodeState(channelIndex = channel, isOn = isOn, timestamp = timestamp)
            }
            return AnimationModel(nodeStates)
        }
    }
}
