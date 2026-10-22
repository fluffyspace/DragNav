package com.ingokodba.dragnav.rainbow

import android.graphics.PointF

/**
 * Defines the shape of the path between start and end points
 */
enum class PathShape {
    LINEAR,           // Straight line from start to end
    CURVED,           // Smooth bezier curve
    CURVED_POLYGON,   // Multiple straight segments forming a curved overall shape
    ZIGZAG,           // Sharp back-and-forth around the straight line
    SINE_WAVE,        // Smooth wave around the straight line
    S_CURVE           // Cubic bezier bending one way, then the other
}

/**
 * How the visible line is drawn beneath the apps. The apps themselves always sit on the path shape;
 * ZIGZAG and WAVE make the line swing from side to side between consecutive apps.
 */
enum class LineStyle {
    FOLLOW_PATH,  // Line runs exactly along the path
    ZIGZAG,       // Sharp zigzag between neighbouring apps
    WAVE          // Smooth wave between neighbouring apps
}

/**
 * Configuration for the app path layout
 * All coordinates are normalized (0 to 1) where:
 * - x: 0 = left edge, 1 = right edge
 * - y: 0 = bottom edge, 1 = top edge
 */
data class PathConfig(
    // Path endpoints (normalized 0-1)
    val startPoint: PointF = PointF(0.2f, 0f),      // Default: bottom, left of center
    val endPoint: PointF = PointF(1f, 0.8f),        // Default: right edge, 4/5 up

    // Path shape
    val pathShape: PathShape = PathShape.CURVED,

    // For curved paths, control point offset (0-1, affects curve intensity)
    val curveIntensity: Float = 0.5f,

    // Where along the path (0 = start/bottom, 1 = end/top) icons are spread out or squeezed together
    val spacingFocus: Float = 0.5f,
    // 0 = even spacing; positive spreads icons apart around the focus, negative squeezes them together
    val spacingStrength: Float = 0f,

    // For curved polygon, number of segments
    val polygonSegments: Int = 8,

    // Zigzag / sine wave: sideways swing (normalized) and number of half-waves along the path
    val waveAmplitude: Float = 0.06f,
    val waveCount: Int = 6,

    // Line drawn between apps (null in configs saved before this existed = FOLLOW_PATH)
    val lineStyle: LineStyle? = LineStyle.FOLLOW_PATH,
    val lineSwing: Float = 0.5f,   // Sideways swing between apps, in icon sizes
    val lineFixed: Boolean = false, // true = line stays in place while apps slide over it
    val lineZigzags: Int = 10,     // Swings (peaks) in the line between two neighbouring apps

    // Path line color. pathColor == 0 means "use pathHue" (older saved configs).
    // With pathGradient the line fades from pathColor to pathColor2 along the path.
    val pathColor: Int = 0,
    val pathGradient: Boolean = false,
    val pathColor2: Int = 0,

    // Path line color (hue in degrees, 0-360)
    val pathHue: Float = 0f,  // 0 = red, 120 = green, 240 = blue
    val pathAlpha: Float = 255f,  // Transparency (0 = fully transparent, 255 = fully opaque)
    val pathWidth: Float = 2f,  // Line width in pixels

    // App display settings
    val appIconSize: Float = 0.12f,                  // Size relative to screen width
    val appSpacing: Float = 0.09f,                   // Spacing between apps (0-1 of path length)
    val showAppNames: Boolean = true,
    val appNameSize: Float = 12f,                    // Text size in sp

    // App name positioning and styling
    val appNameOffsetX: Float = 0f,                  // Horizontal offset from icon center (-1 to 1)
    val appNameOffsetY: Float = 0.22f,               // Vertical offset from icon center (-1 to 1); 0.22 = just below the icon
    val appNameStyle: AppNameStyle = AppNameStyle.PLAIN,
    val appNameBorderWidth: Float = 2f,              // Border width in pixels (for BORDERED style)
    val appNameFont: AppNameFont = AppNameFont.DEFAULT,
    val appNameAlignment: AppNameAlignment = AppNameAlignment.CENTER,

    // Favorites button settings (right side, above the search button,
    // kept left of the letter index touch area)
    val favButtonPosition: PointF = PointF(0.76f, 0.19f),  // Position (normalized)
    val favButtonSize: Float = 0.13f,                       // Size relative to screen width

    // Search button settings
    val searchButtonPosition: PointF = PointF(0.76f, 0.10f),  // Position (normalized)
    val searchButtonSize: Float = 0.13f,                       // Size relative to screen width

    // Letter index settings (for fast scrolling)
    val letterIndexEnabled: Boolean = true,
    val letterIndexPosition: LetterIndexPosition = LetterIndexPosition.RIGHT,
    val letterIndexSize: Float = 0.04f,              // Size relative to screen width
    val letterIndexPadding: Float = 0.02f,           // Padding from edge
    val letterIndexPanFromLetters: Float = 0.05f,    // Padding from letters toward edge for touch area (0-0.3)
    val letterIndexHeight: Float = 1.0f,             // Height as fraction of screen (0-1), shrinks from top
    val letterIndexBottomOffset: Float = 0f,         // Offset from bottom of screen (0-0.5)

    // App sorting
    val appSortOrder: AppSortOrder = AppSortOrder.ASCENDING,  // Alphabetical order (A-Z or Z-A)

    // Scroll sensitivity (multiplier for touch movement to scroll distance)
    // 1.0 = normal, >1.0 = faster scrolling (e.g., 10.0 means 1cm touch scrolls 10cm)
    val scrollSensitivity: Float = 1.0f,

    // Elastic physics: icons trail behind the scroll as if tied to each other by rubber bands,
    // and stretch/squash along the path with speed and acceleration
    val physicsEnabled: Boolean = true,
    val physicsStiffness: Float = 0.5f,   // 0 = loose and slow, 1 = tight and snappy
    val physicsDamping: Float = 0.3f,     // 0 = wobbles for long, 1 = settles without overshoot
    val physicsCoupling: Float = 0.5f,     // 0 = icons move independently, 1 = strongly linked neighbours
    val physicsStretch: Float = 0.6f,      // 0 = rigid icons, 1 = strong stretch and squash

    // Draws the tappable area around every app (for tuning touch zones)
    val showTouchBorders: Boolean = false,

    // Notification icons settings
    val notificationIconSize: Float = 48f,           // Icon size in dp
    val notificationIconSpacing: Float = 8f,         // Spacing between icons in dp
    val notificationOffsetX: Float = 0f,             // Horizontal offset from anchor in dp
    val notificationOffsetY: Float = 16f,            // Vertical offset from anchor in dp
    val notificationAnchor: NotificationAnchor = NotificationAnchor.TOP_CENTER  // Anchor position
)

