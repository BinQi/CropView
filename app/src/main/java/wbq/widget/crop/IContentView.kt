package wbq.widget.crop

import android.graphics.Matrix
import android.view.View
import androidx.annotation.UiThread

/**
 *
 *
 * @description
 * @author wubinqi
 * @date 2021/7/26 10:54 上午
 */
interface IContentView {

    @UiThread
    fun getRenderView(): View

    fun getTransform(matrix: Matrix): Matrix

    @UiThread
    fun updateTransform(matrix: Matrix, invalidate: Boolean = true)
}