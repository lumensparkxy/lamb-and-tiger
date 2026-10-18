package com.maswadkar.lambandtiger

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.maswadkar.lambandtiger.game.*
import kotlin.math.hypot
import kotlin.math.sin

private class BoardHistory(var state: GameState)

/** Use the approved transparent sprite sheet directly, without redrawing the characters. */
private fun DrawScope.animal(bitmap: ImageBitmap, piece: Occupant, center: Offset,
    width: Float, alpha: Float = 1f, halo: Boolean = false) {
    val origin = if (piece == Occupant.LAMB) IntOffset(140, 190) else IntOffset(1040, 215)
    val source = if (piece == Occupant.LAMB) IntSize(730, 530) else IntSize(560, 510)
    val height = width * source.height / source.width
    val destination = IntSize(width.toInt().coerceAtLeast(1), height.toInt().coerceAtLeast(1))
    val offset = IntOffset((center.x - width / 2).toInt(), (center.y - height / 2).toInt())
    if (halo) {
        val edge = 1.dp.toPx().toInt().coerceAtLeast(1)
        listOf(IntOffset(-edge,0), IntOffset(edge,0), IntOffset(0,-edge), IntOffset(0,edge)).forEach {
            drawImage(bitmap, origin, source, offset + it, destination,
                alpha = alpha, colorFilter = ColorFilter.tint(Color(0xFFF7F3EA)))
        }
    }
    drawImage(bitmap, origin, source, offset, destination, alpha = alpha)
}

@Composable
internal fun AnimalIcon(side: Side, modifier: Modifier = Modifier) {
    val atlas = ImageBitmap.imageResource(R.drawable.animal_pieces)
    Canvas(modifier) {
        animal(atlas, if (side == Side.LAMB) Occupant.LAMB else Occupant.TIGER,
            center, minOf(size.width, size.height))
    }
}

