package com.bober.cubecollision

import android.annotation.SuppressLint
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.bober.cubecollision.ui.theme.CubeCollisionTheme
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sign

class MainActivity : ComponentActivity() {
    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CubeCollisionTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) {
                    Main()
                }
            }
        }
    }
}

@Composable
fun Main() {
    val context = LocalContext.current

    val metersToPx = 100f // 1 meter = 100 pixels
    val renderDt = 0.016f      // 60 FPS render
    val physicsDt = 0.002f
    val steps = (renderDt / physicsDt).toInt()
    val g = 9.81f // gravity

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var cube1 by remember { mutableStateOf(Cube(x = 200f, velocity = 0f, mass = 10f)) }
    var cube2 by remember { mutableStateOf(Cube(x = 600f, velocity = 0f, mass = 10f)) }

    var v1Text by remember { mutableStateOf(cube1.velocity.toString()) }
    var v2Text by remember { mutableStateOf(cube2.velocity.toString()) }

    var m1Text by remember { mutableStateOf(cube1.mass.toString()) }
    var m2Text by remember { mutableStateOf(cube2.mass.toString()) }

    var fricText by remember { mutableStateOf("0.2") }
    var mu by remember { mutableStateOf(0f) } // friction coefficient

    var collisionCounter by remember { mutableStateOf(0) }

    var started: Boolean by remember { mutableStateOf(false) }

    val canvasWidthMeters = canvasSize.width / metersToPx

    // Make cube size relative to mass and other cube properties
    val minSize = 40f
    val maxSize = 200f
    val totalMass = cube1.mass + cube2.mass

    val cubeSize1 = if (totalMass > 0) ((cube1.mass / totalMass) * maxSize).coerceIn(
        minSize,
        maxSize
    ) else minSize
    val cubeSize2 = if (totalMass > 0) ((cube2.mass / totalMass) * maxSize).coerceIn(
        minSize,
        maxSize
    ) else minSize

    var lengthCube1Text by remember { mutableStateOf(((cube1.x + cubeSize1 / 2) / metersToPx).toString()) }
    var lengthCube2Text by remember { mutableStateOf(((cube2.x + cubeSize2 / 2) / metersToPx).toString()) }

    fun String.robustToFloat(): Float? = this.replace(',', '.').toFloatOrNull()
    fun Float.format(): String = String.format(Locale.US, "%.2f", this)

    fun applyFriction(v: Float, mu: Float, dt: Float): Float {
        val sign = v.sign
        val a = mu * g
        val vNew = v - sign * a * dt
        return if (v * vNew < 0) 0f else vNew
    }

    LaunchedEffect(started) {
        while (started) {

            repeat(steps) {
                // Position update
                cube1 = cube1.copy(x = cube1.x + cube1.velocity * physicsDt * metersToPx)
                cube2 = cube2.copy(x = cube2.x + cube2.velocity * physicsDt * metersToPx)

                // Collision with walls
                if (cube1.x < 0f) cube1 = cube1.copy(x = 0f, velocity = abs(cube1.velocity))
                else if (cube1.x + cubeSize1 > canvasSize.width) cube1 =
                    cube1.copy(
                        x = canvasSize.width.toFloat() - cubeSize1,
                        velocity = -abs(cube1.velocity)
                    )

                if (cube2.x < 0f) cube2 = cube2.copy(x = 0f, velocity = abs(cube2.velocity))
                else if (cube2.x + cubeSize2 > canvasSize.width) cube2 =
                    cube2.copy(
                        x = canvasSize.width.toFloat() - cubeSize2,
                        velocity = -abs(cube2.velocity)
                    )

                // Collision with each other
                if  (
                    cube1.x < cube2.x + cubeSize2 &&
                    cube1.x + cubeSize1 > cube2.x &&
                    cube1.velocity > cube2.velocity
                ) {
                    val m1 = cube1.mass
                    val m2 = cube2.mass
                    val v1 = cube1.velocity
                    val v2 = cube2.velocity

                    val v1New = ((m1 - m2) * v1 + 2 * m2 * v2) / (m1 + m2)
                    val v2New = ((m2 - m1) * v2 + 2 * m1 * v1) / (m1 + m2)

                    cube1 = cube1.copy(velocity = v1New)
                    cube2 = cube2.copy(velocity = v2New)

                    // We check if the cubes overlap we move them
                    val overlap = (cube1.x + cubeSize1) - cube2.x
                    cube1 = cube1.copy(x = cube1.x - overlap / 2)
                    cube2 = cube2.copy(x = cube2.x + overlap / 2)

                    collisionCounter++
                    MediaPlayer.create(context, R.raw.collision).start()
                }

                // Friction
                val threshold = 0.01f
                cube1 = cube1.copy(
                    velocity = if (abs(cube1.velocity) < threshold) 0f else applyFriction(
                        cube1.velocity,
                        mu,
                        physicsDt
                    )
                )
                cube2 = cube2.copy(
                    velocity = if (abs(cube2.velocity) < threshold) 0f else applyFriction(
                        cube2.velocity,
                        mu,
                        physicsDt
                    )
                )
            }

            // Text fields update
            v1Text = cube1.velocity.format()
            v2Text = cube2.velocity.format()
            lengthCube1Text = ((cube1.x + cubeSize1 / 2) / metersToPx).format()
            lengthCube2Text = ((cube2.x + cubeSize2 / 2) / metersToPx).format()

            delay(16)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Top simulation area
        Row(
            modifier = Modifier
                .weight(0.55f)
                .fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size
                    }
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val groundY = canvasHeight * 0.8f

                val paintSize = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 80f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 35f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                drawContext.canvas.nativeCanvas.drawText(
                    if (cube1.velocity < 0) "<- ${abs(ceil(cube1.velocity * 100) / 100)}" else if (cube1.velocity > 0) "${
                        abs(
                            ceil(cube1.velocity * 100) / 100
                        )
                    } ->" else "",
                    cube1.x + cubeSize1 / 2,
                    groundY - cubeSize1 - 40f,
                    paint
                )
                drawContext.canvas.nativeCanvas.drawText(
                    "${ceil((cube1.x + cubeSize1 / 2) / metersToPx * 100) / 100} m",
                    cube1.x + cubeSize1 / 2,
                    groundY + 50f,
                    paint
                )

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(cube1.x, groundY - cubeSize1 + 4f),
                    size = Size(cubeSize1, cubeSize1)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    if (cube2.velocity < 0) "<- ${abs(ceil(cube2.velocity * 100) / 100)}" else if (cube2.velocity > 0) "${
                        abs(
                            ceil(cube2.velocity * 100) / 100
                        )
                    } ->" else "",
                    cube2.x + cubeSize2 / 2,
                    groundY - cubeSize2 - 40f,
                    paint
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "${ceil((cube2.x + cubeSize2 / 2) / metersToPx * 100) / 100} m",
                    cube2.x + cubeSize2 / 2,
                    groundY + 50f,
                    paint
                )

                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(cube2.x, groundY - cubeSize2 + 4f),
                    size = Size(cubeSize2, cubeSize2)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "<- $canvasWidthMeters meters ->",
                    canvasWidth / 2,
                    canvasHeight - 80f,
                    paintSize
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "Collision counter: $collisionCounter",
                    canvasWidth / 2,
                    100f,
                    paintSize
                )

                drawLine(
                    color = Color.Black,
                    start = Offset(0f, groundY),
                    end = Offset(canvasWidth, groundY),
                    strokeWidth = 8f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, canvasHeight),
                    end = Offset(canvasWidth, canvasHeight),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(0f, canvasHeight),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(0f, 0f),
                    end = Offset(canvasWidth, 0f),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color.Black,
                    start = Offset(canvasWidth, 0f),
                    end = Offset(canvasWidth, canvasHeight),
                    strokeWidth = 4f
                )
                for (i in 0..canvasWidthMeters.toInt()) {
                    drawLine(
                        color = Color.Black,
                        start = Offset(i * metersToPx, groundY),
                        end = Offset(i * metersToPx, groundY + 20f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .weight(0.45f)
                .fillMaxWidth()
                .background(Color.Gray)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {

                Column(
                    modifier = Modifier.weight(0.6f)
                ) {

                    Text("Velocity 1 (m/s)")
                    TextField(
                        value = v1Text,
                        onValueChange = {
                            v1Text = it
                            it.robustToFloat()?.let { v ->
                                cube1 = cube1.copy(velocity = v)
                            }
                        },
                        enabled = !started
                    )

                    Text("Velocity 2 (m/s)")
                    TextField(
                        value = v2Text,
                        onValueChange = {
                            v2Text = it
                            it.robustToFloat()?.let { v ->
                                cube2 = cube2.copy(velocity = v)
                            }
                        },
                        enabled = !started
                    )
                }

                Spacer(modifier = Modifier.padding(4.dp))

                Column(
                    modifier = Modifier.weight(0.4f)
                ) {

                    Text("Mass 1 (kg)")
                    TextField(
                        value = m1Text,
                        onValueChange = {
                            m1Text = it
                            it.robustToFloat()?.let { m ->
                                cube1 = cube1.copy(mass = m)
                            }
                        },
                        enabled = !started
                    )

                    Text("Mass 2 (kg)")
                    TextField(
                        value = m2Text,
                        onValueChange = {
                            m2Text = it
                            it.robustToFloat()?.let { m ->
                                cube2 = cube2.copy(mass = m)
                            }
                        },
                        enabled = !started
                    )
                }
            }

            Row() {
                Column(
                    modifier = Modifier.weight(0.3f)
                ) {
                    Text("Friction (μ)")
                    TextField(
                        value = fricText,
                        onValueChange = {
                            fricText = it
                        },
                        enabled = !started
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Column(
                    modifier = Modifier.weight(0.3f)
                ) {
                    Text("Position 1 (m)")
                    TextField(
                        value = lengthCube1Text,
                        onValueChange = {
                            lengthCube1Text = it
                            it.robustToFloat()?.let { l ->
                                // Ensures the cube is within the canvas bounds and not overlapping the other cube
                                val newX = (l * metersToPx - cubeSize1 / 2).coerceIn(0f, (canvasSize.width - cubeSize1).coerceAtLeast(0f))
                                if (newX + cubeSize1 <= cube2.x || newX >= cube2.x + cubeSize2) {
                                    cube1 = cube1.copy(x = newX)
                                }
                            }
                        },
                        enabled = !started
                    )
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Column(
                    modifier = Modifier.weight(0.3f)
                ) {
                    Text("Position 2 (m)")
                    TextField(
                        value = lengthCube2Text,
                        onValueChange = {
                            lengthCube2Text = it
                            it.robustToFloat()?.let { l ->
                                // Ensures the cube is within the canvas bounds and not overlapping the other cube
                                val newX = (l * metersToPx - cubeSize2 / 2).coerceIn(0f, (canvasSize.width - cubeSize2).coerceAtLeast(0f))
                                if (newX + cubeSize2 <= cube1.x || newX >= cube1.x + cubeSize1) {
                                    cube2 = cube2.copy(x = newX)
                                }
                            }
                        },
                        enabled = !started
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = {
                        cube1 = Cube(x = 200f, velocity = 0f, mass = 10f)
                        cube2 = Cube(x = 600f, velocity = 0f, mass = 10f)
                        v1Text = "0.0"
                        v2Text = "0.0"
                        m1Text = "10.0"
                        m2Text = "10.0"
                        fricText = "0.2"
                        lengthCube1Text = "2.5"
                        lengthCube2Text = "6.5"
                        collisionCounter = 0
                        started = false
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                ) {
                    Text("Reset")
                }

                Button(
                    onClick = {
                        if (started) {
                            started = false
                            return@Button
                        }

                        // Validate all numerical inputs first
                        val l1 = lengthCube1Text.robustToFloat() ?: 2.5f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val l2 = lengthCube2Text.robustToFloat() ?: 6.5f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val v1 = v1Text.robustToFloat() ?: 0f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val v2 = v2Text.robustToFloat() ?: 0f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val m1 = m1Text.robustToFloat() ?: 1f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val m2 = m2Text.robustToFloat() ?: 1f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        val f = fricText.robustToFloat() ?: 0f.also {
                            Toast.makeText(context, "Must be a rational number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check cube positions
                        if (cube1.x > cube2.x) {
                            Toast.makeText(context, "Red cube must be on the left", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Check logical constraints
                        if (m1 <= 0f || m2 <= 0f) {
                            Toast.makeText(context, "Masses must be greater than 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (f < 0f) {
                            Toast.makeText(context, "Friction must be greater than 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Calculate target positions and check for placement overlap
                        val targetX1 = (l1 * metersToPx - cubeSize1 / 2).coerceIn(0f, (canvasSize.width - cubeSize1).coerceAtLeast(0f))
                        val targetX2 = (l2 * metersToPx - cubeSize2 / 2).coerceIn(0f, (canvasSize.width - cubeSize2).coerceAtLeast(0f))

                        if (!(targetX1 + cubeSize1 <= targetX2 || targetX1 >= targetX2 + cubeSize2)) {
                            Toast.makeText(context, "Cubes cannot overlap", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Update simulation state with validated values
                        mu = f
                        cube1 = cube1.copy(x = targetX1, velocity = v1, mass = m1)
                        cube2 = cube2.copy(x = targetX2, velocity = v2, mass = m2)
                        
                        started = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp)
                ) {
                    Text(
                        text = if (!started) "Start" else "Stop"
                    )
                }
            }
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .background(Color.Gray)
        )
    }
}

data class Cube(
    var x: Float = 0f,
    var velocity: Float = 0f,
    var mass: Float = 0f
)

@Preview(showBackground = true)
@Composable
fun Preview() {
    CubeCollisionTheme {
        Main()
    }
}
