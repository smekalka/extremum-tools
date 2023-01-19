package io.extremum.model.tools.mapper

import io.extremum.sharedmodels.basic.GraphQlList
import io.extremum.sharedmodels.basic.GraphQlListEdge

object GraphQlListUtils {

    /**
     * Название поля, содержащего список
     */
    const val LIST_FIELD_NAME = "edges"

    /**
     * Название поля, содержащего элемент в списке [LIST_FIELD_NAME]
     */
    const val LIST_ELEMENT_FIELD_NAME = "node"

    fun <T> GraphQlList<T>.toList(): List<T> = this.edges?.map { it.node } ?: listOf()

    fun <T> List<T>.toGraphQlList(): GraphQlList<T> {
        val edges = this.map { GraphQlListEdge<T>().apply { node = it } }
        return GraphQlList<T>().apply { this.edges = edges }
    }
}