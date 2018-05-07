package org.nbkit.gen.resolve

import com.squareup.kotlinpoet.*
import org.nbkit.ScopeRule
import org.nbkit.common.resolve.EmptyScope
import org.nbkit.common.resolve.LocalScope
import org.nbkit.common.resolve.Scope
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class NamespaceProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        val qualifiedIdPartImplMixinClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}QualifiedIdPartImplMixin")

        TypeSpec.objectBuilder("NamespaceProvider")
                .addFunction(FunSpec.builder("forFile")
                        .addParameter("file", fileWrapperClass)
                        .addParameter("withCommands", Boolean::class)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    if (withCommands) {
                                        handleCommands(file, namespace)
                                    }
                                    file.children
                                            .filterIsInstance<%T>()
                                            .mapNotNull { it.definition }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent(),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Statement")
                        )
                        .build())
                .addFunction(FunSpec.builder("forDirectory")
                        .addParameter("directory", directoryWrapperClass)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    directory.files
                                            .filterIsInstance<%T>()
                                            .map { %T(it) }
                                            .forEach { namespace.put(it) }
                                    directory.subdirectories
                                            .map { %T(it) }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent(),
                                fileClass, fileWrapperClass, directoryWrapperClass
                        )
                        .build())
                .addFunction(FunSpec.builder("forModule")
                        .addParameter("module", ClassName("$basePackageName.psi", "${fileNamePrefix}Module"))
                        .addParameter("withCommands", Boolean::class)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    if (withCommands) {
                                        handleCommands(module, namespace)
                                    }
                                    module.statementList
                                            .mapNotNull { it.definition }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent()
                        )
                        .build())
                .addFunction(FunSpec.builder("handleCommands")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", elementClass)
                        .addParameter("namespace", LocalScope::class)
                        .addCode(
                                """
                                    element.children
                                        .filterIsInstance<%T>()
                                        .mapNotNull { it.command }
                                        .map {
                                            it.qualifiedId
                                                    .children
                                                    .filterIsInstance<%T>()
                                                    .last()
                                        }
                                        .map { it.scope.resolve(it.name) }
                                        .mapNotNull {
                                            when (it) {
                                                is %T -> forFile(it, false)
                                                is %T -> forDirectory(it)
                                                is %T -> forModule(it, false)
                                                else -> %T
                                            }
                                        }
                                        .forEach { namespace.putAll(it) }

                                """.trimIndent(),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Statement"),
                                qualifiedIdPartImplMixinClass,
                                fileWrapperClass, directoryWrapperClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                EmptyScope::class
                        )
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
