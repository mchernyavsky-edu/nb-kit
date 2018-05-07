package org.nbkit.gen.psi.ext

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class ElementSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.interfaceBuilder("${fileNamePrefix}Element")
                .addSuperinterface(PsiElement::class)
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        .returns(referenceClass.asNullable())
                        .build())
                .build()
                .also { addType(it) }

        TypeSpec.classBuilder("${fileNamePrefix}ElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .build())
                .addSuperinterface(elementClass)
                .superclass(ASTWrapperPsiElement::class)
                .addSuperclassConstructorParameter("node")
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(referenceClass.asNullable())
                        .addStatement("return null")
                        .build())
                .build()
                .also { addType(it) }

        TypeSpec.classBuilder("${fileNamePrefix}StubbedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType
                        .withBounds(ParameterizedTypeName.get(
                                StubElement::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY))
                        ))
                .addSuperinterface(elementClass)
                .superclass(ParameterizedTypeName.get(
                        StubBasedPsiElementBase::class.asClassName(),
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
                .addFunction(FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return \"\${javaClass.simpleName}(\$elementType)\"")
                        .build())
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(referenceClass.asNullable())
                        .addStatement("return null")
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
