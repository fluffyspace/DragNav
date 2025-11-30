package com.ingokodba.dragnav.rainbow

import android.graphics.PointF

/**
 * Defines the shape of the path between start and end points
 */
enum class PathShape {
    LINEAR,           // Straight line from start to end
    CURVED,           // Smooth bezier curve
    CURVED_POLYGON    // Multiple straight segments forming a curved overall shape
}

/**
 * Configuration for the app path layout
 * All coordinates are normalized (0 to 1) where:
 * - x: 0 = left edge, 1 = right edge
 * - y: 0 = bottom edge, 1 = top edge
 */
data class PathConfig(
    // Path endpoints (normalized 0-1)
    val startPoint: PointF = PointF(0.5f, 0f),      // Default: bottom center
    val endPoint: PointF = PointF(1f, 0.5f),        // Default: right center

    // Path shape
    val pathShape: PathShape = PathShape.CURVED,

    // For curved paths, control point offset (0-1, affects curve intensity)
    val curveIntensity: Float = 0.5f,

    // For curved polygon, number of segments
    val polygonSegments: Int = 8,

    // App display settings
    val appIconSize: Float = 0.08f,                  // Size relative to screen width
    val appSpacing: Float = 0.05f,                   // Spacing between apps (0-1 of path length)
    val showAppNames: Boolean = true,
    val appNameSize: Float = 12f,                    // Text size in sp

    // Favorites button settings
    val favButtonPosition: PointF = PointF(0.15f, 0.15f),  // Position (normalized)
    val favButtonSize: Float = 0.08f,                       // Size relative to screen width

    // Letter index settings (for fast scrolling)
    val letterIndexEnabled: Boolean = true,
    val letterIndexPosition: LetterIndexPosition = LetterIndexPosition.RIGHT,
    val letterIndexSize: Float = 0.04f,              // Size relative to screen width
    val letterIndexPadding: Float = 0.02f,           // Padding from edge

    // App sorting
    val appSortOrder: AppSortOrder = AppSortOrder.ASCENDING  // Alphabetical order (A-Z or Z-A)
)

enum class LetterIndexPosition {
    LEFT,
    RIGHT
}

enum class AppSortOrder {
    ASCENDING,   // A to Z
    DESCENDING   // Z to A
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
