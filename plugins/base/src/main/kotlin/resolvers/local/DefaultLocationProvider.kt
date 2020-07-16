package org.jetbrains.dokka.base.resolvers.local

import org.jetbrains.dokka.DokkaConfiguration.DokkaSourceSet
import org.jetbrains.dokka.base.renderers.sourceSets
import org.jetbrains.dokka.base.resolvers.anchors.SymbolAnchorHint
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.withDescendants
import org.jetbrains.dokka.pages.*
import org.jetbrains.dokka.plugability.DokkaContext
import java.util.*

private const val PAGE_WITH_CHILDREN_SUFFIX = "index"

open class DefaultLocationProvider(
    protected val pageGraphRoot: RootPageNode,
    dokkaContext: DokkaContext
) : BaseLocationProvider(dokkaContext) {
    protected open val extension = ".html"

    protected val pagesIndex: Map<Pair<DRI, DokkaSourceSet>, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.dri.flatMap { dri ->
                    page.sourceSets().map { sourceSet -> (dri to sourceSet) to page }
                }
            }
            .groupingBy { it.first }
            .aggregate { key, _, (_, page), first ->
                if (first) page else throw AssertionError("Multiple pages associated with key: ${key.first}/${key.second}")
            }

    protected val anchorsIndex: Map<Pair<DRI, DokkaSourceSet>, ContentPage> =
        pageGraphRoot.withDescendants().filterIsInstance<ContentPage>()
            .flatMap { page ->
                page.content.withDescendants()
                    .filter { it.extra[SymbolAnchorHint] != null }
                    .mapNotNull { it.dci.dri.singleOrNull() }
                    .distinct()
                    .flatMap { dri ->
                        page.sourceSets().map { sourceSet ->
                            (dri to sourceSet) to page
                        }
                    }
            }.toMap()


    protected val pathsIndex: Map<PageNode, List<String>> = IdentityHashMap<PageNode, List<String>>().apply {
        fun registerPath(page: PageNode, prefix: List<String>) {
            val newPrefix = prefix + page.pathName
            put(page, newPrefix)
            page.children.forEach { registerPath(it, newPrefix) }
        }
        put(pageGraphRoot, emptyList())
        pageGraphRoot.children.forEach { registerPath(it, emptyList()) }
    }

    override fun resolve(node: PageNode, context: PageNode?, skipExtension: Boolean): String =
        pathTo(node, context) + if (!skipExtension) extension else ""

    override fun resolve(dri: DRI, sourceSets: Set<DokkaSourceSet>, context: PageNode?): String =
        sourceSets.map { Pair(dri, it) }.let {
            it.map { pagesIndex[it]?.let { resolve(it, context) } }.distinct().singleOrNull()
                ?: it.map { anchorsIndex[it]?.let { resolve(it, context) + "#$dri" } }.distinct().singleOrNull()
                ?: getExternalLocation(dri, sourceSets)
        }

    override fun resolveRoot(node: PageNode): String =
        pathTo(pageGraphRoot, node).removeSuffix(PAGE_WITH_CHILDREN_SUFFIX)

    override fun ancestors(node: PageNode): List<PageNode> =
        generateSequence(node) { it.parent() }.toList()

    protected open fun pathTo(node: PageNode, context: PageNode?): String {
        fun pathFor(page: PageNode) = pathsIndex[page] ?: throw AssertionError(
            "${page::class.simpleName}(${page.name}) does not belong to current page graph so it is impossible to compute its path"
        )

        val contextNode =
            if (context !is ClasslikePageNode && context?.children?.isEmpty() == true && context.parent() != null) context.parent() else context
        val nodePath = pathFor(node)
        val contextPath = contextNode?.let { pathFor(it) }.orEmpty()

        val commonPathElements = nodePath.asSequence().zip(contextPath.asSequence())
            .takeWhile { (a, b) -> a == b }.count()

        return (List(contextPath.size - commonPathElements) { ".." } + nodePath.drop(commonPathElements) +
                if (node is ClasslikePageNode || node.children.isNotEmpty())
                    listOf(PAGE_WITH_CHILDREN_SUFFIX)
                else
                    emptyList()
                ).joinToString("/")
    }

    private fun PageNode.parent() = pageGraphRoot.parentMap[this]
}


private val reservedFilenames = setOf("index", "con", "aux", "lst", "prn", "nul", "eof", "inp", "out")

internal fun identifierToFilename(name: String): String {
    if (name.isEmpty()) return "--root--"
    val escaped = name.replace("<|>".toRegex(), "-")
    val lowercase = escaped.replace("[A-Z]".toRegex()) { matchResult -> "-" + matchResult.value.toLowerCase() }
    return if (lowercase in reservedFilenames) "--$lowercase--" else lowercase
}

private val PageNode.pathName: String
    get() = if (this is PackagePageNode) name else identifierToFilename(
        name
    )
