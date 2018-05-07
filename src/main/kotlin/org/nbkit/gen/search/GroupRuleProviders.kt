package org.nbkit.gen.search

import com.intellij.openapi.project.Project
import com.intellij.usages.PsiNamedElementUsageGroupBase
import com.intellij.usages.Usage
import com.intellij.usages.UsageGroup
import com.intellij.usages.UsageTarget
import com.intellij.usages.impl.FileStructureGroupRuleProvider
import com.intellij.usages.rules.PsiElementUsage
import com.intellij.usages.rules.SingleParentUsageGroupingRule
import com.intellij.usages.rules.UsageGroupingRule
import com.squareup.kotlinpoet.*
import org.nbkit.ScopeRule
import org.nbkit.gen.BaseSpec
import java.nio.file.Path

class GroupRuleProvidersSpec(
        fileNamePrefix: String,
        basePackageName: String,
        genPath: Path,
        scopeRules: List<ScopeRule>
) : BaseSpec(fileNamePrefix, basePackageName, genPath, scopeRules) {
    override fun generate() {
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
                        .also { addType(it) }
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
                        UsageTarget::class,  // ???
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
                .also { addFunction(it) }

        writeToFile()
    }
}
