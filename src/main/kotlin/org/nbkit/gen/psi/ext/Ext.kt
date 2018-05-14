package org.nbkit.gen.psi.ext

import com.squareup.kotlinpoet.asClassName
import org.nbkit.lang.ScopeRule
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
        addSpec(NamedElementSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(ReferenceElementSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(IdentifiersSpec(fileNamePrefix, basePackageName, genPath, scopeRules))
        addSpec(DefinitionSpec(fileNamePrefix, basePackageName, genPath, scopeRules))

        val definitionNames = scopeRules
                .filter { it.isDefinition }
                .map { it.klass.asClassName() }
        for (className in definitionNames) {
            addSpec(DefinitionImplementationSpec(
                    fileNamePrefix,
                    basePackageName,
                    genPath,
                    scopeRules,
                    className
            ))
        }
    }
}
