package org.nbkit.gen.psi.stubs.index

import com.intellij.psi.stubs.StringStubIndexExtension
import com.intellij.psi.stubs.StubIndexKey
import com.squareup.kotlinpoet.*
import org.nbkit.lang.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class DefinitionIndexSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .superclass(ParameterizedTypeName.get(StringStubIndexExtension::class.asClassName(), namedElementClass))
                .addFunction(FunSpec.builder("getVersion")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Int::class)
                        .addStatement("return %T.Type.stubVersion", fileStubClass)
                        .build())
                .addFunction(FunSpec.builder("getKey")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ParameterizedTypeName.get(
                                StubIndexKey::class.asClassName(),
                                String::class.asTypeName(),
                                namedElementClass
                        ))
                        .addStatement("return KEY")
                        .build())
                .companionObject(TypeSpec.companionObjectBuilder()
                        .addProperty(PropertySpec.builder("KEY",
                                ParameterizedTypeName.get(
                                        StubIndexKey::class.asClassName(),
                                        String::class.asTypeName(),
                                        namedElementClass
                                ))
                                .initializer(
                                        "%T.createIndexKey(%T::class.java.canonicalName)",
                                        StubIndexKey::class, definitionIndexClass
                                )
                                .build())
                        .build())
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
