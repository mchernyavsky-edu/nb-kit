package org.nbkit.gen

import org.nbkit.gen.navigation.NavigationSpec
import org.nbkit.gen.psi.PsiSpec
import org.nbkit.gen.refactoring.RefactoringSpec
import org.nbkit.gen.resolve.ResolvingSpec
import org.nbkit.gen.search.SearchSpec
import org.nbkit.lang.ScopeRule
import java.nio.file.Path

class RootSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : SpecGroup() {
    init {
        addSpec(PsiSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ResolvingSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(RefactoringSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(NavigationSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(SearchSpec(fileNamePrefix, basePackageName, genPath, scopeRules))

//        addSpec(ScopeProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
//        addSpec(NamespaceProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
