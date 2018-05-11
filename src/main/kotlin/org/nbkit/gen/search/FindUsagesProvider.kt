package org.nbkit.gen.search

import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeSpec
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class FindUsagesProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .addSuperinterface(FindUsagesProvider::class)
                .addFunction(FunSpec.builder("getWordsScanner")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(WordsScanner::class)
                        .addStatement("return %T()", wordScannerClass)
                        .build())
                .addFunction(FunSpec.builder("canFindUsagesFor")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Boolean::class)
                        .addParameter("element", PsiElement::class)
                        .addStatement("return element is %T", namedElementClass)
                        .build())
                .addFunction(FunSpec.builder("getHelpId")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addParameter("element", PsiElement::class)
                        .addStatement("return %T.FIND_OTHER_USAGES", HelpID::class)
                        .build())
                .addFunction(FunSpec.builder("getType")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addParameter("element", PsiElement::class)
                        .addStatement(
                                buildString {
                                    append("return when (element) {\n")
                                    for (className in definitionClasses) {
                                        append("    is %T -> \"${className.commonName.toLowerCase()}\"\n")
                                    }
                                    append("    else -> \"\"\n")
                                    append("}\n")
                                }.trimMargin(),
                                *definitionClasses.toTypedArray()
                        )
                        .build())
                .addFunction(FunSpec.builder("getDescriptiveName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addParameter("element", PsiElement::class)
                        .addStatement(
                                """
                                    return when (element) {
                                        is %T -> element.name ?: "<unnamed>"
                                        else -> ""
                                    }
                                """.trimIndent(),
                                namedElementClass
                        )
                        .build())
                .addFunction(FunSpec.builder("getNodeText")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addParameter("element", PsiElement::class)
                        .addParameter("useFullName", Boolean::class)
                        .addStatement(
                                """
                                    return when (element) {
                                        is %T -> element.name!!
                                        else -> ""
                                    }
                                """.trimIndent(),
                                namedElementClass
                        )
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
