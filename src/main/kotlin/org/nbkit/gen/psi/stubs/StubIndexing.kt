package org.nbkit.gen.psi.stubs

import com.intellij.psi.stubs.IndexSink
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class StubIndexingSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        FunSpec.builder("indexNamedStub")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stubs", namedStubClass)
                .addStatement("stubs.name?.let { occurrence(%T.KEY, it) }", namedElementIndexClass)
                .build()
                .also { addFunction(it) }

        FunSpec.builder("indexDefinitionStub")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stubs", namedStubClass)
                .addStatement("stubs.name?.let { occurrence(%T.KEY, it) }", namedElementIndexClass)
                .build()
                .also { addFunction(it) }

        FunSpec.builder("indexGotoClass")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stubs", namedStubClass)
                .addStatement("stubs.name?.let { occurrence(%T.KEY, it) }", gotoClassIndexClass)
                .build()
                .also { addFunction(it) }

        for (scopeRule in scopeRules) {
            if (scopeRule.isDefinition) {
                val className = scopeRule.klass.asClassName()
                val stubClass = ClassName("$basePackageName.psi.stubs", "${className.simpleName()}Stub")
                val indexSpec = FunSpec.builder("index${className.commonName}")
                        .receiver(IndexSink::class)
                        .addParameter("stubs", stubClass)
                if (scopeRule.isNamedElement) {
                    indexSpec.addStatement("indexNamedStub(stubs)")
                }
                if (scopeRule.isDefinition) {
                    indexSpec.addStatement("indexDefinitionStub(stubs)")
                }
                if (scopeRule.isClass) {
                    indexSpec.addStatement("indexGotoClass(stubs)")
                }
                addFunction(indexSpec.build())
            }
        }

        writeToFile()
    }
}
