package com.myproject.radiojourney.presentation.common

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.findNavController

/**
 * Переходы между экранами с проверкой, что мы всё ещё на нужном экране.
 *
 * Зачем проверка: если быстро нажать кнопку два раза, второе нажатие приходит, когда экран уже сменился.
 * navigate() с действием, которого у нового экрана нет, роняет приложение (IllegalArgumentException:
 * "action ... cannot be found from the current destination"), а второй popBackStack() закрыл бы и предыдущий экран.
 * Раньше в каждом фрагменте перед переходом стояло своё условие "if (currentDestination?.id == R.id.этот_экран)"
 */

// Переход по действию из графа навигации (app_navigation.xml) - только если это действие есть у текущего экрана.
// Действие знает, откуда оно ведёт, поэтому id экрана передавать не нужно
fun NavController.navigateSafely(directions: NavDirections) {
    if (currentDestination?.getAction(directions.actionId) != null) navigate(directions)
}

fun NavController.navigateSafely(@IdRes actionId: Int) {
    if (currentDestination?.getAction(actionId) != null) navigate(actionId)
}

fun Fragment.navigateSafely(directions: NavDirections) = findNavController().navigateSafely(directions)

fun Fragment.navigateSafely(@IdRes actionId: Int) = findNavController().navigateSafely(actionId)

// Назад на предыдущий экран (как системная кнопка "Назад") - только если текущий экран - этот фрагмент.
// Экран в графе навигации хранит имя класса своего фрагмента, с ним и сравниваем
fun Fragment.popBackStackSafely() {
    val navController = findNavController()
    val currentDestination = navController.currentDestination as? FragmentNavigator.Destination
    if (currentDestination?.className == this::class.java.name) navController.popBackStack()
}