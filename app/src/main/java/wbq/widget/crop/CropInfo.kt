package wbq.widget.crop

import android.graphics.RectF
import android.util.SizeF

/**
 * @description 基础裁剪View
 * @author wubinqi
 * @date 2021/7/23 4:46 下午
 *
 * @property mediaFilePath 资源文件路径
 * @property size 资源尺寸
 * @property fillSize 填充尺寸
 * @property clipRect 裁剪尺寸(相对于[size]的坐标)
 */
data class CropInfo(
    val mediaFilePath: String,
    val size: SizeF,
    val fillSize: SizeF,
    val clipRect: RectF
)