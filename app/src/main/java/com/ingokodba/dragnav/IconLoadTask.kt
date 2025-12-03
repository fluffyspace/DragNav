package com.ingokodba.dragnav

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.Executors
import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Async icon loading system with priority-based execution and cancellation support.
 * Based on Lawnchair's IconCache async loading approach.
 */
class IconLoadTask(
    val packageName: String,
    val priority: Priority,
    private val callback: (String, Drawable?, String) -> Unit
) : Comparable<IconLoadTask> {

    enum class Priority(val value: Int) {
        HIGH(0),      // Visible on screen
        MEDIUM(1),    // Just off screen
        LOW(2)        // Background loading
    }

    private val cancelled = AtomicBoolean(false)
    private var job: Job? = null

    fun cancel() {
        cancelled.set(true)
        job?.cancel()
    }

    fun isCancelled(): Boolean = cancelled.get()

    suspend fun execute(context: Context, qualityIcons: Boolean) {
        if (cancelled.get()) return

        try {
            val iconCache = IconCache(context)
            val (drawable, color) = iconCache.getIcon(packageName, qualityIcons)

            if (!cancelled.get()) {
                callback(packageName, drawable, color)
            }
        } catch (e: CancellationException) {
            // Task was cancelled, ignore
        } catch (e: Exception) {
            Log.e(TAG, "Error loading icon for $packageName", e)
        }
    }

    override fun compareTo(other: IconLoadTask): Int {
        // Lower priority value = higher priority
        return this.priority.value.compareTo(other.priority.value)
    }

    companion object {
        private const val TAG = "IconLoadTask"
    }
}

/**
 * Icon loading executor with priority queue and thread pool.
 * Manages async icon loading with configurable parallelism.
 */
class IconLoadExecutor(
    private val context: Context,
    private val qualityIcons: Boolean
) {
    // Priority queue for tasks
    private val taskQueue = PriorityBlockingQueue<IconLoadTask>()

    // Active tasks map for deduplication
    private val activeTasks = mutableMapOf<String, IconLoadTask>()

    // Coroutine scope with custom dispatcher
    private val threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE)
    private val scope = CoroutineScope(threadPool.asCoroutineDispatcher() + SupervisorJob())

    // Track pending task count
    private val pendingCount = AtomicInteger(0)

    /**
     * Submit an icon loading task with given priority.
     * Returns a cancellable task handle.
     */
    fun submitTask(
        packageName: String,
        priority: IconLoadTask.Priority = IconLoadTask.Priority.MEDIUM,
        callback: (String, Drawable?, String) -> Unit
    ): IconLoadTask {
        // Check if already loading this package
        synchronized(activeTasks) {
            activeTasks[packageName]?.let {
                // Already loading, just update priority if needed
                if (priority.value < it.priority.value) {
                    // New task has higher priority, cancel old and create new
                    it.cancel()
                } else {
                    return it
                }
            }
        }

        val task = IconLoadTask(packageName, priority, callback)

        synchronized(activeTasks) {
            activeTasks[packageName] = task
        }

        taskQueue.offer(task)
        pendingCount.incrementAndGet()

        // Process queue
        processNextTask()

        return task
    }

    /**
     * Submit multiple tasks in bulk.
     */
    fun submitBulk(
        packageNames: List<String>,
        priority: IconLoadTask.Priority = IconLoadTask.Priority.LOW,
        callback: (String, Drawable?, String) -> Unit
    ): List<IconLoadTask> {
        return packageNames.map { packageName ->
            submitTask(packageName, priority, callback)
        }
    }

    private fun processNextTask() {
        scope.launch {
            val task = taskQueue.poll() ?: return@launch

            if (task.isCancelled()) {
                synchronized(activeTasks) {
                    activeTasks.remove(task.packageName)
                }
                pendingCount.decrementAndGet()
                return@launch
            }

            try {
                task.execute(context, qualityIcons)
            } finally {
                synchronized(activeTasks) {
                    activeTasks.remove(task.packageName)
                }
                pendingCount.decrementAndGet()

                // Process next task if any
                if (taskQueue.isNotEmpty()) {
                    processNextTask()
                }
            }
        }
    }

    /**
     * Cancel all pending tasks.
     */
    fun cancelAll() {
        taskQueue.clear()
        synchronized(activeTasks) {
            activeTasks.values.forEach { it.cancel() }
            activeTasks.clear()
        }
        pendingCount.set(0)
    }

    /**
     * Cancel task for a specific package.
     */
    fun cancelTask(packageName: String) {
        synchronized(activeTasks) {
            activeTasks[packageName]?.cancel()
            activeTasks.remove(packageName)
        }
    }

    /**
     * Get number of pending tasks.
     */
    fun getPendingCount(): Int = pendingCount.get()

    /**
     * Shutdown the executor.
     */
    fun shutdown() {
        cancelAll()
        scope.cancel()
        threadPool.shutdown()
    }

    companion object {
        private const val THREAD_POOL_SIZE = 3 // Balance between performance and resource usage
    }
}
