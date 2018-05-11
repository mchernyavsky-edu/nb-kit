package org.nbkit.gen.navigation

import com.intellij.ide.projectView.PresentationData
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.asTypeName
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class UtilsSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        FunSpec.builder("getPresentation")
                .addParameter("psi", elementClass)
                .returns(ItemPresentation::class)
                .addStatement("val location = \"(in \${psi.containingFile.name})\"")
                .addStatement("val name = presentableName(psi)")
                .addStatement("return %T(name, location, psi.getIcon(0), null)", PresentationData::class)
                .build()
                .also { addFunction(it) }

        FunSpec.builder("presentableName")
                .addModifiers(KModifier.PRIVATE)
                .addParameter("psi", PsiElement::class)
                .returns(String::class.asTypeName().asNullable())
                .addStatement(
                        """
                            return when (psi) {
                                is %T -> psi.name
                                else -> null
                            }
                        """.trimIndent(),
                        namedElementClass)
                .build()
                .also { addFunction(it) }

        writeToFile()
    }
}
