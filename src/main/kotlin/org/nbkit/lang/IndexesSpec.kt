package org.nbkit.lang

import kotlin.reflect.KClass

class IndexesSpec: LangSpec {
    internal val namedElementClasses: MutableSet<KClass<*>> = mutableSetOf()
    internal val definitionClasses: MutableSet<KClass<*>> = mutableSetOf()
    internal val classClasses: MutableSet<KClass<*>> = mutableSetOf()

    fun namedElements(init: NamedElementsIndexSpec.() -> Unit) {
        val builder = NamedElementsIndexSpec()
        builder.init()
        namedElementClasses.addAll(builder.classes)
    }

    fun definitions(init: DefinitionsIndexSpec.() -> Unit) {
        val builder = DefinitionsIndexSpec()
        builder.init()
        definitionClasses.addAll(builder.classes)
    }

    fun classes(init: ClassesIndexSpec.() -> Unit) {
        val builder = ClassesIndexSpec()
        builder.init()
        classClasses.addAll(builder.classes)
    }
}

abstract class IndexSpec(val indexName: String): LangSpec {
    internal val classes: MutableSet<KClass<*>> = mutableSetOf()

    operator fun KClass<*>.unaryPlus() = classes.add(this)
}

class NamedElementsIndexSpec: IndexSpec("NamedElementsIndex")

class DefinitionsIndexSpec: IndexSpec("DefinitionsIndex")

class ClassesIndexSpec: IndexSpec("ClassesIndex")
