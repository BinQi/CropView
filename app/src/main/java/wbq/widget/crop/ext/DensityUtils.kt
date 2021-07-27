package wbq.widget.crop.ext

import android.content.res.Resources

/**
 * @description
 * @author wubinqi
 * @date 2021/7/27 8:58 下午
 */
object DensityUtils {
    fun dp2px(dpValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }
}