package org.nbkit.gen.refactoring

import org.nbkit.ScopeSpec
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class RefactoringSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {
    init {
        addSpec(NamesValidatorSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(RefactoringSupportProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
