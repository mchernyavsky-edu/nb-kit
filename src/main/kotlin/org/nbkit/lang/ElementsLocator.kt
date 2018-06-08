package org.nbkit.lang

import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

interface ElementsLocator {
    operator fun div(other: ElementsLocator): ComposeElementsLocator {
        when (other) {
            is ConcreteElementsLocator -> ComposeElementsLocator(this, other)
            is ComposeElementsLocator -> this / other.first / other.second
            else -> error("Unsupported locator type")
        }
        return ComposeElementsLocator(this, other)
    }
}

open class ConcreteElementsLocator(val classes: List<KClass<*>>) : ElementsLocator {
    operator fun plus(other: ConcreteElementsLocator): ConcreteElementsLocator {
        return ConcreteElementsLocator(classes + other.classes)
    }
}

object CurrentElementLocator : ConcreteElementsLocator(listOf(Any::class))

class ComposeElementsLocator(
        val first: ElementsLocator,
        val second: ElementsLocator
): ElementsLocator {
    fun toList(): List<ConcreteElementsLocator> {
        val firstList = when (first) {
            is ConcreteElementsLocator -> listOf(first)
            is ComposeElementsLocator -> first.toList()
            else -> listOf()
        }
        val secondList = when (second) {
            is ConcreteElementsLocator -> listOf(second)
            is ComposeElementsLocator -> second.toList()
            else -> listOf()
        }
        return firstList + secondList
    }
}

inline fun <reified T : PsiElement> children(): ConcreteElementsLocator = ConcreteElementsLocator(listOf(T::class))
