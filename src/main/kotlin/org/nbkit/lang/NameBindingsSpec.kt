package org.nbkit.lang

import com.intellij.psi.PsiElement
import org.nbkit.gen.RootSpec
import java.nio.file.Paths
import kotlin.reflect.KClass

@DslMarker
annotation class NameBindingMarker

@NameBindingMarker
interface LangSpec

class NameBindingsSpec : LangSpec {
    var fileNamePrefix: String? = null
    var basePackageName: String? = null
    var genPath: String = "src/gen_nb"

    private val scopeRules: MutableList<ScopeRule.Builder> = mutableListOf()

    private val namedElementClasses: MutableSet<KClass<*>> = mutableSetOf()
    private val definitionClasses: MutableSet<KClass<*>> = mutableSetOf()
    private val classClasses: MutableSet<KClass<*>> = mutableSetOf()

    fun addScopeRule(rule: ScopeRule.Builder) = scopeRules.add(rule)

    inline fun <reified T : PsiElement> scopeRule(
            noinline init: ScopeRule.Builder.(it: ScopeRule.Builder) -> Unit
    ) {
        val rule = ScopeRule.Builder(T::class)
        rule.init(rule)
        addScopeRule(rule)
    }

    inline fun <reified T : PsiElement> scopeRule(): ScopeRule.Builder {
        val rule = ScopeRule.Builder(T::class)
        addScopeRule(rule)
        return rule
    }

    fun indexes(init: IndexesSpec.() -> Unit) {
        val spec = IndexesSpec()
        spec.init()
        namedElementClasses.addAll(spec.namedElementClasses)
        definitionClasses.addAll(spec.definitionClasses)
        classClasses.addAll(spec.classClasses)
    }

    fun build(): RootSpec {
        setUpIndexes()
        setUpQualifiedNameParts()

        return RootSpec(
                requireNotNull(fileNamePrefix),
                requireNotNull(basePackageName),
                Paths.get(genPath),
                scopeRules.map { it.build() }
        )
    }

    private fun setUpIndexes() {
        (namedElementClasses + definitionClasses + classClasses)
                .filterNot { it in scopeRules.map { it.klass } }
                .map { ScopeRule.Builder(it) }
                .forEach { addScopeRule(it) }

        for (rule in scopeRules) {
            rule.isNamedElement = rule.klass in namedElementClasses
            rule.isDefinition = rule.klass in definitionClasses
            rule.isClass = rule.klass in classClasses
        }
    }

    private fun setUpQualifiedNameParts() {
        scopeRules
                .flatMap { it.strategies }
                .filterIsInstance<QualifiedNameStrategy>()
                .mapNotNull { it.nameClass }
                .flatMap { klass -> scopeRules.filter { it.klass == klass } }
                .forEach { it.isPartOfQualifiedName = true }
    }
}

fun nameBindings(init: NameBindingsSpec.() -> Unit): RootSpec {
    val spec = NameBindingsSpec()
    spec.init()
    return spec.build()
}
