package com.bober.cubecollision

import android.annotation.SuppressLint
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
import kotlin.math.abs
import kotlin.math.ceil

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

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }

    var cube1 by remember { mutableStateOf(Cube(x = 200f, velocity = 0f, mass = 1f)) }
    var cube2 by remember { mutableStateOf(Cube(x = 600f, velocity = 0f, mass = 1f)) }

    var v1Text by remember { mutableStateOf(cube1.velocity.toString()) }
    var v2Text by remember { mutableStateOf(cube2.velocity.toString()) }

    var m1Text by remember { mutableStateOf(cube1.mass.toString()) }
    var m2Text by remember { mutableStateOf(cube2.mass.toString()) }

    var started: Boolean by remember { mutableStateOf(false) }

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

    fun applyFriction(v: Float, aF: Float, dt: Float): Float {
        val sign = if (v > 0) 1 else -1
        val vNew = v - sign * aF * dt
        return if (v * vNew < 0) 0f else vNew
    }

    val metersToPx = 100f // 1 meter = 100 pixels
    val dt = 0.016f // 16 ms
    val canvasWidthMeters = canvasSize.width / metersToPx

    LaunchedEffect(started) {
        while (started) {

            // Position update
            cube1 = cube1.copy(x = cube1.x + cube1.velocity * dt * metersToPx)
            cube2 = cube2.copy(x = cube2.x + cube2.velocity * dt * metersToPx)

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
            if (cube1.x + cubeSize1 >= cube2.x && cube1.x <= cube2.x + cubeSize2) {
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
            }

            // Friction
            val frictionAccel = 2f // m/s²
            val threshold = 0.01f
            cube1 = cube1.copy(velocity = if (abs(cube1.velocity) < threshold) 0f else applyFriction(cube1.velocity, frictionAccel, dt))
            cube2 = cube2.copy(velocity = if (abs(cube2.velocity) < threshold) 0f else applyFriction(cube2.velocity, frictionAccel, dt))

            // Text fields update
            v1Text = cube1.velocity.toString()
            v2Text = cube2.velocity.toString()

            delay(16)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        // Top simulation area
        Row(
            modifier = Modifier
                .weight(0.7f)
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
                    textAlign = android.graphics.Paint.Align.LEFT
                }

                drawContext.canvas.nativeCanvas.drawText(
                    if (cube1.velocity < 0) "<- ${abs(ceil(cube1.velocity*100)/100)}" else if (cube1.velocity > 0) "${abs(ceil(cube1.velocity*100)/100)} ->" else "",
                    cube1.x,
                    groundY - cubeSize1 - 40f,
                    paint
                )

                drawRect(
                    color = Color.Red,
                    topLeft = Offset(cube1.x, groundY - cubeSize1 + 4f),
                    size = Size(cubeSize1, cubeSize1)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    if (cube2.velocity < 0) "<- ${abs(ceil(cube2.velocity*100)/100)}" else if (cube2.velocity > 0) "${abs(ceil(cube2.velocity*100)/100)} ->" else "",
                    cube2.x,
                    groundY - cubeSize2 - 40f,
                    paint
                )

                drawRect(
                    color = Color.Blue,
                    topLeft = Offset(cube2.x, groundY - cubeSize2 + 4f),
                    size = Size(cubeSize2, cubeSize2)
                )

                drawContext.canvas.nativeCanvas.drawText(
                    "<- $canvasWidthMeters meters ->",
                    canvasWidth/2,
                    canvasHeight - 100f,
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
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .weight(0.3f)
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
                            it.toFloatOrNull()?.let { v ->
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
                            it.toFloatOrNull()?.let { v ->
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
                            it.toFloatOrNull()?.let { m ->
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
                            it.toFloatOrNull()?.let { m ->
                                cube2 = cube2.copy(mass = m)
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
                        cube1 = Cube(x = 200f, velocity = 0f, mass = 1f)
                        cube2 = Cube(x = 600f, velocity = 0f, mass = 1f)
                        v1Text = "0.0"
                        v2Text = "0.0"
                        m1Text = "1.0"
                        m2Text = "1.0"
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
                        if (cube1.mass <= 0f || cube2.mass <= 0f){
                            Toast.makeText(context, "Masses must be greater than 0", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        started = !started
                        cube1 = cube1.copy(
                            velocity = v1Text.toFloatOrNull() ?: 0f,
                            mass = m1Text.toFloatOrNull() ?: 1f
                        )
                        cube2 = cube2.copy(
                            velocity = v2Text.toFloatOrNull() ?: 0f,
                            mass = m2Text.toFloatOrNull() ?: 1f
                        )
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
