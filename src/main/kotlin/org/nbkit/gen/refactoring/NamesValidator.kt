package org.nbkit.gen.refactoring

import com.intellij.lang.refactoring.NamesValidator
import com.intellij.openapi.project.Project
import com.intellij.psi.tree.IElementType
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class NamesValidatorSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .addSuperinterface(NamesValidator::class)
                .addFunction(FunSpec.builder("isKeyword")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .returns(Boolean::class)
                        .addStatement("return getLexerType(name) in %T.keywords", tokenTypeClass)
                        .build())
                .addFunction(FunSpec.builder("isIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .returns(Boolean::class)
                        .addStatement("return getLexerType(name) in %T.identifiers && !containsComment(name)", tokenTypeClass)
                        .build())
                .addFunction(FunSpec.builder("containsComment")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("name", String::class)
                        .returns(Boolean::class)
                        .addStatement("return name.contains(\"#\")")
                        .build())
                .addFunction(FunSpec.builder("getLexerType")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("text", String::class)
                        .returns(IElementType::class.asClassName().asNullable())
                        .addStatement("val lexer = %T()", lexerAdapterClass)
                        .addStatement("lexer.start(text)")
                        .addStatement("return if (lexer.tokenEnd == text.length) lexer.tokenType else null")
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
