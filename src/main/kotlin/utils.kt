import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun String.intoTextComponent(modifier: Modifier? = null) = Text(
    text = this,
    modifier = modifier ?: Modifier,
    fontSize = 12.sp
)

operator fun TextUnit.minus(other: TextUnit) = (value - other.value).sp
operator fun TextUnit.div(other: TextUnit) = value / other.value
fun Modifier.border(width: Float, side: BorderSide, color: Color = Color.Gray): Modifier = this.drawBehind {
    when (side) {
        BorderSide.TOP -> drawLine(
            color,
            Offset(0f, size.height + width),
            Offset(size.width, size.height + width),
            width
        )
        BorderSide.BOTTOM -> drawLine(
            color,
            Offset(0f, size.height - width),
            Offset(size.width, size.height - width),
            width
        )
        BorderSide.LEFT -> drawLine(
            color,
            Offset(0f + width, size.height),
            Offset(0f + width, 0f),
            width
        )
        BorderSide.RIGHT -> drawLine(
            color,
            Offset(size.width - width, size.height),
            Offset(size.width - width, 0f),
            width
        )
    }
}

fun File.createIfNot(kind: FileKind): Boolean {
    if (this.exists()) return false

    return when (kind) {
        FileKind.DIR -> this.mkdir()
        FileKind.FILE -> this.createNewFile()
    }
}

enum class FileKind { DIR, FILE }