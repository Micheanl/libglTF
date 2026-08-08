package com.micheanl.libgltf.animation

/**
 * libgltf · AnimationCondition
 *
 * @author Chen Micheanl
 * @license MIT
 * @see [Micheanl/libglTF](https://github.com/Micheanl/libglTF)
 */

data class AnimationCondition(
    val parameter: AnimationParameter,
    val comparison: AnimationComparison,
    val floatValue: Float = 0.0f,
    val intValue: Int = 0,
    val booleanValue: Boolean = false
) {
    internal fun matches(floats: FloatArray, ints: IntArray, booleans: BooleanArray): Boolean = when (parameter.type) {
        AnimationParameterType.FLOAT -> compare(floats[parameter.index], floatValue)
        AnimationParameterType.INT -> compare(ints[parameter.index], intValue)
        AnimationParameterType.BOOLEAN -> when (comparison) {
            AnimationComparison.EQUAL -> booleans[parameter.index] == booleanValue
            AnimationComparison.NOT_EQUAL -> booleans[parameter.index] != booleanValue
            else -> false
        }
    }

    private fun compare(left: Float, right: Float): Boolean = when (comparison) {
        AnimationComparison.EQUAL -> left == right
        AnimationComparison.NOT_EQUAL -> left != right
        AnimationComparison.GREATER -> left > right
        AnimationComparison.GREATER_OR_EQUAL -> left >= right
        AnimationComparison.LESS -> left < right
        AnimationComparison.LESS_OR_EQUAL -> left <= right
    }

    private fun compare(left: Int, right: Int): Boolean = when (comparison) {
        AnimationComparison.EQUAL -> left == right
        AnimationComparison.NOT_EQUAL -> left != right
        AnimationComparison.GREATER -> left > right
        AnimationComparison.GREATER_OR_EQUAL -> left >= right
        AnimationComparison.LESS -> left < right
        AnimationComparison.LESS_OR_EQUAL -> left <= right
    }

    companion object {
        fun float(parameter: AnimationParameter, comparison: AnimationComparison, value: Float): AnimationCondition {
            require(parameter.type == AnimationParameterType.FLOAT)
            return AnimationCondition(parameter, comparison, floatValue = value)
        }

        fun int(parameter: AnimationParameter, comparison: AnimationComparison, value: Int): AnimationCondition {
            require(parameter.type == AnimationParameterType.INT)
            return AnimationCondition(parameter, comparison, intValue = value)
        }

        fun boolean(parameter: AnimationParameter, value: Boolean): AnimationCondition {
            require(parameter.type == AnimationParameterType.BOOLEAN)
            return AnimationCondition(parameter, AnimationComparison.EQUAL, booleanValue = value)
        }
    }
}
