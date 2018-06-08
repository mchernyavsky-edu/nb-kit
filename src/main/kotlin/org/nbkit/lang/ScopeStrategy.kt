package org.nbkit.lang

import kotlin.reflect.KClass

interface ScopeStrategy

internal class AddElementsStrategy(
        var what: ElementsLocator,
        var where: ElementsLocator = CurrentElementLocator,
        var sequentially: Boolean = false
) : ScopeStrategy

internal class ImportElementsStrategy(
        var who: ElementsLocator,
        var where: ElementsLocator = CurrentElementLocator,
        var sequentially: Boolean = false
) : ScopeStrategy

//internal class ComposeStrategy(
//        val first: ScopeStrategy,
//        val second: ScopeStrategy
//) : ScopeStrategy {
//    fun toList(): List<ScopeStrategy> {
//        val firstList = (first as? ComposeStrategy)?.toList() ?: listOf(first)
//        val secondList = (second as? ComposeStrategy)?.toList() ?: listOf(second)
//        return firstList + secondList
//    }
//}

class QualifiedNameStrategy(
        var nameClass: KClass<*>? = null,
        var namesSeparator: String = ".",
        config: QualifiedNameStrategy.() -> Unit = {}
) : ScopeStrategy {
    init {
        config()
    }
}

class CurrentDirectoryStrategy(
        config: CurrentDirectoryStrategy.() -> Unit = {}
) : ScopeStrategy {
    init {
        config()
    }
}

class SourceRootStrategy(
        config: SourceRootStrategy.() -> Unit = {}
) : ScopeStrategy {
    init {
        config()
    }
}

class SpecifiedPathStrategy(
        config: SpecifiedPathStrategy.() -> Unit = {}
) : ScopeStrategy {
    init {
        config()
    }
}

infix fun ScopeStrategy.inside(place: ElementsLocator) = when (this) {
    is AddElementsStrategy -> where = place
    is ImportElementsStrategy -> where = place
    else -> Unit
}