enum class LetterIndexPosition {
    LEFT,
    RIGHT
}

enum class AppSortOrder {
    ASCENDING,   // A to Z
    DESCENDING   // Z to A
}

enum class AppNameStyle {
    PLAIN,       // Plain text
    BORDERED,    // Text with border/stroke
    SHADOW       // Text with shadow
}

enum class AppNameFont {
    DEFAULT,     // System default
    SANS_SERIF,  // Sans-serif font
    SERIF,       // Serif font
    MONOSPACE    // Monospace font
}

enum class AppNameAlignment {
    LEFT,
    CENTER,
    RIGHT
}

enum class NotificationAnchor {
    TOP_LEFT,
    TOP_CENTER,
    TOP_RIGHT,
    CENTER_LEFT,
    CENTER,
    CENTER_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_CENTER,
    BOTTOM_RIGHT
}

/**
 * Interface for custom path shape providers
 * Users can implement this to add new path shapes
 */
interface PathShapeProvider {
    /**
     * Calculate a point along the path
     * @param t Progress along path (0 = start, 1 = end)
     * @param start Start point (normalized)
     * @param end End point (normalized)
     * @param config Path configuration
     * @return Point on the path (normalized coordinates)
     */
    fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF
}

/**
 * Registry for path shape providers
 * Allows adding custom path shapes at runtime
 */
