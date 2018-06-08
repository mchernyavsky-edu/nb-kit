package org.nbkit.gen.resolve

import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import org.nbkit.common.resolve.EmptyScope
import org.nbkit.common.resolve.LocalScope
import org.nbkit.common.resolve.OverridingScope
import org.nbkit.common.resolve.Scope
import org.nbkit.gen.BaseSpec
import org.nbkit.lang.*
import java.nio.file.Path
import kotlin.reflect.KClass

class ScopeProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    private val simpleNameRules = scopeRules.filter { rule ->
        rule.isReference && !rule.isPartOfQualifiedName
    }

    private val qualifiedNameRules = scopeRules.filter { rule ->
        rule.strategies.isNotEmpty() && rule.strategies.all { it is QualifiedNameStrategy }
    }

    private val addElementsRules = scopeRules.filter { rule ->
        rule.strategies.isNotEmpty() && rule.strategies.all {
            it is AddElementsStrategy || it is ImportElementsStrategy || it is CurrentDirectoryStrategy
        }
    }

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
        for (rule in simpleNameRules) {
            val className = rule.klass
            spec
                    .addFunction(FunSpec.builder("getScope")
                            .addParameter("element", className)
                            .returns(Scope::class)
                            .addStatement("return forElement(element.parent, listOf(element))")
                            .build())
        }
    }

    private fun generateQualifiedNames(spec: TypeSpec.Builder) {
        for (rule in qualifiedNameRules) {
            val strategy = rule.strategies.first() as QualifiedNameStrategy
            val className = requireNotNull(strategy.nameClass?.asClassName())
            val nameImplMixinClass = ClassName("$basePackageName.psi.ext", "${className.simpleName()}ImplMixin")
            spec
                    .addFunction(FunSpec.builder("getScope")
                            .addParameter("element", className)
                            .returns(Scope::class)
                            .addStatement("return forIdentifier(element.%T(), listOf(element) + prevs)",
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
                                                    is %T -> %T.forModule(resolved)
                                                    is %T -> %T.forFile(resolved)
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
        spec.addFunction(FunSpec.builder("forElement")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("element", PsiElement::class.asClassName().asNullable())
                .addParameter("prevs", ParameterizedTypeName.get(
                        List::class,
                        PsiElement::class
                ))
                .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                        .defaultValue("true")
                        .build())
                .returns(Scope::class)
                .addStatement("element ?: return %T()", LocalScope::class)
                .addStatement("val parentScope = forElement(element.parent, listOf(element) + prevs)")
                .addStatement("val namespace = getNamespace(element, prevs, parentScope)")
                .addCode(
                        """
                                return when {
                                    parentScope.items.isEmpty() -> namespace
                                    namespace.items.isEmpty() -> parentScope
                                    else -> %T(parentScope, namespace)
                                }

                        """.trimIndent(), OverridingScope::class
                )
                .build())

        spec.addFunction(FunSpec.builder("getNamespace")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("element", PsiElement::class.asClassName().asNullable())
                .addParameter("prevs", ParameterizedTypeName.get(
                        List::class,
                        PsiElement::class
                ))
                .addParameter("parentScope", Scope::class)
                .returns(Scope::class)
                .addStatement("element ?: return %T()", LocalScope::class)
                .addCode("return when (element) {\n")
                .apply {
                    for (rule in addElementsRules) {
                        val className = rule.klass.asClassName()
                        addCode("    is %T -> for%L(element, prevs, parentScope)\n", className, className.commonName)
                    }
                }
                .addCode("    else -> getNamespace(element.parent, prevs, parentScope)\n")
                .addCode("}\n")
                .build())

        for (rule in addElementsRules) {
            val className = rule.klass.asClassName()
            spec.addFunction(FunSpec.builder("for${className.commonName}")
                    .addModifiers(KModifier.PRIVATE)
                    .addParameter("element", className)
                    .addParameter("prevs", ParameterizedTypeName.get(
                            List::class,
                            PsiElement::class
                    ))
                    .addParameter("parentScope", Scope::class)
                    .returns(Scope::class)
                    .apply { emitFunction(rule) }
                    .build())
        }
    }

    private fun FunSpec.Builder.emitFunction(rule: ScopeRule) {
        addStatement("val scope = %T()", LocalScope::class)
        addStatement("var where: %T = listOf()", ParameterizedTypeName.get(
                List::class.asClassName(),
                ParameterizedTypeName.get(
                        KClass::class.asClassName(),
                        WildcardTypeName.subtypeOf(Any::class)
                )
        ))
        for (strategy in rule.strategies) {
            when (strategy) {
                is AddElementsStrategy -> {
                    val seq = strategy.sequentially

                    val what = strategy.what
                    val where = strategy.where.toList()

                    emitWhereList(where)

                    addCode(
                            """
                                if (where.zip(prevs).all { (klass, prev) ->
                                    klass.isInstance(prev)
                                }) {

                            """.trimIndent()
                    )
                    addCode("    element\n")
                    val locators = what.toList()
                    for (locator in locators) {
                        addCode(
                                "        .%T(%L)\n",
                                ParameterizedTypeName.get(
                                        childrenOfTypeFunction,
                                        locator.classes.first().asClassName()
                                ),
                                if (seq) "prevs.first()" else "null"
                        )
                    }
                    addCode("        .forEach { scope.put(it) }\n")
                    addCode("}\n")
                }
                is ImportElementsStrategy -> {
                    val seq = strategy.sequentially
                    val who = strategy.who.toList()
                    val where = strategy.where.toList()

                    emitWhereList(where)

                    addCode(
                            """
                            if (where.zip(prevs).all { (klass, prev) ->
                                klass.isInstance(prev)
                            }) {

                        """.trimIndent()
                    )
                    addCode("    element\n")

                    for (locator in who) {
                        val klass = locator.classes.first()
                        addCode("        .%T(%L)\n",
                                ParameterizedTypeName.get(childrenOfTypeFunction, klass.asClassName()),
                                if (seq) "prevs.first()" else "null")
                    }

                    val className = who.last().classes.first().asClassName()
                    val nameImplMixinClass = ClassName("$basePackageName.psi.ext", "${className.simpleName()}ImplMixin")
                    addCode("        .filterIsInstance<%T>()\n", nameImplMixinClass)
                    addCode("        .mapNotNull { parentScope.resolve(it.name) }\n")
                    addCode("        .mapNotNull { getNamespace(it, prevs, parentScope) }\n")
                    addCode("        .forEach { scope.putAll(it) }\n")
                    addCode("}\n")
                }
                is CurrentDirectoryStrategy -> {
                    addCode(
                            """
                                val containingDirectory = element
                                        .containingFile
                                        .containingDirectory
                                containingDirectory
                                        .files
                                        .filterIsInstance<%T>()
                                        .map { %T(it) }
                                        .forEach { scope.put(it) }
                                containingDirectory
                                        .subdirectories
                                        .map { %T(it) }
                                        .forEach { scope.put(it) }

                            """.trimIndent(),
                            fileClass, fileWrapperClass, directoryWrapperClass
                    )
                }
                else -> Unit
            }
        }
        addStatement("return scope")
    }

    private fun FunSpec.Builder.emitWhereList(where: List<ConcreteElementsLocator>) {
        addCode("where = listOf(\n")
        where.forEachIndexed { i, elem ->
            if (i == 0) {
                addCode("    %T::class", elem.classes.first())
            } else {
                addCode(",\n    %T::class", elem.classes.first())
            }
        }
        addCode("\n)\n")
    }

    private fun ElementsLocator.toList(): List<ConcreteElementsLocator> {
        return when (this) {
            is ConcreteElementsLocator -> listOf(this)
            is ComposeElementsLocator -> this.toList()
            else -> error("Unknown locator type")
        }
    }
}
