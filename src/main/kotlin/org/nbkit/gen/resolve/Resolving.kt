package org.nbkit.gen.resolve

import org.nbkit.ScopeSpec
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class ResolvingSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {
    init {
        addSpec(ReferenceSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ScopeProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(NamespaceProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
