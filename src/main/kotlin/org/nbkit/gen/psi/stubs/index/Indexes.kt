package org.nbkit.gen.psi.stubs.index

import org.nbkit.ScopeSpec
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class IndexesSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {
    init {
        addSpec(DefinitionIndexSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(GotoClassIndexSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(NamedElementIndexSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
