package org.nbkit

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.ide.projectView.PresentationData
import com.intellij.lang.ASTNode
import com.intellij.lang.HelpID
import com.intellij.lang.cacheBuilder.DefaultWordsScanner
import com.intellij.lang.cacheBuilder.WordsScanner
import com.intellij.lang.findUsages.FindUsagesProvider
import com.intellij.lang.refactoring.NamesValidator
import com.intellij.lang.refactoring.RefactoringSupportProvider
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.*
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IStubFileElementType
import com.intellij.usages.PsiNamedElementUsageGroupBase
import com.intellij.usages.Usage
import com.intellij.usages.UsageGroup
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.FileStructureGroupRuleProvider
import com.intellij.usages.rules.PsiElementUsage
import com.intellij.usages.rules.SingleParentUsageGroupingRule
import com.intellij.usages.rules.UsageGroupingRule
import com.squareup.kotlinpoet.*
import org.nbkit.common.resolve.EmptyScope
import org.nbkit.common.resolve.LocalScope
import org.nbkit.common.resolve.OverridingScope
import org.nbkit.common.resolve.Scope
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

class NameBindingSpec(
        private val fileNamePrefix: String,
        private val basePackageName: String,
        private val genPath: Path = Paths.get("src/gen_nb")
) {
    // Basic
    private val elementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}Element")
    private val elementImplClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}ElementImpl")
    private val elementTypesClass = ClassName("$basePackageName.psi", "${fileNamePrefix}ElementTypes")
    private val tokenTypeClass = ClassName("$basePackageName.psi", "${fileNamePrefix}TokenType")
    private val referenceElementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}ReferenceElement")
    private val namedElementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}NamedElement")
    private val languageClass = ClassName(basePackageName, "${fileNamePrefix}Language")
    private val lexerAdapterClass = ClassName("$basePackageName.lexer", "${fileNamePrefix}LexerAdapter")
    private val namesValidatorClass = ClassName("$basePackageName.refactoring", "${fileNamePrefix}NamesValidator")
    private val wordScannerClass = ClassName("$basePackageName.search", "${fileNamePrefix}WordScanner")
    private val psiFactoryClass = ClassName("$basePackageName.psi", "${fileNamePrefix}PsiFactory")
    private val navigationContributorBaseClass = ClassName("$basePackageName.navigation", "${fileNamePrefix}NavigationContributorBase")
    private val getPresentationFunction = ClassName("$basePackageName.navigation", "getPresentation")

    // Resolve
    private val referenceClass = ClassName("$basePackageName.resolve", "${fileNamePrefix}Reference")
    private val scopeProviderClass = ClassName("$basePackageName.resolve", "ScopeProvider")
    private val namespaceProviderClass = ClassName("$basePackageName.resolve", "NamespaceProvider")

    // Stubs
    private val fileStubClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}FileStub")
    private val stubElementTypeClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}StubElementType")
    private val namedStubClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}NamedStub")
    private val stubbedElementImplClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}StubbedElementImpl")

    // Indexes
    private val namedElementIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}NamedElementIndex")
    private val definitionIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}DefinitionIndex")
    private val gotoClassIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}GotoClassIndex")

    // Files
    private val fileClass = ClassName("$basePackageName.psi", "${fileNamePrefix}File")
    private val fileTypeClass = ClassName(basePackageName, "${fileNamePrefix}FileType")
    private val fileWrapperClass = ClassName("$basePackageName.psi", "${fileNamePrefix}FileWrapper")
    private val directoryWrapperClass = ClassName("$basePackageName.psi", "${fileNamePrefix}DirectoryWrapper")

    private val scopeRules: MutableList<ScopeRule> = mutableListOf()

    fun addScopeRule(scopeRule: ScopeRule): NameBindingSpec {
        scopeRules.add(scopeRule)
        return this
    }

    fun generate() {
        generatePsi()
        generateResolving()
        generateRefactoring()
        generateNavigation()
        generateSearch()
    }

    private fun generatePsi() {
        generateExt()
        generateFileWrapper()
        generateDirectoryWrapper()
        generateStubs()
    }

    private fun generateExt() {
        generateElement()
//        generateNamedElement()
        generateReferenceElement()
        generateIdentifiers()
//        generateDefinition()
        for (scopeRule in scopeRules) {
            if (scopeRule.isDefinition) {
                generateDefinitionImplementation(scopeRule.klass.asClassName())
            }
        }
    }

    private fun generateElement() {
        val elementSpec = TypeSpec.interfaceBuilder("${fileNamePrefix}Element")
                .addSuperinterface(PsiElement::class)
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        .returns(referenceClass.asNullable())
                        .build())
                .build()
        val elementImplSpec = TypeSpec.classBuilder("${fileNamePrefix}ElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .build())
                .addSuperinterface(elementClass)
                .superclass(ASTWrapperPsiElement::class)
                .addSuperclassConstructorParameter("node")
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(referenceClass.asNullable())
                        .addStatement("return null")
                        .build())
                .build()
        val stubbedElementImplSpec = TypeSpec.classBuilder("${fileNamePrefix}StubbedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType
                        .withBounds(ParameterizedTypeName.get(
                                StubElement::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY))
                        ))
                .addSuperinterface(elementClass)
                .superclass(ParameterizedTypeName.get(
                        StubBasedPsiElementBase::class.asClassName(),
                        stubType
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubType)
                        .addParameter("nodeType", ParameterizedTypeName.get(
                                IStubElementType::class.asTypeName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .callSuperConstructor("node", "nodeType")
                        .build())
                .addFunction(FunSpec.builder("toString")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return \"\${javaClass.simpleName}(\$elementType)\"")
                        .build())
                .addFunction(FunSpec.builder("getReference")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(referenceClass.asNullable())
                        .addStatement("return null")
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.ext", "${fileNamePrefix}Element")
                .addType(elementSpec)
                .addType(elementImplSpec)
                .addType(stubbedElementImplSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateNamedElement() {
        val namedElementSpec = TypeSpec.interfaceBuilder("${fileNamePrefix}NamedElement")
                .addSuperinterface(elementClass)
                .addSuperinterface(PsiNameIdentifierOwner::class)
                .addSuperinterface(NavigatablePsiElement::class)
                .build()
        val namedElementImplSpec = TypeSpec.classBuilder("${fileNamePrefix}NamedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(namedElementClass)
                .superclass(elementImplClass)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .build())
                .addSuperclassConstructorParameter("node")
                .addFunction(FunSpec.builder("getNameIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(elementImplClass.asNullable())
                        .addStatement("return %T()", childOfTypeFunction)
                        .build())
                .addFunction(FunSpec.builder("getName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class.asTypeName().asNullable())
                        .addStatement("return nameIdentifier?.text")
                        .build())
                .addFunction(FunSpec.builder("setName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .returns(PsiElement::class.asTypeName().asNullable())
                        .addStatement("val factory = %T(project)", psiFactoryClass)
                        .addCode(
                                buildString {
                                    append("val newNameIdentifier = when (nameIdentifier) {\n")
                                    for (scopeRule in scopeRules) {
                                        if (scopeRule.isReferable || scopeRule.isReference) {
                                            val className = scopeRule.klass.asClassName()
                                            append("    is %T -> factory.create${className.commonName}(name)\n")
                                        }
                                    }
                                    append("    else -> return this\n")
                                    append("}")
                                }.trimMargin(),
                                *scopeRules
                                        .filter { it.isReferable || it.isReference }
                                        .map { it.klass.asClassName() }
                                        .toTypedArray()
                        )
                        .addStatement("")
                        .addStatement("nameIdentifier?.replace(newNameIdentifier)")
                        .addStatement("return this")
                        .build())
                .addFunction(FunSpec.builder("getNavigationElement")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(PsiElement::class)
                        .addStatement("return nameIdentifier ?: this")
                        .build())
                .addFunction(FunSpec.builder("getTextOffset")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Int::class)
                        .addStatement("return nameIdentifier?.textOffset ?: super.getTextOffset()")
                        .build())
                .addFunction(FunSpec.builder("getPresentation")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ItemPresentation::class)
                        .addStatement("return %T(this)", getPresentationFunction)
                        .build())
                .build()
        val stubbedNamedElementImplSpec = TypeSpec.classBuilder("${fileNamePrefix}StubbedNamedElementImpl")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType.withBounds(
                        namedStubClass,
                        ParameterizedTypeName.get(
                                StubElement::class.asClassName(),
                                WildcardTypeName.subtypeOf(ANY)
                        )
                ))
                .addSuperinterface(namedElementClass)
                .superclass(ParameterizedTypeName.get(
                        stubbedElementImplClass,
                        stubType
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubType)
                        .addParameter("nodeType", ParameterizedTypeName.get(
                                IStubElementType::class.asTypeName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .callSuperConstructor("node", "nodeType")
                        .build())
                .addFunction(FunSpec.builder("getNameIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(elementImplClass.asNullable())
                        .addStatement("return childOfType()")
                        .build())
                .addFunction(FunSpec.builder("getName")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class.asTypeName().asNullable())
                        .addStatement("return nameIdentifier?.text")
                        .build())
                .addFunction(FunSpec.builder("setName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .returns(PsiElement::class.asTypeName().asNullable())
                        .addStatement("val factory = %T(project)", psiFactoryClass)
                        .addCode(
                                buildString {
                                    append("val newNameIdentifier = when (nameIdentifier) {\n")
                                    for (scopeRule in scopeRules) {
                                        if (scopeRule.isReferable || scopeRule.isReference) {
                                            val className = scopeRule.klass.asClassName()
                                            append("    is %T -> factory.create${className.commonName}(name)\n")
                                        }
                                    }
                                    append("    else -> return this\n")
                                    append("}\n\n")
                                }.trimMargin(),
                                *scopeRules
                                        .filter { it.isReferable || it.isReference }
                                        .map { it.klass.asClassName() }
                                        .toTypedArray()
                        )
                        .addStatement("")
                        .addStatement("nameIdentifier?.replace(newNameIdentifier)")
                        .addStatement("return this")
                        .build())
                .addFunction(FunSpec.builder("getNavigationElement")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(PsiElement::class)
                        .addStatement("return nameIdentifier ?: this")
                        .build())
                .addFunction(FunSpec.builder("getTextOffset")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(Int::class)
                        .addStatement("return nameIdentifier?.textOffset ?: super.getTextOffset()")
                        .build())
                .addFunction(FunSpec.builder("getPresentation")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(ItemPresentation::class)
                        .addStatement("return %T(this)", getPresentationFunction)
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.ext", "${fileNamePrefix}NamedElement")
                .addType(namedElementSpec)
                .addType(namedElementImplSpec)
                .addType(stubbedNamedElementImplSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateReferenceElement() {
        val referenceElementSpec = TypeSpec.interfaceBuilder("${fileNamePrefix}ReferenceElement")
                .addSuperinterface(elementClass)
                .addProperty("referenceNameElement", elementClass.asNullable())
                .addProperty("referenceName", String::class.asTypeName().asNullable())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.ext", "${fileNamePrefix}ReferenceElement")
                .addType(referenceElementSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateIdentifiers() {
        val file = FileSpec.builder("$basePackageName.psi.ext", "${fileNamePrefix}Identifiers")

        val referableNames = scopeRules.filter { it.isReferable }.map { it.klass.asClassName() }
        for (referableName in referableNames) {
            TypeSpec.classBuilder("${referableName.simpleName()}ImplMixin")
                    .addModifiers(KModifier.ABSTRACT)
                    .addSuperinterface(referableName)
                    .superclass(elementImplClass)
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("node", ASTNode::class)
                            .build())
                    .addSuperclassConstructorParameter("node")
                    .addProperty(PropertySpec.builder(
                            "referenceNameElement",
                            ClassName("$basePackageName.psi.ext", "${referableName.simpleName()}ImplMixin"), KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return this").build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceName",
                            String::class,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return referenceNameElement.text").build())
                            .build())
                    .addFunction(FunSpec.builder("getName")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return referenceName")
                            .build())
                    .build()
                    .also { file.addType(it) }
        }

        val referenceNames = scopeRules.filter { it.isReference }.map { it.klass.asClassName() }
        for (referenceName in referenceNames) {
            TypeSpec.classBuilder("${referenceName.simpleName()}ImplMixin")
                    .addModifiers(KModifier.ABSTRACT)
                    .addSuperinterface(referenceName)
                    .superclass(elementImplClass)
                    .primaryConstructor(FunSpec.constructorBuilder()
                            .addParameter("node", ASTNode::class)
                            .build())
                    .addSuperclassConstructorParameter("node")
                    .addProperty(PropertySpec.builder(
                            "scope",
                            Scope::class)
                            .getter(FunSpec.getterBuilder().addStatement("return %T.getScope(this)", scopeProviderClass).build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceNameElement",
                            elementClass,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return this").build())
                            .build())
                    .addProperty(PropertySpec.builder(
                            "referenceName",
                            String::class,
                            KModifier.OVERRIDE)
                            .getter(FunSpec.getterBuilder().addStatement("return text").build())
                            .build())
                    .addFunction(FunSpec.builder("getName")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(String::class)
                            .addStatement("return referenceName")
                            .build())
                    .addFunction(FunSpec.builder("getReference")
                            .addModifiers(KModifier.OVERRIDE)
                            .returns(referenceClass)
                            .addStatement("return ${fileNamePrefix}IdReference()")
                            .build())
                    .addType(TypeSpec.classBuilder("${fileNamePrefix}IdReference")
                            .addModifiers(KModifier.PRIVATE)
                            .addModifiers(KModifier.INNER)
                            .superclass(ParameterizedTypeName.get(
                                    ClassName("$basePackageName.resolve", "${fileNamePrefix}ReferenceBase"),
                                    referenceName
                            ))
                            .addSuperclassConstructorParameter("this@${referenceName.simpleName()}ImplMixin")
                            .addFunction(FunSpec.builder("resolve")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(PsiElement::class.asClassName().asNullable())
                                    .addStatement("return scope.resolve(name)")
                                    .build())
                            .addFunction(FunSpec.builder("getVariants")
                                    .addModifiers(KModifier.OVERRIDE)
                                    .returns(ParameterizedTypeName.get(Array<Any>::class.asClassName(), ANY))
                                    .addStatement("return scope.symbols.toTypedArray()")
                                    .build())
                            .build())
                    .build()
                    .also { file.addType(it) }
        }

        file.build().writeTo(genPath)
    }

    private fun generateDefinition() {
        val definitionMixinSpec = TypeSpec.classBuilder("${fileNamePrefix}DefinitionMixin")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType
                        .withBounds(
                                ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}NamedStub"),
                                ParameterizedTypeName.get(
                                        StubElement::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY)
                                )
                        ))
                .superclass(ParameterizedTypeName.get(
                        ClassName("", "${fileNamePrefix}StubbedNamedElementImpl"),
                        stubType
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubType)
                        .addParameter("nodeType", ParameterizedTypeName.get(
                                IStubElementType::class.asTypeName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .callSuperConstructor("node", "nodeType")
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.ext", "${fileNamePrefix}Definition")
                .addType(definitionMixinSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateDefinitionImplementation(className: ClassName) {
        val stubClass = ClassName("$basePackageName.psi.stubs", "${className.simpleName()}Stub")
        val classMixinSpec = TypeSpec.classBuilder("${className.simpleName()}Mixin")
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(className)
                .superclass(ParameterizedTypeName.get(
                        ClassName("", "${fileNamePrefix}DefinitionMixin"),
                        stubClass
                ))
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", ASTNode::class)
                        .callSuperConstructor("node")
                        .build())
                .addFunction(FunSpec.constructorBuilder()
                        .addParameter("node", stubClass)
                        .addParameter("nodeType", ParameterizedTypeName.get(
                                IStubElementType::class.asTypeName(),
                                WildcardTypeName.subtypeOf(ANY),
                                WildcardTypeName.subtypeOf(ANY)
                        ))
                        .callSuperConstructor("node", "nodeType")
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.ext", className.simpleName())
                .addType(classMixinSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateFileWrapper() {
        val fileWrapperSpec = TypeSpec.classBuilder("${fileNamePrefix}FileWrapper")
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

        val file = FileSpec.builder("$basePackageName.psi", "${fileNamePrefix}FileWrapper")
                .addType(fileWrapperSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateDirectoryWrapper() {
        val directoryWrapperSpec = TypeSpec.classBuilder("${fileNamePrefix}DirectoryWrapper")
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

        val file = FileSpec.builder("$basePackageName.psi", "${fileNamePrefix}DirectoryWrapper")
                .addType(directoryWrapperSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateStubs() {
        generateIndexes()
        generateStubElementType()
        generateStubImplementations()
        generateStubIndexing()
        generateStubInterfaces()
    }

    private fun generateIndexes() {
        generateDefinitionIndex()
        generateGotoClassIndex()
        generateNamedElementIndex()
    }

    private fun generateDefinitionIndex() {
        val definitionIndexSpec = TypeSpec.classBuilder("${fileNamePrefix}DefinitionIndex")
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

        val file = FileSpec.builder("$basePackageName.psi.stubs.index", "${fileNamePrefix}DefinitionIndex")
                .addType(definitionIndexSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateGotoClassIndex() {
        val gotoClassIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}GotoClassIndex")

        val gotoClassIndexSpec = TypeSpec.classBuilder("${fileNamePrefix}GotoClassIndex")
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
                                        StubIndexKey::class, gotoClassIndexClass
                                )
                                .build())
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.stubs.index", "${fileNamePrefix}GotoClassIndex")
                .addType(gotoClassIndexSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateNamedElementIndex() {
        val namedElementIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}NamedElementIndex")

        val namedElementIndexSpec = TypeSpec.classBuilder("${fileNamePrefix}NamedElementIndex")
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
                                        StubIndexKey::class, namedElementIndexClass
                                )
                                .build())
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.stubs.index", "${fileNamePrefix}NamedElementIndex")
                .addType(namedElementIndexSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateStubElementType() {
        val stubElementTypeSpec = TypeSpec.classBuilder("${fileNamePrefix}StubElementType")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(stubType.withBounds(ParameterizedTypeName.get(
                        StubElement::class.asClassName(),
                        WildcardTypeName.subtypeOf(ANY)
                )))
                .addTypeVariable(psiType.withBounds(elementClass))
                .superclass(ParameterizedTypeName.get(
                        IStubElementType::class.asClassName(),
                        stubType,
                        psiType
                ))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("debugName", String::class)
                        .build())
                .addSuperclassConstructorParameter("debugName")
                .addSuperclassConstructorParameter("%T", languageClass)
                .addFunction(FunSpec.builder("getExternalId")
                        .addModifiers(KModifier.FINAL, KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return \"$basePackageName.\${super.toString()}\"")
                        .build())
                .addFunction(FunSpec.builder("createStubIfParentIsStub")
                        .addModifiers(KModifier.PROTECTED)
                        .addParameter("node", ASTNode::class)
                        .returns(Boolean::class)
                        .addCode(
                                """
                                    val parent = node.treeParent
                                    val parentType = parent.elementType
                                    return (parentType is %T && parentType.shouldCreateStub(parent)) || parentType is %T

                                """.trimIndent(),
                                ParameterizedTypeName.get(
                                        IStubElementType::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY),
                                        WildcardTypeName.subtypeOf(ANY)
                                ),
                                ParameterizedTypeName.get(
                                        IStubFileElementType::class.asClassName(),
                                        WildcardTypeName.subtypeOf(ANY)
                                )
                        )
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.stubs", "${fileNamePrefix}StubElementType")
                .addType(stubElementTypeSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateStubImplementations() {
        val typeClass = ClassName("", "Type")
        val definitionStubClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}DefinitionStub")

        val file = FileSpec.builder("$basePackageName.psi.stubs", "StubImplementations")

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
                                .addParameter("stub", fileStubClass)
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
                .also { file.addType(it) }

        val stubbedClassNames = scopeRules
                .filter { it.isStubbed }
                .map { it.klass.asClassName() }

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
                            for (stubClass in stubbedClassNames) {
                                append("    \"${stubClass.commonName.toUpperCase()}\" -> %T.Type\n")
                            }
                            append("    else -> error(\"Unknown element: \" + name)\n")
                            append("}\n")
                        }.trimMargin(),
                        *stubbedClassNames
                                .map { ClassName("$basePackageName.psi.stubs", "${it.simpleName()}Stub") }
                                .toTypedArray()
                )
                .build()
                .also { file.addFunction(it) }

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
                .also { file.addType(it) }

        for (className in stubbedClassNames) {
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
                                    .addParameter("stub", stubClass)
                                    .addParameter("dataStream", StubOutputStream::class)
                                    .addStatement("with(dataStream) { writeName(stub.name) }")
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
                                    .addParameter("stub", stubClass)
                                    .returns(className)
                                    .addStatement("return %T(stub, this)", implClass)
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
                                    .addParameter("stub", stubClass)
                                    .addParameter("sink", IndexSink::class)
                                    .addStatement("sink.index${className.commonName}(stub)")
                                    .build())
                            .build())
                    .build()
                    .also { file.addType(it) }
        }

        file.build().writeTo(genPath)
    }

    private fun generateStubIndexing() {
        val file = FileSpec.builder("$basePackageName.psi.stubs", "StubIndexing")

        FunSpec.builder("indexNamedStub")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stub", namedStubClass)
                .addStatement("stub.name?.let { occurrence(%T.KEY, it) }", namedElementIndexClass)
                .build()
                .also { file.addFunction(it) }
        FunSpec.builder("indexDefinitionStub")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stub", namedStubClass)
                .addStatement("stub.name?.let { occurrence(%T.KEY, it) }", namedElementIndexClass)
                .build()
                .also { file.addFunction(it) }
        FunSpec.builder("indexGotoClass")
                .addModifiers(KModifier.PRIVATE)
                .receiver(IndexSink::class)
                .addParameter("stub", namedStubClass)
                .addStatement("stub.name?.let { occurrence(%T.KEY, it) }", gotoClassIndexClass)
                .build()
                .also { file.addFunction(it) }

        for (scopeRule in scopeRules) {
            if (scopeRule.isStubbed) {
                val className = scopeRule.klass.asClassName()
                val stubClass = ClassName("$basePackageName.psi.stubs", "${className.simpleName()}Stub")
                val indexSpec = FunSpec.builder("index${className.commonName}")
                        .receiver(IndexSink::class)
                        .addParameter("stub", stubClass)
                if (scopeRule.isNamedElement) {
                    indexSpec.addStatement("indexNamedStub(stub)")
                }
                if (scopeRule.isDefinition) {
                    indexSpec.addStatement("indexDefinitionStub(stub)")
                }
                if (scopeRule.isClass) {
                    indexSpec.addStatement("indexGotoClass(stub)")
                }
                file.addFunction(indexSpec.build())
            }
        }

        file.build().writeTo(genPath)
    }

    private fun generateStubInterfaces() {
        val namedStubSpec = TypeSpec.interfaceBuilder("${fileNamePrefix}NamedStub")
                .addProperty("name", String::class.asTypeName().asNullable())
                .build()

        val file = FileSpec.builder("$basePackageName.psi.stubs", "StubInterfaces")
                .addType(namedStubSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateResolving() {
        generateReference()
        generateScopeProvider()
        generateNamespaceProvider()
    }

    private fun generateReference() {
        val referenceSpec = TypeSpec.interfaceBuilder("${fileNamePrefix}Reference")
                .addSuperinterface(PsiReference::class)
                .addFunction(FunSpec.builder("getElement")
                        .addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        .returns(elementClass)
                        .build())
                .addFunction(FunSpec.builder("resolve")
                        .addModifiers(KModifier.OVERRIDE, KModifier.ABSTRACT)
                        .returns(PsiElement::class.asClassName().asNullable())
                        .build())
                .build()
        val referenceBaseSpec = TypeSpec.classBuilder("${fileNamePrefix}ReferenceBase")
                .addModifiers(KModifier.ABSTRACT)
                .addTypeVariable(variableType.withBounds(referenceElementClass))
                .addSuperinterface(referenceClass)
                .superclass(ParameterizedTypeName.get(
                        PsiReferenceBase::class.asClassName(),
                        variableType
                ))
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addParameter("element", variableType)
                        .build())
                .addSuperclassConstructorParameter("element")
                .addSuperclassConstructorParameter("%T(0, element.textLength)", TextRange::class)
                .addFunction(FunSpec.builder("handleElementRename")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("newName", String::class)
                        .returns(PsiElement::class)
                        .addStatement("element.referenceNameElement?.let { doRename(it, newName) }")
                        .addStatement("return element")
                        .build())
                .companionObject(TypeSpec.companionObjectBuilder()
                        .addFunction(FunSpec.builder("doRename")
                                .addModifiers(KModifier.PRIVATE)
                                .addParameter("oldNameIdentifier", PsiElement::class)
                                .addParameter("rawName", String::class)
                                .addStatement("val name = rawName.removeSuffix('.' + %T.defaultExtension)", fileTypeClass)
                                .addStatement("if (!%T().isIdentifier(name, oldNameIdentifier.project)) return", namesValidatorClass)
                                .addStatement("val factory = %T(oldNameIdentifier.project)", psiFactoryClass)
                                .addCode(
                                        buildString {
                                            append("val newNameIdentifier = when (oldNameIdentifier) {\n")
                                            for (scopeRule in scopeRules) {
                                                if (scopeRule.isReferable || scopeRule.isReference) {
                                                    val className = scopeRule.klass.asClassName()
                                                    append("    is %T -> factory.create${className.commonName}(name)\n")
                                                }
                                            }
                                            append("    else -> return\n")
                                            append("}\n")
                                        }.trimMargin(),
                                        *scopeRules
                                                .filter { it.isReferable || it.isReference }
                                                .map { it.klass.asClassName() }
                                                .toTypedArray()
                                )
                                .addStatement("")
                                .addStatement("oldNameIdentifier.replace(newNameIdentifier)")
                                .build())
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.resolve", "${fileNamePrefix}Reference")
                .addType(referenceSpec)
                .addType(referenceBaseSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateScopeProvider() {
        val qualifiedIdPartImplMixinClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}QualifiedIdPartImplMixin")

        val scopeProviderSpec = TypeSpec.objectBuilder("ScopeProvider")
                .addFunction(FunSpec.builder("getScope")
                        .addParameter("id", ClassName("$basePackageName.psi", "${fileNamePrefix}QualifiedIdPart"))
                        .returns(Scope::class)
                        .addStatement("val prev = id.%T()", ParameterizedTypeName.get(prevSiblingOfTypeFunction, qualifiedIdPartImplMixinClass))
                        .addStatement("return forIdentifier(prev, id)")
                        .build())
                .addFunction(FunSpec.builder("forIdentifier")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("id", qualifiedIdPartImplMixinClass.asNullable())
                        .addParameter("from", ClassName("$basePackageName.psi", "${fileNamePrefix}QualifiedIdPart"))
                        .returns(Scope::class)
                        .addCode(
                                """
                                    id ?: return forElement(from.parent, from)
                                    val resolved = id.scope.resolve(id.name)
                                    return when (resolved) {
                                        is %T -> %T.forFile(resolved, true)
                                        is %T -> %T.forDirectory(resolved)
                                        is %T -> %T.forModule(resolved, true)
                                        else -> %T
                                    }

                                """.trimIndent(),
                                fileWrapperClass, namespaceProviderClass,
                                directoryWrapperClass, namespaceProviderClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                namespaceProviderClass,
                                EmptyScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forElement")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", PsiElement::class.asClassName().asNullable())
                        .addParameter("from", PsiElement::class)
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    element ?: return %T()
                                    val useCommand = useCommand && element !is %T
                                    val parentScope = forElement(element.parent, element, useCommand)
                                    val scope = when (element) {
                                        is %T -> forFile(%T(element), useCommand)
                                        is %T -> forModule(element, useCommand)
                                        is %T -> forFunction(element)
                                        is %T -> forLet(element, from)
                                        else -> %T
                                    }
                                    return when {
                                        parentScope.items.isEmpty() -> scope
                                        scope.items.isEmpty() -> parentScope
                                        else -> %T(parentScope, scope)
                                    }

                                """.trimIndent(),
                                LocalScope::class,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Command"),
                                fileClass, fileWrapperClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Function"),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Let"),
                                EmptyScope::class, OverridingScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forFile")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("file", fileWrapperClass)
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val dirNamespace = file.parent?.let { %T.forDirectory(%T(it)) }
                                    val fileNamespace = %T.forFile(file, useCommand)
                                    return when {
                                        dirNamespace == null || dirNamespace.items.isEmpty() -> fileNamespace
                                        fileNamespace.items.isEmpty() -> dirNamespace
                                        else -> %T(dirNamespace, fileNamespace)
                                    }

                                """.trimIndent(),
                                namespaceProviderClass, directoryWrapperClass,
                                namespaceProviderClass,
                                OverridingScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forModule")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("module", ClassName("$basePackageName.psi", "${fileNamePrefix}Module"))
                        .addParameter(ParameterSpec.builder("useCommand", Boolean::class)
                                .defaultValue("true")
                                .build())
                        .returns(Scope::class)
                        .addStatement("return %T.forModule(module, useCommand)", namespaceProviderClass)
                        .build())
                .addFunction(FunSpec.builder("forFunction")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", ClassName("$basePackageName.psi", "${fileNamePrefix}Function"))
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val scope = %T()
                                    scope.put(element.parameter)
                                    return scope

                                """.trimIndent(),
                                LocalScope::class
                        )
                        .build())
                .addFunction(FunSpec.builder("forLet")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", ClassName("$basePackageName.psi", "${fileNamePrefix}Let"))
                        .addParameter("from", PsiElement::class)
                        .returns(Scope::class)
                        .addCode(
                                """
                                    val scope = %T()
                                    val letType = (element.letKw ?: element.letrecKw ?: element.letparKw)?.%T
                                    val bindings = when {
                                        letType === %T.LETREC_KW || from === element.expression ->
                                            element.bindingList
                                        letType === %T.LET_KW ->
                                            element.bindingList.takeWhile { it !== from }
                                        else ->
                                            listOf()
                                    }
                                    bindings.forEach { scope.put(it) }
                                    return scope

                                """.trimIndent(),
                                LocalScope::class, elementTypeProperty,
                                elementTypesClass, elementTypesClass
                        )
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.resolve", "ScopeProvider")
                .addType(scopeProviderSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateNamespaceProvider() {
        val qualifiedIdPartImplMixinClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}QualifiedIdPartImplMixin")

        val namespaceProviderSpec = TypeSpec.objectBuilder("NamespaceProvider")
                .addFunction(FunSpec.builder("forFile")
                        .addParameter("file", fileWrapperClass)
                        .addParameter("withCommands", Boolean::class)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    if (withCommands) {
                                        handleCommands(file, namespace)
                                    }
                                    file.children
                                            .filterIsInstance<%T>()
                                            .mapNotNull { it.definition }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent(),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Statement")
                        )
                        .build())
                .addFunction(FunSpec.builder("forDirectory")
                        .addParameter("directory", directoryWrapperClass)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    directory.files
                                            .filterIsInstance<%T>()
                                            .map { %T(it) }
                                            .forEach { namespace.put(it) }
                                    directory.subdirectories
                                            .map { %T(it) }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent(),
                                fileClass, fileWrapperClass, directoryWrapperClass
                        )
                        .build())
                .addFunction(FunSpec.builder("forModule")
                        .addParameter("module", ClassName("$basePackageName.psi", "${fileNamePrefix}Module"))
                        .addParameter("withCommands", Boolean::class)
                        .addParameter(ParameterSpec.builder("namespace", LocalScope::class)
                                .defaultValue("%T()", LocalScope::class)
                                .build())
                        .returns(Scope::class)
                        .addCode(
                                """
                                    if (withCommands) {
                                        handleCommands(module, namespace)
                                    }
                                    module.statementList
                                            .mapNotNull { it.definition }
                                            .forEach { namespace.put(it) }
                                    return namespace

                                """.trimIndent()
                        )
                        .build())
                .addFunction(FunSpec.builder("handleCommands")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("element", elementClass)
                        .addParameter("namespace", LocalScope::class)
                        .addCode(
                                """
                                    element.children
                                        .filterIsInstance<%T>()
                                        .mapNotNull { it.command }
                                        .map {
                                            it.qualifiedId
                                                    .children
                                                    .filterIsInstance<%T>()
                                                    .last()
                                        }
                                        .map { it.scope.resolve(it.name) }
                                        .mapNotNull {
                                            when (it) {
                                                is %T -> forFile(it, false)
                                                is %T -> forDirectory(it)
                                                is %T -> forModule(it, false)
                                                else -> %T
                                            }
                                        }
                                        .forEach { namespace.putAll(it) }

                                """.trimIndent(),
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Statement"),
                                qualifiedIdPartImplMixinClass,
                                fileWrapperClass, directoryWrapperClass,
                                ClassName("$basePackageName.psi", "${fileNamePrefix}Module"),
                                EmptyScope::class
                        )
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.resolve", "NamespaceProvider")
                .addType(namespaceProviderSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateRefactoring() {
        generateNamesValidator()
        generateRefactoringSupportProvider()
    }

    private fun generateNamesValidator() {
        val namesValidatorSpec = TypeSpec.classBuilder("${fileNamePrefix}NamesValidator")
                .addSuperinterface(NamesValidator::class)
                .addFunction(FunSpec.builder("isKeyword")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .returns(Boolean::class)
                        .addStatement("return getLexerType(name) in %T.keywords", tokenTypeClass)
                        .build())
                .addFunction(FunSpec.builder("isIdentifier")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class)
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .returns(Boolean::class)
                        .addStatement("return getLexerType(name) in %T.identifiers && !containsComment(name)", tokenTypeClass)
                        .build())
                .addFunction(FunSpec.builder("containsComment")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("name", String::class)
                        .returns(Boolean::class)
                        .addStatement("return name.contains(\"#\")")
                        .build())
                .addFunction(FunSpec.builder("getLexerType")
                        .addModifiers(KModifier.PRIVATE)
                        .addParameter("text", String::class)
                        .returns(IElementType::class.asClassName().asNullable())
                        .addStatement("val lexer = %T()", lexerAdapterClass)
                        .addStatement("lexer.start(text)")
                        .addStatement("return if (lexer.tokenEnd == text.length) lexer.tokenType else null")
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.refactoring", "${fileNamePrefix}NamesValidator")
                .addType(namesValidatorSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateRefactoringSupportProvider() {
        val refactoringSupportProviderSpec = TypeSpec.classBuilder("${fileNamePrefix}RefactoringSupportProvider")
                .superclass(RefactoringSupportProvider::class)
                .addFunction(FunSpec.builder("isSafeDeleteAvailable")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("element", PsiElement::class)
                        .returns(Boolean::class)
                        .addStatement("return element is %T", namedElementClass)
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.refactoring", "${fileNamePrefix}RefactoringSupportProvider")
                .addType(refactoringSupportProviderSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateNavigation() {
        generateNavigationUtils()
//        generateNavigationContributorBase()
        generateClassNavigationContributor()
        generateSymbolNavigationContributor()
    }

    private fun generateNavigationUtils() {
        val getPresentationSpec = FunSpec.builder("getPresentation")
                .addParameter("psi", elementClass)
                .returns(ItemPresentation::class)
                .addStatement("val location = \"(in \${psi.containingFile.name})\"")
                .addStatement("val name = presentableName(psi)")
                .addStatement("return %T(name, location, psi.getIcon(0), null)", PresentationData::class)
                .build()
        val presentableNameSpec = FunSpec.builder("presentableName")
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

        val file = FileSpec.builder("$basePackageName.navigation", "Utils")
                .addFunction(getPresentationSpec)
                .addFunction(presentableNameSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateNavigationContributorBase() {
        val navigationContributorBaseSpec = TypeSpec.classBuilder("${fileNamePrefix}NavigationContributorBase")
                .addTypeVariable(variableType.withBounds(NavigationItem::class.asClassName(), namedElementClass))
                .addModifiers(KModifier.ABSTRACT)
                .addSuperinterface(GotoClassContributor::class)
                .primaryConstructor(FunSpec.constructorBuilder()
                        .addModifiers(KModifier.PROTECTED)
                        .addParameter(ParameterSpec.builder(
                                "indexKey",
                                ParameterizedTypeName.get(
                                        StubIndexKey::class.asClassName(),
                                        String::class.asTypeName(),
                                        variableType
                                ))
                                .addModifiers(KModifier.PRIVATE)
                                .build())
                        .addParameter(ParameterSpec.builder(
                                "clazz",
                                ParameterizedTypeName.get(
                                        Class::class.asClassName(),
                                        variableType
                                ))
                                .addModifiers(KModifier.PRIVATE)
                                .build())
                        .build())
                .addProperty(PropertySpec.builder(
                        "indexKey",
                        ParameterizedTypeName.get(
                                StubIndexKey::class.asClassName(),
                                String::class.asTypeName(),
                                variableType
                        ))
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("indexKey")
                        .build())
                .addProperty(PropertySpec.builder(
                        "clazz",
                        ParameterizedTypeName.get(
                                Class::class.asClassName(),
                                variableType
                        ))
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("clazz")
                        .build())
                .addFunction(FunSpec.builder("getNames")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .addParameter("includeNonProjectItems", Boolean::class)
                        .returns(ParameterizedTypeName.get(Array<String>::class, String::class))
                        .addStatement("project ?: return emptyArray()")
                        .addStatement("return %T.getInstance().getAllKeys(indexKey, project).toTypedArray()", StubIndex::class)
                        .build())
                .addFunction(FunSpec.builder("getItemsByName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("name", String::class.asClassName().asNullable())
                        .addParameter("pattern", String::class.asClassName().asNullable())
                        .addParameter("project", Project::class.asClassName().asNullable())
                        .addParameter("includeNonProjectItems", Boolean::class)
                        .returns(ParameterizedTypeName.get(Array<NavigationItem>::class, NavigationItem::class))
                        .addCode(
                                """
                                    if (project == null || name == null) return emptyArray()
                                    val scope = if (includeNonProjectItems) {
                                        %T.allScope(project)
                                    } else {
                                        %T.projectScope(project)
                                    }
                                    return %T.getElements(indexKey, name, project, scope, clazz).toTypedArray()

                                """.trimIndent(),
                                GlobalSearchScope::class, GlobalSearchScope::class, StubIndex::class)
                        .build())
                .addFunction(FunSpec.builder("getQualifiedName")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("item", NavigationItem::class.asClassName().asNullable())
                        .returns(String::class.asTypeName().asNullable())
                        .addStatement("return (item as? %T)?.name", namedElementClass)
                        .build())
                .addFunction(FunSpec.builder("getQualifiedNameSeparator")
                        .addModifiers(KModifier.OVERRIDE)
                        .returns(String::class)
                        .addStatement("return \".\"")
                        .build())
                .build()

        val file = FileSpec.builder("$basePackageName.navigation", "${fileNamePrefix}NavigationContributorBase")
                .addType(navigationContributorBaseSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateClassNavigationContributor() {
        val classNavigationContributorSpec = TypeSpec.classBuilder("${fileNamePrefix}ClassNavigationContributor")
                .superclass(ParameterizedTypeName.get(navigationContributorBaseClass, namedElementClass))
                .addSuperclassConstructorParameter("%T.KEY", gotoClassIndexClass)
                .addSuperclassConstructorParameter("%T::class.java", namedElementClass)
                .build()

        val file = FileSpec.builder("$basePackageName.navigation", "${fileNamePrefix}ClassNavigationContributor")
                .addType(classNavigationContributorSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateSymbolNavigationContributor() {
        val classNavigationContributorSpec = TypeSpec.classBuilder("${fileNamePrefix}SymbolNavigationContributor")
                .superclass(ParameterizedTypeName.get(navigationContributorBaseClass, namedElementClass))
                .addSuperclassConstructorParameter("%T.KEY", namedElementIndexClass)
                .addSuperclassConstructorParameter("%T::class.java", namedElementClass)
                .build()

        val file = FileSpec.builder("$basePackageName.navigation", "${fileNamePrefix}SymbolNavigationContributor")
                .addType(classNavigationContributorSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateSearch() {
        generateWordScanner()
        generateFindUsagesProvider()
        generateGroupRuleProviders()
    }

    private fun generateWordScanner() {
        val wordScannerSpec = TypeSpec.classBuilder("${fileNamePrefix}WordScanner")
                .superclass(DefaultWordsScanner::class)
                .addSuperclassConstructorParameter("%T()", lexerAdapterClass)
                .addSuperclassConstructorParameter("%T.identifiers", tokenTypeClass)
                .addSuperclassConstructorParameter("%T.comments", tokenTypeClass)
                .addSuperclassConstructorParameter("%T.literals", tokenTypeClass)
                .build()

        val file = FileSpec.builder("$basePackageName.search", "${fileNamePrefix}WordScanner")
                .addType(wordScannerSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateFindUsagesProvider() {
        val findUsagesProviderSpec = TypeSpec.classBuilder("${fileNamePrefix}FindUsagesProvider")
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
                                    for (scopeRule in scopeRules) {
                                        if (scopeRule.isDefinition) {
                                            val name = scopeRule.klass.asClassName()
                                            append("    is %T -> \"${name.commonName.toLowerCase()}\"\n")
                                        }
                                    }
                                    append("    else -> \"\"\n")
                                    append("}\n")
                                }.trimMargin(),
                                *scopeRules
                                        .filter { it.isDefinition }
                                        .map { it.klass.asClassName() }
                                        .toTypedArray()
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

        val file = FileSpec.builder("$basePackageName.search", "${fileNamePrefix}FindUsagesProvider")
                .addType(findUsagesProviderSpec)
                .build()
        file.writeTo(genPath)
    }

    private fun generateGroupRuleProviders() {
        val file = FileSpec.builder("$basePackageName.search", "${fileNamePrefix}GroupRuleProviders")

        for (scopeRule in scopeRules) {
            if (scopeRule.isDefinition) {
                val name = scopeRule.klass.asClassName().simpleName()
                TypeSpec.classBuilder("${name}GroupingRuleProvider")
                        .addSuperinterface(FileStructureGroupRuleProvider::class)
                        .addFunction(FunSpec.builder("getUsageGroupingRule")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("project", Project::class)
                                .returns(UsageGroupingRule::class.asClassName().asNullable())
                                .addStatement("return createGroupingRule<%T>()", scopeRule.klass.asClassName())
                                .build())
                        .build()
                        .also { file.addType(it) }
            }
        }

        FunSpec.builder("createGroupingRule")
                .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
                .addTypeVariable(variableType.withBounds(namedElementClass).reified())
                .returns(UsageGroupingRule::class)
                .addStatement(
                        """
                            return object : %T() {
                                override fun getParentGroupFor(usage: %T, targets: %T<out %T>): %T {
                                    if (usage !is %T) return null
                                    val parent = usage.element.%T()
                                    return parent?.let { %T(it) }
                                }
                            }
                        """.trimIndent(),
                        SingleParentUsageGroupingRule::class,
                        Usage::class,
                        Array<out UsageTarget>::class,
                        UsageTarget::class,  // :(
                        UsageGroup::class.asClassName().asNullable(),
                        PsiElementUsage::class,
                        ParameterizedTypeName.get(
                                parentOfTypeFunction,
                                variableType
                        ),
                        ParameterizedTypeName.get(
                                PsiNamedElementUsageGroupBase::class.asClassName(),
                                namedElementClass
                        )
                )
                .build()
                .also { file.addFunction(it) }

        file.build().writeTo(genPath)
    }

    companion object {
        private const val NB_PACKAGE_NAME = "org.nbkit"

        // Generics
        private val variableType = TypeVariableName("T")
        private val stubType = TypeVariableName("StubT")
        private val psiType = TypeVariableName("PsiT")
        private val defType = TypeVariableName("DefT")

        // Extensions
        private val parentOfTypeFunction = ClassName("$NB_PACKAGE_NAME.common.psi.ext", "parentOfType")
        private val prevSiblingOfTypeFunction = ClassName("$NB_PACKAGE_NAME.common.psi.ext", "prevSiblingOfType")
        private val elementTypeProperty = ClassName("$NB_PACKAGE_NAME.common.psi.ext", "elementType")
        private val childOfTypeFunction = ClassName("$NB_PACKAGE_NAME.common.psi.ext", "childOfType")
    }

    private val ClassName.commonName: String
        get() = simpleName().removePrefix(fileNamePrefix)
}

class ScopeRuleSpec(val klass: KClass<*>) {
    private var isNamedElement: Boolean = false
    private var isDefinition: Boolean = false
    private var isClass: Boolean = false
    private var isStubbed: Boolean = false
    private var isReferable: Boolean = false
    private var isReference: Boolean = false

    fun setIsNamedElement(value: Boolean = true): ScopeRuleSpec {
        isNamedElement = value
        return this
    }

    fun setIsDefinition(value: Boolean = true): ScopeRuleSpec {
        isDefinition = value
        return this
    }

    fun setIsClass(value: Boolean = true): ScopeRuleSpec {
        isClass = value
        return this
    }

    fun setIsStubbed(value: Boolean = true): ScopeRuleSpec {
        isStubbed = value
        return this
    }

    fun setIsReferable(value: Boolean = true): ScopeRuleSpec {
        isReferable = value
        return this
    }

    fun setIsReference(value: Boolean = true): ScopeRuleSpec {
        isReference = value
        return this
    }

    fun build(): ScopeRule = ScopeRule(
            klass,
            isNamedElement,
            isDefinition,
            isClass,
            isStubbed,
            isReferable,
            isReference
    )
}

class ScopeRule(
        val klass: KClass<*>,
        val isNamedElement: Boolean,
        val isDefinition: Boolean,
        val isClass: Boolean,
        val isStubbed: Boolean,
        val isReferable: Boolean,
        val isReference: Boolean
)
