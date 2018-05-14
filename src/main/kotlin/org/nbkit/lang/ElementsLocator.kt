package org.nbkit.lang

import com.intellij.psi.PsiElement
import kotlin.reflect.KClass

interface ScopeDefinition

open class ElementsLocator(val classes: List<KClass<*>>) : ScopeDefinition {

    operator fun plus(other: ElementsLocator): ElementsLocator {
        return ElementsLocator(classes + other.classes)
    }

    operator fun div(other: ElementsLocator): ElementsLocator {
        return ElementsLocator(classes + other.classes)
    }
}

object CurrentElementLocator : ElementsLocator(listOf(Any::class))

class ComposeScopeDefinition(val first: ScopeDefinition, val second: ScopeDefinition): ScopeDefinition

inline fun <reified T : PsiElement> children(): ElementsLocator = ElementsLocator(listOf(T::class))
