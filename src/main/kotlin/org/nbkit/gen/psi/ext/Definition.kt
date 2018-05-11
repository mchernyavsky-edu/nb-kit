package org.nbkit.gen.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.squareup.kotlinpoet.*
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class DefinitionSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder("${fileNamePrefix}DefinitionMixin")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType
                        .withBounds(
                                ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}NamedStub"),
                                ParameterizedTypeName.get(
                                        StubElement::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY)
                                )
                        ))
                .superclass(ParameterizedTypeName.get(
                        ClassName("", "${fileNamePrefix}StubbedNamedElementImpl"),
                        stubType
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubType)
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