@Composable
internal fun BoardView(
    state: GameState,
    selected: Node?,
    actions: List<Action>,
    hint: Action?,
    onTap: (Node) -> Unit,
    modifier: Modifier,
    last: Action? = null,
    immersive: Boolean = false,
    reducedMotion: Boolean = true,
    motionEnabled: Boolean = true,
    onMotionRunning: (Boolean) -> Unit = {},
) {
    val atlas = ImageBitmap.imageResource(R.drawable.animal_pieces)
    val lines = MaterialTheme.colorScheme.primary
    val paper = MaterialTheme.colorScheme.surface
    val ink = MaterialTheme.colorScheme.onSurface
    val halo = paper.red < .3f
    val label = stringResource(if (immersive) R.string.play_board_label else R.string.board_label)
    val history = remember { BoardHistory(state) }
    // Prepare the initial visual position during composition, before the first draw.
    // Creating it only inside LaunchedEffect briefly painted the destination first.
    val motion = remember(state) {
        if (!reducedMotion && motionEnabled) boardMotion(history.state, state) else null
    }
    val progress = remember(state) { Animatable(if (motion == null) 1f else 0f) }
    SideEffect { history.state = state }
    val report by rememberUpdatedState(onMotionRunning)
    LaunchedEffect(state, reducedMotion, motionEnabled) {
        if (reducedMotion || !motionEnabled) {
            progress.snapTo(1f)
            report(false)
        } else if (motion != null && progress.value < 1f) {
            report(true)
            try { progress.animateTo(1f, tween(motion.durationMillis, easing = LinearEasing)) }
            finally { report(false) }
        } else report(false)
    }
    DisposableEffect(Unit) { onDispose { report(false) } }
    val destinations = actions.filter { it is Action.Place || it.origin == selected }
        .map { it.destination }.toSet()
    val recent = remember(state, last) { last ?: (state.events.lastOrNull() as? Event.Played)?.action }
    Canvas(modifier.semantics { contentDescription = label }
        .pointerInput(state, selected, actions, progress, reducedMotion, motionEnabled) {
            detectTapGestures { tap ->
                if (actions.isNotEmpty() && (progress.value >= 1f || reducedMotion || !motionEnabled)) {
                    val margin = 18.dp.toPx()
                    fun p(n: Node) = boardPoint(size.width.toFloat(), size.height.toFloat(), margin, n, 30.dp.toPx())
                    val node = Node.entries.minBy { hypot(tap.x-p(it).x, tap.y-p(it).y) }
                    if (hypot(tap.x-p(node).x, tap.y-p(node).y) <= 24.dp.toPx()) onTap(node)
                }
            }
        }) {
        val margin = 18.dp.toPx()
        val scale = minOf((size.width-2*margin)/10, (size.height-60.dp.toPx())/8).coerceAtLeast(0f)
        val diameter = minOf(24.dp.toPx(), scale * .61f)
        fun p(n: Node) = boardPoint(size.width, size.height, margin, n, 30.dp.toPx())
        val active = motion?.takeIf { progress.value < 1f && !reducedMotion && motionEnabled }
        val action = active?.action
        val t = progress.value.coerceIn(0f,1f)
        Board.edges.forEach { drawLine(lines, p(it.from), p(it.to), 1.5.dp.toPx()) }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb((ink.red*255).toInt(), (ink.green*255).toInt(), (ink.blue*255).toInt())
            textSize = 11.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
        }
        Node.entries.forEach { node ->
            val c=p(node)
            val available = node in destinations && state.position.at(node) == Occupant.EMPTY
            val radius = (if (available) 6.dp else 4.5.dp).toPx()
            drawCircle(paper, radius, c)
            if (available) drawCircle(lines.copy(alpha=.08f), radius, c)
            drawCircle(lines, radius, c, style=Stroke(1.5.dp.toPx()))
            if (node == recent?.origin || recent is Action.Capture && node == recent.over)
                drawCircle(lines.copy(alpha=.8f), 8.dp.toPx(), c,
                    style=Stroke(1.4.dp.toPx(),pathEffect=PathEffect.dashPathEffect(floatArrayOf(3.dp.toPx(),3.dp.toPx()))))
            if (node == recent?.destination)
                drawCircle(lines.copy(alpha=.8f), diameter/2+3.dp.toPx(), c,style=Stroke(1.4.dp.toPx()))
            if (node == selected || node == hint?.destination)
                drawCircle(lines, diameter/2+4.dp.toPx(), c,style=Stroke(2.5.dp.toPx()))
            if (actions.any { it is Action.Capture && it.origin == selected && it.to == node }) {
                val r=3.dp.toPx()
                drawLine(lines,c-Offset(r,0f),c+Offset(r,0f),1.8.dp.toPx())
                drawLine(lines,c-Offset(0f,r),c+Offset(0f,r),1.8.dp.toPx())
            }
            if (fontScale <= 1.3f)
                drawContext.canvas.nativeCanvas.drawText(node.name,c.x,c.y+diameter/2+12.dp.toPx(),paint)
            val piece = state.position.at(node)
            if (piece != Occupant.EMPTY && node != action?.destination)
                animal(atlas,piece,c,diameter,halo=halo)
        }
        if (active != null && action != null) {
            val destination=p(action.destination)
            when(action) {
                is Action.Place -> {
                    val size = if(t<.68f) .86f+.17f*(1-(1-t/.68f)*(1-t/.68f)) else 1.03f-.03f*((t-.68f)/.32f)
                    animal(atlas,Occupant.LAMB,destination,diameter*size,(t*4).coerceAtMost(1f),halo)
                    drawCircle(lines.copy(alpha=(1-t)*.35f),diameter/2+3.dp.toPx()+t*8.dp.toPx(),destination,style=Stroke(1.5.dp.toPx()))
                }
                is Action.Step -> {
                    val eased=1-(1-t)*(1-t)*(1-t)
                    animal(atlas,active.before.position.at(action.from),p(action.from)+(destination-p(action.from))*eased,diameter,halo=halo)
                }
                is Action.Capture -> {
                    val fade=1-((t-.48f)/.3f).coerceIn(0f,1f)
                    if(fade>0) animal(atlas,Occupant.LAMB,p(action.over),diameter*(.9f+.1f*fade),fade,halo)
                    val travel=t*t*(3-2*t)
                    val arc=sin(Math.PI.toFloat()*t)
                    animal(atlas,Occupant.TIGER,p(action.from)+(destination-p(action.from))*travel-Offset(0f,7.dp.toPx()*arc),diameter*(1+.05f*arc),halo=halo)
                }
            }
        }
    }
}

private fun boardPoint(width: Float,height: Float,margin: Float,node: Node,verticalMargin: Float): Offset {
    val scale=minOf((width-2*margin)/10,(height-2*verticalMargin)/8).coerceAtLeast(0f)
    val point=Board.points.getValue(node)
    return Offset(width/2+point.x*scale,height/2+(point.y-4)*scale)
}
