package org.nbkit.gen.psi.stubs

import org.nbkit.ScopeRule
import org.nbkit.gen.SpecGroup
import org.nbkit.gen.psi.stubs.index.IndexesSpec
import java.nio.file.Path

class StubsSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : SpecGroup() {
    init {
        addSpec(IndexesSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(StubElementTypeSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(StubImplementationsSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(StubIndexingSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(StubInterfacesSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
