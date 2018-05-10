package org.nbkit.gen

import org.nbkit.ScopeSpec
import org.nbkit.gen.navigation.NavigationSpec
import org.nbkit.gen.psi.PsiSpec
import org.nbkit.gen.refactoring.RefactoringSpec
import org.nbkit.gen.resolve.ResolvingSpec
import org.nbkit.gen.search.SearchSpec
import java.nio.file.Path
import java.nio.file.Paths

class NameBindingsSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {

    init {
        addSpec(PsiSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ResolvingSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(RefactoringSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(NavigationSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(SearchSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }

    class Builder {
        var fileNamePrefix: String? = null
        var basePackageName: String? = null
        var genPath: String = "src/gen_nb"

//        val namedElementsClasses: MutableSet<KClass<*>> = mutableSetOf()
//        val definitionsClasses: MutableSet<KClass<*>> = mutableSetOf()
//        val classesClasses: MutableSet<KClass<*>> = mutableSetOf()

        private val scopeRules: MutableList<ScopeSpec> = mutableListOf()

        fun addScopeRule(spec: ScopeSpec) = scopeRules.add(spec)

        fun build(): NameBindingsSpec = NameBindingsSpec(
                requireNotNull(fileNamePrefix),
                requireNotNull(basePackageName),
                Paths.get(genPath),
                scopeRules
        )
    }
}
