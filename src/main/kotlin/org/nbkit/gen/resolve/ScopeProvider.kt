package org.nbkit.gen.resolve

import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import org.nbkit.lang.ScopeRule
import org.nbkit.common.resolve.EmptyScope
import org.nbkit.common.resolve.LocalScope
import org.nbkit.common.resolve.OverridingScope
import org.nbkit.common.resolve.Scope
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class ScopeProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        val qualifiedIdPartImplMixinClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}QualifiedIdPartImplMixin")

        TypeSpec.objectBuilder("ScopeProvider")
                .addFunction(FunSpec.builder("getScope")
                        .addParameter("id", ClassName("$basePackageName.psi", "${fileNamePrefix}QualifiedIdPart"))
                        .returns(Scope::class)
                        .addStatement("val prev = id.%T()", ParameterizedTypeName.get(prevSiblingOfTypeFunction, qualifiedIdPartImplMixinClass))
                        .addStatement("return forIdentifier(prev, id)")
                        .build())
                .addFunction(FunSpec.builder("forIdentifier")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("id", qualifiedIdPartImplMixinClass.asNullable())
                        .addParameter("from", ClassName("$basePackageName.psi", "${fileNamePrefix}QualifiedIdPart"))
                        .returns(Scope::class)
                        .addCode(
                                """
                                    id ?: return forElement(from.parent, from)
                                    val resolved = id.scope.resolve(id.name)
                                    return when (resolved) {
                                        is %T -> %T.forFile(resolved, true)
                                        is %T -> %T.forDirectory(resolved)
                                        is %T -> %T.forModule(resolved, true)
                                        else -> %T
                                    }

                                """.trimIndent(),
                                fileWrapperClass, namespaceProviderClass,
                                directoryWrapperClass, namespaceProviderClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                namespaceProviderClass,
                                EmptyScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forElement")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", PsiElement::class.asClassName().asNullable())
                        .addParameter("from", PsiElement::class)
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    element ?: return %T()
                                    val useCommand = useCommand && element !is %T
                                    val parentScope = forElement(element.parent, element, useCommand)
                                    val scope = when (element) {
                                        is %T -> forFile(%T(element), useCommand)
                                        is %T -> forModule(element, useCommand)
                                        is %T -> forFunction(element)
                                        is %T -> forLet(element, from)
                                        else -> %T
                                    }
                                    return when {
                                        parentScope.items.isEmpty() -> scope
                                        scope.items.isEmpty() -> parentScope
                                        else -> %T(parentScope, scope)
                                    }

                                """.trimIndent(),
                                LocalScope::class,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Command"),
                                fileClass, fileWrapperClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Function"),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Let"),
                                EmptyScope::class, OverridingScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forFile")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("file", fileWrapperClass)
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val dirNamespace = file.parent?.let { %T.forDirectory(%T(it)) }
                                    val fileNamespace = %T.forFile(file, useCommand)
                                    return when {
                                        dirNamespace == null || dirNamespace.items.isEmpty() -> fileNamespace
                                        fileNamespace.items.isEmpty() -> dirNamespace
                                        else -> %T(dirNamespace, fileNamespace)
                                    }

                                """.trimIndent(),
                                namespaceProviderClass, directoryWrapperClass,
                                namespaceProviderClass,
                                OverridingScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forModule")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("module", ClassName("$basePackageName.psi", "${fileNamePrefix}Module"))
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addStatement("return %T.forModule(module, useCommand)", namespaceProviderClass)
                        .build())
                .addFunction(FunSpec.builder("forFunction")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", ClassName("$basePackageName.psi", "${fileNamePrefix}Function"))
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val scope = %T()
                                    scope.put(element.parameter)
                                    return scope

                                """.trimIndent(),
                                LocalScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forLet")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", ClassName("$basePackageName.psi", "${fileNamePrefix}Let"))
                        .addParameter("from", PsiElement::class)
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val scope = %T()
                                    val letType = (element.letKw ?: element.letrecKw ?: element.letparKw)?.%T
                                    val bindings = when {
                                        letType === %T.LETREC_KW || from === element.expression ->
                                            element.bindingList
                                        letType === %T.LET_KW ->
                                            element.bindingList.takeWhile { it !== from }
                                        else ->
                                            listOf()
                                    }
                                    bindings.forEach { scope.put(it) }
                                    return scope

                                """.trimIndent(),
                                LocalScope::class, elementTypeProperty,
                                elementTypesClass, elementTypesClass
                        )
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
