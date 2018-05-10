package org.nbkit.gen.psi.ext

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.common.resolve.Scope
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class IdentifiersSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        for (className in referableNames) {
            TypeSpec.classBuilder("${className.simpleName()}ImplMixin")
                    .addModifiers(KModifier.ABSTRACT)
                    .addSuperinterface(className)
                    .superclass(elementImplClass)
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("node", ASTNode::class)
                            .build())
                    .addSuperclassConstructorParameter("node")
                    .addProperty(PropertySpec.builder(
                            "referenceNameElement",
                            ClassName("$basePackageName.psi.ext", "${className.simpleName()}ImplMixin"), KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return this").build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceName",
                            String::class,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return referenceNameElement.text").build())
                            .build())
                    .addFunction(FunSpec.builder("getName")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return referenceName")
                            .build())
                    .build()
                    .also { addType(it) }
        }

        for (className in referenceNames) {
            TypeSpec.classBuilder("${className.simpleName()}ImplMixin")
                    .addModifiers(KModifier.ABSTRACT)
                    .addSuperinterface(className)
                    .superclass(elementImplClass)
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("node", ASTNode::class)
                            .build())
                    .addSuperclassConstructorParameter("node")
                    .addProperty(PropertySpec.builder(
                            "scope",
                            Scope::class)
                            .getter(FunSpec.getterBuilder().addStatement("return %T.getScope(this)", scopeProviderClass).build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceNameElement",
                            elementClass,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return this").build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceName",
                            String::class,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return text").build())
                            .build())
                    .addFunction(FunSpec.builder("getName")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return referenceName")
                            .build())
                    .addFunction(FunSpec.builder("getReference")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(referenceClass)
                            .addStatement("return ${fileNamePrefix}IdReference()")
                            .build())
                    .addType(TypeSpec.classBuilder("${fileNamePrefix}IdReference")
                            .addModifiers(KModifier.PRIVATE)
                            .addModifiers(KModifier.INNER)
                            .superclass(ParameterizedTypeName.get(
                                    ClassName("$basePackageName.resolve", "${fileNamePrefix}ReferenceBase"),
                                    className
                            ))
                            .addSuperclassConstructorParameter("this@${className.simpleName()}ImplMixin")
                            .addFunction(FunSpec.builder("resolve")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(PsiElement::class.asClassName().asNullable())
                                    .addStatement("return scope.resolve(name)")
                                    .build())
                            .addFunction(FunSpec.builder("getVariants")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(ParameterizedTypeName.get(Array<Any>::class.asClassName(), ANY))
                                    .addStatement("return scope.symbols.toTypedArray()")
                                    .build())
                            .build())
                    .build()
                    .also { addType(it) }
        }

        writeToFile()
    }
}
