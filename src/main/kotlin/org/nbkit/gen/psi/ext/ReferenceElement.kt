package org.nbkit.gen.psi.ext

import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class ReferenceElementSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.interfaceBuilder(className)
                .addSuperinterface(elementClass)
                .addProperty("referenceNameElement", elementClass.asNullable())
                .addProperty("referenceName", String::class.asTypeName().asNullable())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
