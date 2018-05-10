package org.nbkit.gen.navigation

import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeSpec
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class NavigationContributorBaseSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeSpec>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
        TypeSpec.classBuilder(className)
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
                .also { addType(it) }

        writeToFile()
    }
}
