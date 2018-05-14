package org.nbkit.gen.resolve

import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import org.nbkit.common.resolve.EmptyScope
import org.nbkit.common.resolve.LocalScope
import org.nbkit.common.resolve.OverridingScope
import org.nbkit.common.resolve.Scope
import org.nbkit.gen.BaseSpec
import org.nbkit.lang.AddElementsStrategy
import org.nbkit.lang.QualifiedNameStrategy
import org.nbkit.lang.ScopeRule
import java.nio.file.Path

class ScopeProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        val spec = TypeSpec.objectBuilder("ScopeProvider")

        generateSimpleNames(spec)
        generateQualifiedNames(spec)
        generateAddElements(spec)

//        spec
//                .addFunction(FunSpec.builder("forModule")
//                        .addModifiers(KModifier.PRIVATE)
//                        .addParameter("element", ClassName("$basePackageName.psi", "${fileNamePrefix}Module"))
//                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
//                                .defaultValue("true")
//                                .build())
//                        .returns(Scope::class)
//                        .addStatement("return %T.forModule(element, useCommand)", namespaceProviderClass)
//                        .build())
//                .addFunction(FunSpec.builder("forFile")
//                        .addModifiers(KModifier.PRIVATE)
//                        .addParameter("file", fileWrapperClass)
//                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
//                                .defaultValue("true")
//                                .build())
//                        .returns(Scope::class)
//                        .addCode(
//                                """
//                            val dirNamespace = file.parent?.let { %T.forDirectory(%T(it)) }
//                            val fileNamespace = %T.forFile(file, useCommand)
//                            return when {
//                                dirNamespace == null || dirNamespace.items.isEmpty() -> fileNamespace
//                                fileNamespace.items.isEmpty() -> dirNamespace
//                                else -> %T(dirNamespace, fileNamespace)
//                            }
//
//                        """.trimIndent(),
//                                namespaceProviderClass, directoryWrapperClass,
//                                namespaceProviderClass,
//                                OverridingScope::class
//                        )
//                        .build())

        addType(spec.build())
        writeToFile()
    }

    private fun generateSimpleNames(spec: TypeSpec.Builder) {
        val simpleNameRules = scopeRules.filter { rule ->
            rule.isReference && !rule.isPartOfQualifiedName
        }

        for (rule in simpleNameRules) {
            val className = rule.klass
            spec
                    .addFunction(FunSpec.builder("getScope")
                            .addParameter("element", className)
                            .returns(Scope::class)
                            .addStatement("return forElement(element.parent, element)")
                            .build())
        }
    }

    private fun generateQualifiedNames(spec: TypeSpec.Builder) {
        val qualifiedNameRules = scopeRules.filter { rule ->
            rule.strategies.isNotEmpty() && rule.strategies.all { it is QualifiedNameStrategy }
        }

        for (rule in qualifiedNameRules) {
            val strategy = rule.strategies.first() as QualifiedNameStrategy
            val className = requireNotNull(strategy.nameClass?.asClassName())
            val nameImplMixinClass = ClassName("$basePackageName.psi.ext", "${className.simpleName()}ImplMixin")
            spec
                    .addFunction(FunSpec.builder("getScope")
                            .addParameter("element", className)
                            .returns(Scope::class)
                            .addStatement("return forIdentifier(element.%T(), element)",
                                    ParameterizedTypeName.get(prevSiblingOfTypeFunction, nameImplMixinClass))
                            .build())
                    .addFunction(FunSpec.builder("forIdentifier")
                            .addModifiers(KModifier.PRIVATE)
                            .addParameter("prevSibling", nameImplMixinClass.asNullable())
                            .addParameter("element", PsiElement::class)
                            .returns(Scope::class)
                            .addCode(
                                    """
                                            return if (prevSibling != null) {
                                                val resolved = prevSibling.scope.resolve(prevSibling.name)
                                                when (resolved) {
                                                    is %T -> %T.forModule(resolved, true)
                                                    is %T -> %T.forFile(resolved, true)
                                                    is %T -> %T.forDirectory(resolved)
                                                    else -> %T
                                                }
                                            } else {
                                                forElement(element.parent, element)
                                            }

                                    """.trimIndent(),
                                    ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                    namespaceProviderClass,
                                    fileWrapperClass, namespaceProviderClass,
                                    directoryWrapperClass, namespaceProviderClass,
                                    EmptyScope::class
                            )
                            .build())
        }
    }

    private fun generateAddElements(spec: TypeSpec.Builder) {
        val addElementsRules = scopeRules.filter { rule ->
            rule.strategies.isNotEmpty() && rule.strategies.all { it is AddElementsStrategy }
        }

        spec.addFunction(FunSpec.builder("forElement")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("element", PsiElement::class.asClassName().asNullable())
                .addParameter("prev", PsiElement::class)
                .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                        .defaultValue("true")
                        .build())
                .returns(Scope::class)
                .addStatement("element ?: return %T()", LocalScope::class)
                .addStatement("val parentScope = forElement(element.parent, element)")
                .apply {
                    addCode("val scope = when (element) {\n")
                    for (rule in addElementsRules) {
                        val className = rule.klass.asClassName()
                        addCode("    is %T -> for%L(element, prev)\n", className, className.commonName)
                    }
                    addCode("    else -> %T\n", EmptyScope::class)
                    addCode("}\n")
                }
                .addCode(
                        """
                                return when {
                                    parentScope.items.isEmpty() -> scope
                                    scope.items.isEmpty() -> parentScope
                                    else -> %T(parentScope, scope)
                                }

                            """.trimIndent(), OverridingScope::class
                )
                .build())

        for (rule in addElementsRules) {
            val className = rule.klass.asClassName()
            spec.addFunction(FunSpec.builder("for${className.commonName}")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("element", className)
                    .addParameter("prev", PsiElement::class)
                    .returns(Scope::class)
                    .apply { emitFunction(rule) }
                    .build())
        }
    }

    private fun FunSpec.Builder.emitFunction(rule: ScopeRule) {
        addStatement("val scope = %T()", LocalScope::class)
        for (strategy in rule.strategies) {
            when (strategy) {
                is AddElementsStrategy -> {
                    val seq = strategy.sequentially
                    for (what in strategy.what.classes) {
                        for (where in strategy.where.classes) {
                            addCode("if (prev is %T) {\n", where)
                            addCode("    element\n")
                            addCode(
                                    "        .%T(%L)\n",
                                    ParameterizedTypeName.get(childrenOfTypeFunction, what.asClassName()),
                                    if (seq) "prev" else "null"
                            )
                            addCode("        .forEach { scope.put(it) }\n")
                            addCode("}\n")
                        }
                    }
                }
                else -> Unit
            }
        }
        addStatement("return scope")
    }
}
