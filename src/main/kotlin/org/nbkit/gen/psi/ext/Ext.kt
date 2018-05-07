package org.nbkit.gen.psi.ext

import com.squareup.kotlinpoet.asClassName
import org.nbkit.ScopeRule
import org.nbkit.gen.SpecGroup
import java.nio.file.Path

class ExtSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : SpecGroup() {
    init {
        addSpec(ElementSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
//        addSpec(NamedElementSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ReferenceElementSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(IdentifiersSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
//        addSpec(DefinitionSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        for (scopeRule in scopeRules) {
            if (scopeRule.isDefinition) {
                addSpec(DefinitionImplementationSpec(
                        fileNamePrefix,
                        basePackageName,
                        genPath,
                        scopeRules,
                        scopeRule.klass.asClassName()
                ))
            }
        }
    }
}
