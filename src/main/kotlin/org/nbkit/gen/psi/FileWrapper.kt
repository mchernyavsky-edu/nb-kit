package org.nbkit.gen.psi

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.squareup.kotlinpoet.*
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class FileWrapperSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .addSuperinterface(namedElementClass)
                .addSuperinterface(PsiFile::class, CodeBlock.of("file"))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("file", fileClass)
                        .build())
                .addProperty(PropertySpec.builder("file", fileClass)
                        .initializer("file")
                        .build())
                .addFunction(FunSpec.builder("setName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .returns(PsiElement::class)
                        .addCode(
                                """
                                    val nameWithExtension = if (name.endsWith('.' + %T.defaultExtension)) {
                                        name
                                    } else {
                                        name + '.' + %T.defaultExtension
                                    }

                                """.trimIndent(),
                                fileTypeClass, fileTypeClass
                        )
                        .addStatement("return file.setName(nameWithExtension)")
                        .build())
                .addFunction(FunSpec.builder("getName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return file.name.removeSuffix('.' + %T.defaultExtension)", fileTypeClass)
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
