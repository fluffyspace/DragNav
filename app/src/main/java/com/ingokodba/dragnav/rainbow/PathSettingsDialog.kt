package com.ingokodba.dragnav.rainbow

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.core.view.setPadding
import com.example.dragnav.R
import com.google.android.material.slider.Slider
import com.google.android.material.tabs.TabLayout

/**
 * Floating settings dialog with categorized options
 * 10% opaque background with organized slider categories
 */
class PathSettingsDialog(
    context: Context,
    private var config: PathConfig,
    private val onConfigChanged: (PathConfig) -> Unit,
    private val onCategoryChanged: ((Category) -> Unit)? = null
) : Dialog(context) {

    private lateinit var tabLayout: TabLayout
    private lateinit var contentContainer: FrameLayout

    private val categories = listOf(
        Category.PATH,
        Category.APPS,
        Category.APP_NAMES,
        Category.FAVORITES,
        Category.SEARCH,
        Category.LETTERS
    )

    enum class Category(val title: String) {
        PATH("Path"),
        APPS("Apps"),
        APP_NAMES("App Names"),
        FAVORITES("Favorites"),
        SEARCH("Search"),
        LETTERS("Letters")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val rootLayout = createRootLayout()
        setContentView(rootLayout)

        setupWindow()
        setupTabs()
        showCategory(Category.PATH)

        // Notify initial category
        onCategoryChanged?.invoke(Category.PATH)
    }

    override fun dismiss() {
        // Notify that we're leaving all categories
        onCategoryChanged?.invoke(Category.PATH)  // Reset to default when closing
        super.dismiss()
    }

    private fun setupWindow() {
        window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#1A000000"))) // 10% opacity black
            setLayout(
                (context.resources.displayMetrics.widthPixels * 0.9).toInt(),
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setGravity(Gravity.CENTER)

            // Allow touches outside to dismiss
            setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
        }
    }

    private fun createRootLayout(): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#E6222222")) // 90% opacity dark background
            setPadding(24)
        }

        // Close button
        val closeButton = ImageButton(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setBackgroundColor(Color.TRANSPARENT)
            setColorFilter(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.END
            }
            setOnClickListener { dismiss() }
        }
        root.addView(closeButton)

        // Title
        val title = TextView(context).apply {
            text = context.getString(R.string.rainbow_preferences)
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 16)
        }
        root.addView(title)

        // Tab layout
        tabLayout = TabLayout(context).apply {
            setBackgroundColor(Color.parseColor("#33FFFFFF"))
            setSelectedTabIndicatorColor(Color.WHITE)
            setTabTextColors(Color.GRAY, Color.WHITE)
            tabMode = TabLayout.MODE_SCROLLABLE
        }
        root.addView(tabLayout)

        // Content container
        contentContainer = FrameLayout(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 16
            }
            minimumHeight = 400
        }
        root.addView(contentContainer)

        return root
    }

    private fun setupTabs() {
        categories.forEach { category ->
            tabLayout.addTab(tabLayout.newTab().setText(category.title))
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = categories[tab.position]
                showCategory(category)
                onCategoryChanged?.invoke(category)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun showCategory(category: Category) {
        contentContainer.removeAllViews()

        val scrollView = ScrollView(context).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val content = when (category) {
            Category.PATH -> createPathSettings()
            Category.APPS -> createAppsSettings()
            Category.APP_NAMES -> createAppNamesSettings()
            Category.FAVORITES -> createFavoritesSettings()
            Category.SEARCH -> createSearchSettings()
            Category.LETTERS -> createLettersSettings()
        }

        scrollView.addView(content)
        contentContainer.addView(scrollView)
    }

    private fun createPathSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Path shape selector
            addView(createLabel("Path Shape"))
            addView(createPathShapeSpinner())

            // Start point
            addView(createLabel("Start Point"))
            addView(createPointSliders("start",
                config.startPoint,
                { x -> config = config.copy(startPoint = PointF(x, config.startPoint.y)); notifyChange() },
                { y -> config = config.copy(startPoint = PointF(config.startPoint.x, y)); notifyChange() }
            ))

            // End point
            addView(createLabel("End Point"))
            addView(createPointSliders("end",
                config.endPoint,
                { x -> config = config.copy(endPoint = PointF(x, config.endPoint.y)); notifyChange() },
                { y -> config = config.copy(endPoint = PointF(config.endPoint.x, y)); notifyChange() }
            ))

            // Curve intensity (for curved paths)
            addView(createLabel("Curve Intensity"))
            addView(createSlider(-1f, 1f, config.curveIntensity) {
                config = config.copy(curveIntensity = it)
                notifyChange()
            })

            // Polygon segments (for curved polygon)
            addView(createLabel("Polygon Segments"))
            addView(createSlider(2f, 20f, config.polygonSegments.toFloat()) {
                config = config.copy(polygonSegments = it.toInt())
                notifyChange()
            })
        }
    }

    private fun createAppsSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Sort order
            addView(createLabel("Sort Order"))
            addView(createSortOrderSpinner())

            // Icon size
            addView(createLabel("Icon Size"))
            addView(createSlider(0.03f, 0.5f, config.appIconSize) {
                config = config.copy(appIconSize = it)
                notifyChange()
            })

            // App spacing
            addView(createLabel("App Spacing"))
            addView(createSlider(0.02f, 0.15f, config.appSpacing) {
                config = config.copy(appSpacing = it)
                notifyChange()
            })

            // Scroll sensitivity
            addView(createLabel("Scroll Sensitivity"))
            addView(TextView(context).apply {
                text = "Higher values = faster scrolling (e.g., 10 = 1cm touch scrolls 10cm)"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(0.5f, 20f, config.scrollSensitivity) {
                config = config.copy(scrollSensitivity = it)
                notifyChange()
            })
        }
    }

    private fun createAppNamesSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Show app names toggle
            addView(createCheckbox("Show App Names", config.showAppNames) {
                config = config.copy(showAppNames = it)
                notifyChange()
            })

            // Name position
            addView(createLabel("Name Position (offset from icon center)"))

            // X offset slider
            addView(TextView(context).apply {
                text = "Horizontal Offset (-1 = left, 1 = right)"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(-1f, 1f, config.appNameOffsetX) {
                config = config.copy(appNameOffsetX = it)
                notifyChange()
            })

            // Y offset slider
            addView(TextView(context).apply {
                text = "Vertical Offset (-1 = below, 1 = above)"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(-1f, 1f, config.appNameOffsetY) {
                config = config.copy(appNameOffsetY = it)
                notifyChange()
            })

            // Text size
            addView(createLabel("Text Size"))
            addView(createSlider(8f, 24f, config.appNameSize) {
                config = config.copy(appNameSize = it)
                notifyChange()
            })

            // Style
            addView(createLabel("Text Style"))
            addView(createAppNameStyleSpinner())

            // Border width (only for BORDERED style)
            addView(createLabel("Border Width (for bordered style)"))
            addView(createSlider(1f, 8f, config.appNameBorderWidth) {
                config = config.copy(appNameBorderWidth = it)
                notifyChange()
            })

            // Font
            addView(createLabel("Font"))
            addView(createAppNameFontSpinner())
        }
    }

    private fun createFavoritesSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Button position
            addView(createLabel("Button Position"))
            addView(createPointSliders("fav",
                config.favButtonPosition,
                { x -> config = config.copy(favButtonPosition = PointF(x, config.favButtonPosition.y)); notifyChange() },
                { y -> config = config.copy(favButtonPosition = PointF(config.favButtonPosition.x, y)); notifyChange() }
            ))

            // Button size
            addView(createLabel("Button Size"))
            addView(createSlider(0.05f, 0.5f, config.favButtonSize) {
                config = config.copy(favButtonSize = it)
                notifyChange()
            })
        }
    }

    private fun createSearchSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Button position
            addView(createLabel("Button Position"))
            addView(createPointSliders("search",
                config.searchButtonPosition,
                { x -> config = config.copy(searchButtonPosition = PointF(x, config.searchButtonPosition.y)); notifyChange() },
                { y -> config = config.copy(searchButtonPosition = PointF(config.searchButtonPosition.x, y)); notifyChange() }
            ))

            // Button size
            addView(createLabel("Button Size"))
            addView(createSlider(0.05f, 0.5f, config.searchButtonSize) {
                config = config.copy(searchButtonSize = it)
                notifyChange()
            })
        }
    }

    private fun createLettersSettings(): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16)

            // Enable letter index
            addView(createCheckbox("Enable Letter Index", config.letterIndexEnabled) {
                config = config.copy(letterIndexEnabled = it)
                notifyChange()
            })

            // Position (left/right)
            addView(createLabel("Position"))
            addView(createLetterPositionSpinner())

            // Size
            addView(createLabel("Letter Size"))
            addView(createSlider(0.02f, 0.08f, config.letterIndexSize) {
                config = config.copy(letterIndexSize = it)
                notifyChange()
            })

            // Padding from edge
            addView(createLabel("Padding from Edge"))
            addView(createSlider(0f, 0.1f, config.letterIndexPadding) {
                config = config.copy(letterIndexPadding = it)
                notifyChange()
            })

            // Pan from letters
            addView(createLabel("Pan from Letters"))
            addView(TextView(context).apply {
                text = "Clickable area padding from letters toward edge"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(0f, 0.3f, config.letterIndexPanFromLetters) {
                config = config.copy(letterIndexPanFromLetters = it)
                notifyChange()
            })
        }
    }

    private fun createLabel(text: String): TextView {
        return TextView(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, 16, 0, 8)
        }
    }

    private fun createSlider(min: Float, max: Float, value: Float, onChange: (Float) -> Unit): Slider {
        return Slider(context).apply {
            valueFrom = min
            valueTo = max
            this.value = value.coerceIn(min, max)
            addOnChangeListener { _, newValue, _ ->
                onChange(newValue)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
    }

    private fun createPointSliders(
        prefix: String,
        point: PointF,
        onXChange: (Float) -> Unit,
        onYChange: (Float) -> Unit
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL

            // X slider
            addView(TextView(context).apply {
                text = "X (left → right)"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(0f, 1f, point.x, onXChange))

            // Y slider
            addView(TextView(context).apply {
                text = "Y (bottom → top)"
                setTextColor(Color.LTGRAY)
                textSize = 12f
            })
            addView(createSlider(0f, 1f, point.y, onYChange))
        }
    }

    private fun createCheckbox(text: String, checked: Boolean, onChange: (Boolean) -> Unit): CheckBox {
        return CheckBox(context).apply {
            this.text = text
            setTextColor(Color.WHITE)
            isChecked = checked
            setOnCheckedChangeListener { _, isChecked ->
                onChange(isChecked)
            }
        }
    }

    private fun createPathShapeSpinner(): Spinner {
        return Spinner(context).apply {
            val shapes = PathShape.values()
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                shapes.map { it.name.replace("_", " ") }
            )
            setSelection(shapes.indexOf(config.pathShape))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    config = config.copy(pathShape = shapes[position])
                    notifyChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun createLetterPositionSpinner(): Spinner {
        return Spinner(context).apply {
            val positions = LetterIndexPosition.values()
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                positions.map { it.name }
            )
            setSelection(positions.indexOf(config.letterIndexPosition))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    config = config.copy(letterIndexPosition = positions[position])
                    notifyChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun createSortOrderSpinner(): Spinner {
        return Spinner(context).apply {
            val orders = AppSortOrder.values()
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                orders.map {
                    when (it) {
                        AppSortOrder.ASCENDING -> "A to Z"
                        AppSortOrder.DESCENDING -> "Z to A"
                    }
                }
            )
            setSelection(orders.indexOf(config.appSortOrder))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    config = config.copy(appSortOrder = orders[position])
                    notifyChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun createAppNameStyleSpinner(): Spinner {
        return Spinner(context).apply {
            val styles = AppNameStyle.values()
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                styles.map { it.name.replace("_", " ") }
            )
            setSelection(styles.indexOf(config.appNameStyle))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    config = config.copy(appNameStyle = styles[position])
                    notifyChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun createAppNameFontSpinner(): Spinner {
        return Spinner(context).apply {
            val fonts = AppNameFont.values()
            adapter = ArrayAdapter(
                context,
                android.R.layout.simple_spinner_dropdown_item,
                fonts.map { it.name.replace("_", " ") }
            )
            setSelection(fonts.indexOf(config.appNameFont))
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    (view as? TextView)?.setTextColor(Color.WHITE)
                    config = config.copy(appNameFont = fonts[position])
                    notifyChange()
                }
                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        }
    }

    private fun notifyChange() {
        onConfigChanged(config)
    }

    fun getConfig(): PathConfig = config
}