object PathShapeRegistry {
    private val providers = mutableMapOf<PathShape, PathShapeProvider>()
    private val customProviders = mutableMapOf<String, PathShapeProvider>()

    init {
        // Register default providers
        providers[PathShape.LINEAR] = LinearPathProvider()
        providers[PathShape.CURVED] = CurvedPathProvider()
        providers[PathShape.CURVED_POLYGON] = CurvedPolygonPathProvider()
        providers[PathShape.ZIGZAG] = WavePathProvider(zigzag = true)
        providers[PathShape.SINE_WAVE] = WavePathProvider(zigzag = false)
        providers[PathShape.S_CURVE] = SCurvePathProvider()
    }

    fun getProvider(shape: PathShape): PathShapeProvider {
        return providers[shape] ?: LinearPathProvider()
    }

    fun registerCustomProvider(name: String, provider: PathShapeProvider) {
        customProviders[name] = provider
    }

    fun getCustomProvider(name: String): PathShapeProvider? {
        return customProviders[name]
    }

    fun getCustomProviderNames(): List<String> {
        return customProviders.keys.toList()
    }
}

/**
 * Re-parametrizes another provider by on-screen arc length, so equal steps in t are equal
 * distances along the drawn path. Bezier parameter t alone bunches icons up where the curve
 * bends and spreads them where it runs straight. Lengths are measured in pixels (w x h),
 * because normalized x and y are stretched differently by the screen. On top of that, the local
 * gap between icons is scaled by config.spacingStrength around config.spacingFocus.
 */
class ArcLengthPathProvider(
    private val base: PathShapeProvider,
    private val start: PointF,
    private val end: PointF,
    private val config: PathConfig,
    private val w: Float,
    private val h: Float
) : PathShapeProvider {
    private val samples = 256
    private val cumulative = FloatArray(samples + 1)

    init {
        val lengths = FloatArray(samples + 1)
        var prev = base.getPointOnPath(0f, start, end, config)
        for (i in 1..samples) {
            val p = base.getPointOnPath(i / samples.toFloat(), start, end, config)
            val dx = (p.x - prev.x) * w
            val dy = (p.y - prev.y) * h
            lengths[i] = lengths[i - 1] + kotlin.math.sqrt(dx * dx + dy * dy)
            prev = p
        }
        // Weight each stretch of path by the local gap scale; a wider gap means fewer icons per length
        val total = lengths[samples]
        val strength = config.spacingStrength.coerceAtLeast(-0.95f)
        for (i in 1..samples) {
            val seg = lengths[i] - lengths[i - 1]
            val s = if (total > 0f) (lengths[i - 1] + lengths[i]) / 2f / total else 0f
            val d = (s - config.spacingFocus) / 0.2f
            val gap = 1f + strength * kotlin.math.exp(-d * d)
            cumulative[i] = cumulative[i - 1] + seg * gap
        }
    }

    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        val total = cumulative[samples]
        if (total <= 0f) return base.getPointOnPath(t, start, end, config)
        val target = t.coerceIn(0f, 1f) * total
        var lo = 0
        var hi = samples
        while (hi - lo > 1) {
            val mid = (lo + hi) ushr 1
            if (cumulative[mid] <= target) lo = mid else hi = mid
        }
        val segLen = cumulative[hi] - cumulative[lo]
        val frac = if (segLen > 0f) (target - cumulative[lo]) / segLen else 0f
        return base.getPointOnPath((lo + frac) / samples, start, end, config)
    }
}

/**
 * Linear path - straight line from start to end
 */
