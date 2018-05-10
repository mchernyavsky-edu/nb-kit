package org.nbkit.gen.navigation

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeSpec
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class SymbolNavigationContributorSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
                .superclass(ParameterizedTypeName.get(navigationContributorBaseClass, namedElementClass))
                .addSuperclassConstructorParameter("%T.KEY", namedElementIndexClass)
                .addSuperclassConstructorParameter("%T::class.java", namedElementClass)
                .build()
                .also { addType(it) }

        writeToFile()
    }
}
