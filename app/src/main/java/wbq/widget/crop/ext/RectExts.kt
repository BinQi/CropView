package wbq.widget.crop.ext

import android.graphics.Matrix
import android.graphics.RectF
import kotlin.math.min

/**
 * @description
 * @author wubinqi
 * @date 2021/7/23 12:55 下午
 */

/**
 * @param dst
 * @return 把[this]居中对齐到[dst]中的变换矩阵
 */
fun RectF.getFitCenterMatrix(dst: RectF): Matrix = Matrix().apply {
    postTranslate(dst.centerX() - centerX(), dst.centerY() - centerY())
    val scale = min(dst.width() / width(), dst.height() / height())
    postScale(scale, scale, dst.centerX(), dst.centerY())
}

/**
 * 把[this]居中对齐到[dst]
 * @param dst
 * @return
 */
fun RectF.fitCenterTo(dst: RectF): RectF = this.apply { getFitCenterMatrix(dst).mapRect(this) }

/**
 * @param dst
 * @return 是否是[dst]的闭包
 */
fun RectF.isClosureTo(dst: RectF): Boolean {
    return left <= dst.left && top <= dst.top && right >= dst.right && bottom >= dst.bottom
}