class LinearPathProvider : PathShapeProvider {
    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        return PointF(
            start.x + (end.x - start.x) * t,
            start.y + (end.y - start.y) * t
        )
    }
}

/**
 * Curved path - quadratic bezier curve
 */
class CurvedPathProvider : PathShapeProvider {
    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        // Calculate control point perpendicular to the line
        val midX = (start.x + end.x) / 2
        val midY = (start.y + end.y) / 2

        // Perpendicular direction
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy)

        // Control point offset perpendicular to line
        val perpX = -dy / length * config.curveIntensity
        val perpY = dx / length * config.curveIntensity

        val controlX = midX + perpX
        val controlY = midY + perpY

        // Quadratic bezier formula: B(t) = (1-t)²P0 + 2(1-t)tP1 + t²P2
        val oneMinusT = 1 - t
        val x = oneMinusT * oneMinusT * start.x +
                2 * oneMinusT * t * controlX +
                t * t * end.x
        val y = oneMinusT * oneMinusT * start.y +
                2 * oneMinusT * t * controlY +
                t * t * end.y

        return PointF(x, y)
    }
}

/**
 * Curved polygon path - multiple straight segments forming overall curve
 */
class CurvedPolygonPathProvider : PathShapeProvider {
    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        val segments = config.polygonSegments.coerceAtLeast(2)

        // First, get the curved path points
        val curvedProvider = CurvedPathProvider()

        // Determine which segment we're in
        val segmentLength = 1f / segments
        val segmentIndex = (t / segmentLength).toInt().coerceAtMost(segments - 1)
        val segmentT = (t - segmentIndex * segmentLength) / segmentLength

        // Get start and end of this segment from the curved path
        val segmentStart = curvedProvider.getPointOnPath(
            segmentIndex * segmentLength, start, end, config
        )
        val segmentEnd = curvedProvider.getPointOnPath(
            (segmentIndex + 1) * segmentLength, start, end, config
        )

        // Linear interpolation within segment
        return PointF(
            segmentStart.x + (segmentEnd.x - segmentStart.x) * segmentT,
            segmentStart.y + (segmentEnd.y - segmentStart.y) * segmentT
        )
    }
}

/**
 * Swings sideways around the straight start-end line: triangle wave for a zigzag, sine for a wave.
 * waveCount is the number of half-waves, so the path always starts and ends on the line.
 */
class WavePathProvider(private val zigzag: Boolean) : PathShapeProvider {
    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6f)
        val phase = Math.PI * config.waveCount.coerceAtLeast(1) * t
        val wave = if (zigzag) (kotlin.math.asin(kotlin.math.sin(phase)) * 2 / Math.PI).toFloat()
                   else kotlin.math.sin(phase).toFloat()
        val offset = wave * config.waveAmplitude
        return PointF(
            start.x + dx * t - dy / length * offset,
            start.y + dy * t + dx / length * offset
        )
    }
}

/** Cubic bezier whose two control points sit on opposite sides of the line (curveIntensity sets the bend). */
class SCurvePathProvider : PathShapeProvider {
    override fun getPointOnPath(t: Float, start: PointF, end: PointF, config: PathConfig): PointF {
        val dx = end.x - start.x
        val dy = end.y - start.y
        val length = kotlin.math.sqrt(dx * dx + dy * dy).coerceAtLeast(1e-6f)
        val px = -dy / length * config.curveIntensity
        val py = dx / length * config.curveIntensity
        val c1x = start.x + dx / 3 + px
        val c1y = start.y + dy / 3 + py
        val c2x = start.x + dx * 2 / 3 - px
        val c2y = start.y + dy * 2 / 3 - py
        val u = 1 - t
        val a = u * u * u
        val b = 3 * u * u * t
        val c = 3 * u * t * t
        val d = t * t * t
        return PointF(
            a * start.x + b * c1x + c * c2x + d * end.x,
            a * start.y + b * c1y + c * c2y + d * end.y
        )
    }
}
