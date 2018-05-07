package org.nbkit.gen.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class DefinitionImplementationSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>,
        private val klass: ClassName
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = klass.simpleName()

    override fun generate() {
        val stubClass = ClassName("$basePackageName.psi.stubs", "${className}Stub")
        TypeSpec.classBuilder("${className}Mixin")
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(klass)
                .superclass(ParameterizedTypeName.get(
                        ClassName("", "${fileNamePrefix}DefinitionMixin"),
                        stubClass
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubClass)
                        .addParameter("nodeType", ParameterizedTypeName.get(
                                IStubElementType::class.asTypeName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .callSuperConstructor("node", "nodeType")
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
