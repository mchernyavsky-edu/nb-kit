package org.nbkit.gen.refactoring

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.squareup.kotlinpoet.TypeSpec
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class RefactoringSupportProviderSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .superclass(DefaultWordsScanner::class)
                .addSuperclassConstructorParameter("%T()", lexerAdapterClass)
                .addSuperclassConstructorParameter("%T.identifiers", tokenTypeClass)
                .addSuperclassConstructorParameter("%T.comments", tokenTypeClass)
                .addSuperclassConstructorParameter("%T.literals", tokenTypeClass)
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
