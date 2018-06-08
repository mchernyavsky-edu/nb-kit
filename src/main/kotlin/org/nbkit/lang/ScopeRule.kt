package org.nbkit.lang

import kotlin.reflect.KClass

class ScopeRule(
        val klass: KClass<*>,
        val isNamedElement: Boolean,
        val isDefinition: Boolean,
        val isClass: Boolean,

        val isReferable: Boolean,
        val isReference: Boolean,
        val isPartOfQualifiedName: Boolean,

        val strategies: List<ScopeStrategy>
) {
    class Builder(val klass: KClass<*>) {
        internal var isNamedElement: Boolean = false
        internal var isDefinition: Boolean = false
        internal var isClass: Boolean = false

        var isReferable: Boolean = false
        var isReference: Boolean = false
        internal var isPartOfQualifiedName: Boolean = false

        internal var strategies: MutableList<ScopeStrategy> = mutableListOf()

        infix fun defines(what: ElementsLocator): ScopeStrategy {
            val strategy = AddElementsStrategy(what)
            strategies.add(strategy)
            return strategy
        }

        infix fun definesSeq(what: ElementsLocator): ScopeStrategy {
            val strategy = AddElementsStrategy(what, sequentially = true)
            strategies.add(strategy)
            return strategy
        }

        infix fun imports(who: ElementsLocator): ScopeStrategy {
            val strategy = ImportElementsStrategy(who)
            strategies.add(strategy)
            return strategy
        }

        infix fun importsSeq(who: ElementsLocator): ScopeStrategy {
            val strategy = ImportElementsStrategy(who, sequentially = true)
            strategies.add(strategy)
            return strategy
        }

        infix fun delegates(other: ScopeStrategy) {
            strategies = mutableListOf(other)
        }

        fun build(): ScopeRule = ScopeRule(
                klass,
                isNamedElement,
                isDefinition,
                isClass,
                isReferable,
                isReference,
                isPartOfQualifiedName,
                strategies
        )
    }
}
