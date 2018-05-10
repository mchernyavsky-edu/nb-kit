package org.nbkit.gen.navigation

import org.nbkit.ScopeSpec
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class NavigationSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {
    init {
        addSpec(UtilsSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
//        addSpec(NavigationContributorBaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ClassNavigationContributorSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(SymbolNavigationContributorSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
