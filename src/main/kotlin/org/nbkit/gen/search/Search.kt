package org.nbkit.gen.search

import org.nbkit.ScopeRule
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class SearchSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : SpecGroup() {
    init {
        addSpec(WordScannerSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(FindUsagesProviderSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(GroupRuleProvidersSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
    }
}
