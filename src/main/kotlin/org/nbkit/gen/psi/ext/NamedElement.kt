package org.nbkit.gen.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class NamedElementSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.interfaceBuilder("${fileNamePrefix}NamedElement")
                .addSuperinterface(elementClass)
                .addSuperinterface(PsiNameIdentifierOwner::class)
                .addSuperinterface(NavigatablePsiElement::class)
                .build()
                .also { addType(it) }

        TypeSpec.classBuilder("${fileNamePrefix}NamedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(namedElementClass)
                .superclass(elementImplClass)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .build())
                .addSuperclassConstructorParameter("node")
                .addFunction(FunSpec.builder("getNameIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(elementImplClass.asNullable())
                        .addStatement("return %T()", childOfTypeFunction)
                        .build())
                .addFunction(FunSpec.builder("getName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class.asTypeName().asNullable())
                        .addStatement("return nameIdentifier?.text")
                        .build())
                .addFunction(FunSpec.builder("setName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .returns(PsiElement::class.asTypeName().asNullable())
                        .addStatement("val factory = %T(project)", psiFactoryClass)
                        .addCode(
                                buildString {
                                    append("val newNameIdentifier = when (nameIdentifier) {\n")
                                    for (scopeRule in scopeRules) {
                                        if (scopeRule.isReferable || scopeRule.isReference) {
                                            val className = scopeRule.klass.asClassName()
                                            append("    is %T -> factory.create${className.commonName}(name)\n")
                                        }
                                    }
                                    append("    else -> return this\n")
                                    append("}")
                                }.trimMargin(),
                                *scopeRules
                                        .filter { it.isReferable || it.isReference }
                                        .map { it.klass.asClassName() }
                                        .toTypedArray()
                        )
                        .addStatement("")
                        .addStatement("nameIdentifier?.replace(newNameIdentifier)")
                        .addStatement("return this")
                        .build())
                .addFunction(FunSpec.builder("getNavigationElement")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(PsiElement::class)
                        .addStatement("return nameIdentifier ?: this")
                        .build())
                .addFunction(FunSpec.builder("getTextOffset")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Int::class)
                        .addStatement("return nameIdentifier?.textOffset ?: super.getTextOffset()")
                        .build())
                .addFunction(FunSpec.builder("getPresentation")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ItemPresentation::class)
                        .addStatement("return %T(this)", getPresentationFunction)
                        .build())
                .build()
                .also { addType(it) }

        TypeSpec.classBuilder("${fileNamePrefix}StubbedNamedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType.withBounds(
                        namedStubClass,
                        ParameterizedTypeName.get(
                                StubElement::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY)
                        )
                ))
                .addSuperinterface(namedElementClass)
                .superclass(ParameterizedTypeName.get(
                        stubbedElementImplClass,
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
                .addFunction(FunSpec.builder("getNameIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(elementImplClass.asNullable())
                        .addStatement("return childOfType()")
                        .build())
                .addFunction(FunSpec.builder("getName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class.asTypeName().asNullable())
                        .addStatement("return nameIdentifier?.text")
                        .build())
                .addFunction(FunSpec.builder("setName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .returns(PsiElement::class.asTypeName().asNullable())
                        .addStatement("val factory = %T(project)", psiFactoryClass)
                        .addCode(
                                buildString {
                                    append("val newNameIdentifier = when (nameIdentifier) {\n")
                                    for (scopeRule in scopeRules) {
                                        if (scopeRule.isReferable || scopeRule.isReference) {
                                            val className = scopeRule.klass.asClassName()
                                            append("    is %T -> factory.create${className.commonName}(name)\n")
                                        }
                                    }
                                    append("    else -> return this\n")
                                    append("}\n\n")
                                }.trimMargin(),
                                *scopeRules
                                        .filter { it.isReferable || it.isReference }
                                        .map { it.klass.asClassName() }
                                        .toTypedArray()
                        )
                        .addStatement("")
                        .addStatement("nameIdentifier?.replace(newNameIdentifier)")
                        .addStatement("return this")
                        .build())
                .addFunction(FunSpec.builder("getNavigationElement")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(PsiElement::class)
                        .addStatement("return nameIdentifier ?: this")
                        .build())
                .addFunction(FunSpec.builder("getTextOffset")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Int::class)
                        .addStatement("return nameIdentifier?.textOffset ?: super.getTextOffset()")
                        .build())
                .addFunction(FunSpec.builder("getPresentation")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ItemPresentation::class)
                        .addStatement("return %T(this)", getPresentationFunction)
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}