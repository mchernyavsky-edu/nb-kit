package org.nbkit.gen.psi.stubs

import com.intellij.psi.PsiFile
import com.intellij.psi.StubBuilder
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IStubFileElementType
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class StubImplementationsSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override val className: String = this::class.simpleName?.removeSuffix("Spec") ?: error("Class name is null")

    override fun generate() {
        val typeClass = ClassName("", "Type")
        val definitionStubClass = ClassName(packageName, "${fileNamePrefix}DefinitionStub")

        TypeSpec.classBuilder("${fileNamePrefix}FileStub")
                .superclass(ParameterizedTypeName.get(PsiFileStubImpl::class.asClassName(), fileClass))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("file", fileClass.asNullable())
                        .build())
                .addSuperclassConstructorParameter("file")
                .addFunction(FunSpec.builder("getType")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(typeClass)
                        .addStatement("return %T", typeClass)
                        .build())
                .companionObject(TypeSpec.companionObjectBuilder("Type")
                        .superclass(ParameterizedTypeName.get(
                                IStubFileElementType::class.asClassName(),
                                fileStubClass
                        ))
                        .addSuperclassConstructorParameter("%T", languageClass)
                        .addFunction(FunSpec.builder("getStubVersion")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(Int::class)
                                .addStatement("return 1")
                                .build())
                        .addFunction(FunSpec.builder("getBuilder")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(StubBuilder::class)
                                .addStatement(
                                        """
                                            return object : %T() {
                                                override fun createStubForFile(file: %T): %T =
                                                    %T(file as %T)
                                            }
                                        """.trimIndent(),
                                        DefaultStubBuilder::class,
                                        PsiFile::class,
                                        ParameterizedTypeName.get(
                                                StubElement::class.asClassName(),
                                                WildcardTypeName.subtypeOf(ANY)
                                        ),
                                        fileStubClass,
                                        fileClass
                                )
                                .build())
                        .addFunction(FunSpec.builder("serialize")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("stubs", fileStubClass)
                                .addParameter("dataStream", StubOutputStream::class)
                                .build())
                        .addFunction(FunSpec.builder("deserialize")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("dataStream", StubInputStream::class)
                                .addParameter("parentStub", ParameterizedTypeName.get(
                                        StubElement::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY)
                                ).asNullable())
                                .returns(fileStubClass)
                                .addStatement("return %T(null)", fileStubClass)
                                .build())
                        .addFunction(FunSpec.builder("getExternalId")
                                .addModifiers(KModifier.OVERRIDE)
                                .returns(String::class)
                                .addStatement("return \"$basePackageName.file\"")
                                .build())
                        .build())
                .build()
                .also { addType(it) }

        FunSpec.builder("factory")
                .addParameter("name", String::class)
                .returns(ParameterizedTypeName.get(
                        stubElementTypeClass,
                        WildcardTypeName.subtypeOf(ANY),
                        WildcardTypeName.subtypeOf(ANY)
                ))
                .addStatement(
                        buildString {
                            append("return when (name) {\n")
                            for (className in definitionNames) {
                                append("    \"${className.commonName.toUpperCase()}\" -> %T.Type\n")
                            }
                            append("    else -> error(\"Unknown element: \" + name)\n")
                            append("}\n")
                        }.trimMargin(),
                        *definitionNames
                                .map { ClassName("$basePackageName.psi.stubs", "${it.simpleName()}Stub") }
                                .toTypedArray()
                )
                .build()
                .also { addFunction(it) }

        TypeSpec.classBuilder("${fileNamePrefix}DefinitionStub")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(defType.withBounds(elementClass))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("parent", ParameterizedTypeName.get(
                                StubElement::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY)
                        ).asNullable())
                        .addParameter("elementType", ParameterizedTypeName.get(
                                IStubElementType::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .addParameter(ParameterSpec.builder("name", String::class.asTypeName().asNullable())
                                .build())
                        .build())
                .superclass(ParameterizedTypeName.get(StubBase::class.asClassName(), defType))
                .addSuperclassConstructorParameter("parent")
                .addSuperclassConstructorParameter("elementType")
                .addSuperinterface(namedStubClass)
                .addProperty(PropertySpec.builder("name", String::class.asTypeName().asNullable())
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer("name")
                        .build())
                .build()
                .also { addType(it) }

        for (className in definitionNames) {
            val stubClass = ClassName("$basePackageName.psi.stubs", "${className.simpleName()}Stub")
            val implClass = ClassName("$basePackageName.psi.impl", "${className.simpleName()}Impl")

            TypeSpec.classBuilder("${className.simpleName()}Stub")
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("parent", ParameterizedTypeName.get(
                                    StubElement::class.asClassName(),
                                    WildcardTypeName.subtypeOf(ANY)
                            ).asNullable())
                            .addParameter("elementType", ParameterizedTypeName.get(
                                    IStubElementType::class.asClassName(),
                                    WildcardTypeName.subtypeOf(ANY),
                                    WildcardTypeName.subtypeOf(ANY)
                            ))
                            .addParameter("name", String::class.asTypeName().asNullable())
                            .build())
                    .superclass(ParameterizedTypeName.get(definitionStubClass, className))
                    .addSuperclassConstructorParameter("parent")
                    .addSuperclassConstructorParameter("elementType")
                    .addSuperclassConstructorParameter("name")
                    .companionObject(TypeSpec.companionObjectBuilder("Type")
                            .superclass(ParameterizedTypeName.get(
                                    stubElementTypeClass,
                                    stubClass,
                                    className
                            ))
                            .addSuperclassConstructorParameter("\"${className.commonName.toUpperCase()}\"")
                            .addFunction(FunSpec.builder("serialize")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addParameter("stubs", stubClass)
                                    .addParameter("dataStream", StubOutputStream::class)
                                    .addStatement("with(dataStream) { writeName(stubs.name) }")
                                    .build())
                            .addFunction(FunSpec.builder("deserialize")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addParameter("dataStream", StubInputStream::class)
                                    .addParameter("parentStub", ParameterizedTypeName.get(
                                            StubElement::class.asClassName(),
                                            WildcardTypeName.subtypeOf(ANY)
                                    ).asNullable())
                                    .returns(stubClass)
                                    .addStatement("return %T(parentStub, this, dataStream.readName()?.string)", stubClass)
                                    .build())
                            .addFunction(FunSpec.builder("createPsi")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addParameter("stubs", stubClass)
                                    .returns(className)
                                    .addStatement("return %T(stubs, this)", implClass)
                                    .build())
                            .addFunction(FunSpec.builder("createStub")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addParameter("psi", className)
                                    .addParameter("parentStub", ParameterizedTypeName.get(
                                            StubElement::class.asClassName(),
                                            WildcardTypeName.subtypeOf(ANY)
                                    ).asNullable())
                                    .returns(stubClass)
                                    .addStatement("return %T(parentStub, this, psi.name)", stubClass)
                                    .build())
                            .addFunction(FunSpec.builder("indexStub")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .addParameter("stubs", stubClass)
                                    .addParameter("sink", IndexSink::class)
                                    .addStatement("sink.index${className.commonName}(stubs)")
                                    .build())
                            .build())
                    .build()
                    .also { addType(it) }
        }

        writeToFile()
    }
}
