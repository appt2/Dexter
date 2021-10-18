package ma.dexter.ui.activity

import android.Manifest
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.viewpager2.widget.ViewPager2
import com.github.angads25.filepicker.model.DialogConfigs
import com.github.angads25.filepicker.model.DialogProperties
import com.github.angads25.filepicker.view.FilePickerDialog
import com.github.zawadz88.materialpopupmenu.popupMenu
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ma.dexter.R
import ma.dexter.databinding.ActivityMainBinding
import ma.dexter.managers.DexProjectManager
import ma.dexter.ui.adapter.DexPagerAdapter
import ma.dexter.ui.viewmodel.MainViewModel
import ma.dexter.util.*
import java.io.File

class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    private val viewModel: MainViewModel by viewModels()
    private lateinit var pagerAdapter: DexPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ), 100
        )
        requestAllFilesAccessPermission(this)

        subtitle = "by MikeAndrson"

        initLiveData()
        initTabs()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (binding.viewPager.currentItem != 0) return false

        menuInflater.inflate(R.menu.menu_main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (binding.viewPager.currentItem != 0) return false

        when (item.itemId) {
            R.id.it_more -> {
                popupMenu {
                    section {
                        item {
                            label = "Export DEX"
                            callback = {
                                exportDex()
                            }
                        }
                    }
                    section {
                        title = "About"
                        item {
                            label = "GitHub"
                            callback = {
                                openUrl(this@MainActivity, "https://github.com/MikeAndrson/Dexter")
                            }
                        }
                    }
                }.show(this, findViewById(R.id.it_more))
            }
        }

        return super.onOptionsItemSelected(item)
    }

    private fun exportDex() {
        val properties = DialogProperties().apply {
            root = storagePath
            selection_type = DialogConfigs.DIR_SELECT
        }

        FilePickerDialog(this, properties).apply {
            setTitle("Select a folder to extract DEX files to")
            setDialogSelectionListener { files ->
                DexProjectManager.dexContainer.exportTo(File(files[0]))
                debugToast("Done")
            }
            show()
        }
    }

    private fun initLiveData() {
        viewModel.getPageItems().observe(this) {
            pagerAdapter.submitList(it)
        }

        viewModel.currentPosition.observe(this) {
            binding.viewPager.currentItem = it
        }
    }

    private fun initTabs() {
        pagerAdapter = DexPagerAdapter(this)

        binding.viewPager.isUserInputEnabled = false
        binding.viewPager.offscreenPageLimit = 100 // todo
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                viewModel.viewPagerScrolled.value = positionOffsetPixels
            }
            override fun onPageSelected(pos: Int) {
                invalidateOptionsMenu()
            }
        })
        binding.viewPager.adapter = pagerAdapter

        viewModel.addMainItem()

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, pos ->
            tab.text = pagerAdapter.getItem(pos).getTitle()
            tab.setIcon(pagerAdapter.getItem(pos).getIconResId())
        }.attach()

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {}

            override fun onTabUnselected(tab: TabLayout.Tab?) {}

            override fun onTabReselected(tab: TabLayout.Tab) {
                // position 0 is reserved for DexEditorFragment (for now)
                if (tab.position == 0) return

                popupMenu {
                    section {
                        item {
                            label = "Close"
                            callback = {
                                viewModel.removePageItem(tab.position)
                            }
                        }
                        item {
                            label = "Close others"
                            callback = {
                                viewModel.removeAllPageItems(excludePos = tab.position)
                            }
                        }
                        item {
                            label = "Close all"
                            callback = {
                                viewModel.removeAllPageItems()
                            }
                        }
                    }
                }.show(this@MainActivity, tab.view)
            }
        })
    }

}
