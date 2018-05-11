package org.nbkit.gen

import com.squareup.kotlinpoet.*
import org.nbkit.lang.ScopeRule
import java.nio.file.Path

interface Spec {
    fun generate()
}

abstract class BaseSpec(
        protected val fileNamePrefix: String,
        protected val basePackageName: String,
        protected val genPath: Path,
        protected val scopeRules: List<ScopeRule>
) : Spec {
    protected val namedElementClasses by lazy {
        scopeRules
                .filter { it.isNamedElement }
                .map { it.klass.asClassName() }
    }
    protected val definitionClasses by lazy {
        scopeRules
                .filter { it.isDefinition }
                .map { it.klass.asClassName() }
    }
    protected val classClasses by lazy {
        scopeRules
                .filter { it.isClass }
                .map { it.klass.asClassName() }
    }
    protected val referableClasses by lazy {
        scopeRules
                .filter { it.isReferable }
                .map { it.klass.asClassName() }
    }
    protected val referenceClasses by lazy {
        scopeRules
                .filter { it.isReference }
                .map { it.klass.asClassName() }
    }

    protected open val packageName: String by lazy {
        this::class
                .asClassName()
                .packageName()
                .replace("$NBKIT_PACKAGE_NAME.gen", basePackageName)
    }

    protected open val className: String by lazy {
        "$fileNamePrefix${this::class.simpleName?.removeSuffix("Spec")}"
    }

    // Basic
    protected val elementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}Element")
    protected val elementImplClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}ElementImpl")
    protected val elementTypesClass = ClassName("$basePackageName.psi", "${fileNamePrefix}ElementTypes")
    protected val tokenTypeClass = ClassName("$basePackageName.psi", "${fileNamePrefix}TokenType")
    protected val referenceElementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}ReferenceElement")
    protected val namedElementClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}NamedElement")
    protected val languageClass = ClassName(basePackageName, "${fileNamePrefix}Language")
    protected val lexerAdapterClass = ClassName("$basePackageName.lexer", "${fileNamePrefix}LexerAdapter")
    protected val namesValidatorClass = ClassName("$basePackageName.refactoring", "${fileNamePrefix}NamesValidator")
    protected val wordScannerClass = ClassName("$basePackageName.search", "${fileNamePrefix}WordScanner")
    protected val psiFactoryClass = ClassName("$basePackageName.psi", "${fileNamePrefix}PsiFactory")
    protected val navigationContributorBaseClass = ClassName("$basePackageName.navigation", "${fileNamePrefix}NavigationContributorBase")
    protected val getPresentationFunction = ClassName("$basePackageName.navigation", "getPresentation")

    // Resolve
    protected val referenceClass = ClassName("$basePackageName.resolve", "${fileNamePrefix}Reference")
    protected val scopeProviderClass = ClassName("$basePackageName.resolve", "ScopeProvider")
    protected val namespaceProviderClass = ClassName("$basePackageName.resolve", "NamespaceProvider")

    // Stubs
    protected val fileStubClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}FileStub")
    protected val stubElementTypeClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}StubElementType")
    protected val namedStubClass = ClassName("$basePackageName.psi.stubs", "${fileNamePrefix}NamedStub")
    protected val stubbedElementImplClass = ClassName("$basePackageName.psi.ext", "${fileNamePrefix}StubbedElementImpl")

    // Indexes
    protected val namedElementIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}NamedElementIndex")
    protected val definitionIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}DefinitionIndex")
    protected val gotoClassIndexClass = ClassName("$basePackageName.psi.stubs.index", "${fileNamePrefix}GotoClassIndex")

    // Files
    protected val fileClass = ClassName("$basePackageName.psi", "${fileNamePrefix}File")
    protected val fileTypeClass = ClassName(basePackageName, "${fileNamePrefix}FileType")
    protected val fileWrapperClass = ClassName("$basePackageName.psi", "${fileNamePrefix}FileWrapper")
    protected val directoryWrapperClass = ClassName("$basePackageName.psi", "${fileNamePrefix}DirectoryWrapper")

    // Generics
    protected val variableType = TypeVariableName("T")
    protected val stubType = TypeVariableName("StubT")
    protected val psiType = TypeVariableName("PsiT")
    protected val defType = TypeVariableName("DefT")

    // Extensions
    protected val parentOfTypeFunction = ClassName("$NBKIT_PACKAGE_NAME.common.psi.ext", "parentOfType")
    protected val prevSiblingOfTypeFunction = ClassName("$NBKIT_PACKAGE_NAME.common.psi.ext", "prevSiblingOfType")
    protected val elementTypeProperty = ClassName("$NBKIT_PACKAGE_NAME.common.psi.ext", "elementType")
    protected val descendantOfTypeFunction = ClassName("$NBKIT_PACKAGE_NAME.common.psi.ext", "descendantOfType")

    private val file: FileSpec.Builder by lazy {
        FileSpec.builder(packageName, className)
    }

    protected fun addType(typeSpec: TypeSpec) = file.addType(typeSpec)

    protected fun addFunction(funSpec: FunSpec) = file.addFunction(funSpec)

    protected fun addProperty(propertySpec: PropertySpec) = file.addProperty(propertySpec)

    protected fun writeToFile() = file.build().writeTo(genPath)

    companion object {
        const val NBKIT_PACKAGE_NAME = "org.nbkit"
    }

    protected val ClassName.commonName: String
        get() = simpleName().removePrefix(fileNamePrefix)
}

open class SpecGroup : Spec {
    private val specs: MutableList<Spec> = mutableListOf()

    fun addSpec(spec: Spec): SpecGroup {
        specs.add(spec)
        return this
    }

    override fun generate() = specs.forEach(Spec::generate)
}
