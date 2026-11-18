// Never compiled. `check` runs detekt on this file and fails unless every marked line is reported,
// so the bans on raw colors and sizes in UI code are proven to fire (HEM-15).
package io.github.ctorres345.hemisphere.feature.browser.fixture

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color // expect: ForbiddenImport
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp // expect: ForbiddenImport
import androidx.compose.ui.unit.em // expect: ForbiddenImport
import androidx.compose.ui.unit.sp // expect: ForbiddenImport

val rawColor = Color(0xFF000000) // expect: ForbiddenMethodCall
val rawDp = Dp(16f) // expect: ForbiddenMethodCall
val rawTextUnit = TextUnit(14f, TextUnitType.Sp) // expect: ForbiddenMethodCall
val rawSizes = listOf(4.dp, 12.sp, 1.em)
val rawRadius = RoundedCornerShape(8) // expect: ForbiddenMethodCall
val rawRadiusPx = RoundedCornerShape(8f) // expect: ForbiddenMethodCall

@Composable
fun resourceColor() = colorResource(android.R.color.black) // expect: ForbiddenMethodCall

@Composable
fun resourceSize() = dimensionResource(android.R.dimen.app_icon_size) // expect: ForbiddenMethodCall
