package org.nbkit.gen.psi.stubs

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.tree.IStubFileElementType
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class StubElementTypeSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder("${fileNamePrefix}StubElementType")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType.withBounds(ParameterizedTypeName.get(
                        StubElement::class.asClassName(),
                        WildcardTypeName.subtypeOf(ANY)
                )))
                .addTypeVariable(psiType.withBounds(elementClass))
                .superclass(ParameterizedTypeName.get(
                        IStubElementType::class.asClassName(),
                        stubType,
                        psiType
                ))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("debugName", String::class)
                        .build())
                .addSuperclassConstructorParameter("debugName")
                .addSuperclassConstructorParameter("%T", languageClass)
                .addFunction(FunSpec.builder("getExternalId")
                        .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return \"$basePackageName.\${super.toString()}\"")
                        .build())
                .addFunction(FunSpec.builder("createStubIfParentIsStub")
                        .addModifiers(KModifier.PROTECTED)
                        .addParameter("node", ASTNode::class)
                        .returns(Boolean::class)
                        .addCode(
                                """
                                    val parent = node.treeParent
                                    val parentType = parent.elementType
                                    return (parentType is %T && parentType.shouldCreateStub(parent)) || parentType is %T

                                """.trimIndent(),
                                ParameterizedTypeName.get(
                                        IStubElementType::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY),
                                        WildcardTypeName.subtypeOf(ANY)
                                ),
                                ParameterizedTypeName.get(
                                        IStubFileElementType::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY)
                                )
                        )
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
