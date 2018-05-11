package org.nbkit.gen.psi.stubs

import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class StubInterfacesSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        TypeSpec.interfaceBuilder("${fileNamePrefix}NamedStub")
                .addProperty("name", String::class.asTypeName().asNullable())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
