package org.nbkit

import kotlin.reflect.KClass

class ScopeRuleSpec(val klass: KClass<*>) {
    private var isNamedElement: Boolean = false
    private var isDefinition: Boolean = false
    private var isClass: Boolean = false
    private var isStubbed: Boolean = false
    private var isReferable: Boolean = false
    private var isReference: Boolean = false

    fun setIsNamedElement(value: Boolean = true): ScopeRuleSpec {
        isNamedElement = value
        return this
    }

    fun setIsDefinition(value: Boolean = true): ScopeRuleSpec {
        isDefinition = value
        return this
    }

    fun setIsClass(value: Boolean = true): ScopeRuleSpec {
        isClass = value
        return this
    }

    fun setIsStubbed(value: Boolean = true): ScopeRuleSpec {
        isStubbed = value
        return this
    }

    fun setIsReferable(value: Boolean = true): ScopeRuleSpec {
        isReferable = value
        return this
    }

    fun setIsReference(value: Boolean = true): ScopeRuleSpec {
        isReference = value
        return this
    }

    fun build(): ScopeRule = ScopeRule(
            klass,
            isNamedElement,
            isDefinition,
            isClass,
            isStubbed,
            isReferable,
            isReference
    )
}

class ScopeRule(
        val klass: KClass<*>,
        val isNamedElement: Boolean,
        val isDefinition: Boolean,
        val isClass: Boolean,
        val isStubbed: Boolean,
        val isReferable: Boolean,
        val isReference: Boolean
)
