package org.nbkit.gen.psi

import org.nbkit.ScopeSpec
import org.nbkit.gen.SpecGroup
import org.nbkit.gen.psi.ext.ExtSpec
import org.nbkit.gen.psi.stubs.StubsSpec
import java.nio.file.Path

class PsiSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : SpecGroup() {
    init {
        addSpec(ExtSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(FileWrapperSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(DirectoryWrapperSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(StubsSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
