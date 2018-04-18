package org.nbkit.common.resolve

import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement

interface Scope {
    val items: Map<String, PsiElement>
    val symbols: Set<LookupElement>

    fun resolve(name: String): PsiElement?
}

class FilteredScope(
        private val scope: Scope,
        private val includeSet: Set<String>,
        private val include: Boolean
) : Scope {
    override val items: Map<String, PsiElement>
        get() {
            return if (include) {
                scope.items.filter { it.key in includeSet }
            } else {
                scope.items.filterNot { it.key in includeSet }
            }.toMap()
        }

    override val symbols: Set<LookupElement>
        get() {
            return if (include) {
                scope.symbols.filter { it.psiElement?.text in includeSet }
            } else {
                scope.symbols.filterNot { it.psiElement?.text in includeSet }
            }.toSet()
        }

    override fun resolve(name: String): PsiElement? = if (include) {
        if (includeSet.contains(name)) scope.resolve(name) else null
    } else {
        if (includeSet.contains(name)) null else scope.resolve(name)
    }
}

class MergeScope(private val scope1: Scope, private val scope2: Scope) : Scope {
    override val items: Map<String, PsiElement>
        get() = scope1.items + scope2.items

    override val symbols: Set<LookupElement>
        get() = scope1.symbols + scope2.symbols

    override fun resolve(name: String): PsiElement? =
            choose(scope1.resolve(name), scope2.resolve(name))

    private fun <T : PsiElement> choose(ref1: T?, ref2: T?): T? = ref1 ?: ref2
}

object EmptyScope : Scope {
    override val items: Map<String, PsiElement> = mapOf()
    override val symbols: Set<LookupElement> = emptySet()

    override fun resolve(name: String): PsiElement? = null
}

class OverridingScope(private val parent: Scope, private val child: Scope) : Scope {
    override val items: Map<String, PsiElement>
        get() = parent.items + child.items

    override val symbols: Set<LookupElement>
        get() = parent.symbols + child.symbols

    override fun resolve(name: String): PsiElement? =
            child.resolve(name) ?: parent.resolve(name)
}

class LocalScope : Scope {
    override val items: MutableMap<String, PsiElement> = mutableMapOf()
    override val symbols: Set<LookupElement>
        get() = items.values.map {
            if (it is PsiNamedElement) {
                LookupElementBuilder.createWithIcon(it)
            } else {
                LookupElementBuilder.create(it)
            }
        }.toSet()

    override fun resolve(name: String): PsiElement? = items[name]

    fun put(definition: PsiNamedElement?): PsiElement? =
            definition?.name?.let { items.put(it, definition) }

    fun put(name: String, definition: PsiElement): PsiElement? = items.put(name, definition)

    fun putAll(other: Scope) = items.putAll(other.items)
}
