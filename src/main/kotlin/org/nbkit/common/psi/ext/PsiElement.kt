package org.nbkit.common.psi.ext

import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.PsiUtilCore

val PsiElement.ancestors: Sequence<PsiElement>
    get() = generateSequence(this) { it.parent }

inline fun <reified T : PsiElement> PsiElement.parentOfType(
        strict: Boolean = true,
        minStartOffset: Int = -1
): T? = PsiTreeUtil.getParentOfType(this, T::class.java, strict, minStartOffset)

inline fun <reified T : PsiElement> PsiElement.childOfType(
        strict: Boolean = true
): T? = PsiTreeUtil.findChildOfType(this, T::class.java, strict)

inline fun <reified T : PsiElement> PsiElement.prevSiblingOfType(): T? =
        PsiTreeUtil.getPrevSiblingOfType(this, T::class.java)

val PsiElement.elementType: IElementType
    get() = PsiUtilCore.getElementType(this)

inline fun <reified T : PsiElement> PsiElement.ancestorOrSelf(): T? =
        PsiTreeUtil.getParentOfType(this, T::class.java, false)

inline fun <reified T : PsiElement> PsiElement.descendantsOfType(): Collection<T> =
        PsiTreeUtil.findChildrenOfType(this, T::class.java)