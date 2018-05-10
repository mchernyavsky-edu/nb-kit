package org.nbkit.gen.search

import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.squareup.kotlinpoet.TypeSpec
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class WordScannerSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
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
