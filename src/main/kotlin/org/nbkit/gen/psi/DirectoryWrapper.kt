package org.nbkit.gen.psi

import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class DirectoryWrapperSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .addSuperinterface(namedElementClass)
                .addSuperinterface(PsiDirectory::class, CodeBlock.of("directory"))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("directory", PsiDirectory::class)
                        .build())
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(referenceClass.asNullable())
                        .addStatement("return null")
                        .build())
                .addFunction(FunSpec.builder("getNameIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(PsiElement::class.asClassName().asNullable())
                        .addStatement("return null")
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
