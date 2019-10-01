package pm.gnosis.heimdall.ui.safe.create

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager
import com.jakewharton.rxbinding2.view.clicks
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import pm.gnosis.heimdall.R
import pm.gnosis.heimdall.reporting.ScreenId
import pm.gnosis.heimdall.ui.base.BaseActivity
import kotlinx.android.synthetic.main.layout_create_safe_intro_page.view.*
import kotlinx.android.synthetic.main.screen_create_safe_intro.*

class CreateSafeIntroActivity : BaseActivity() {

    override fun screenId(): ScreenId? = ScreenId.CREATE_SAFE_INTRO

    private val viewPager = object : PagerAdapter() {

        override fun instantiateItem(container: ViewGroup, position: Int): Any {
            val inflater = LayoutInflater.from(container.context)
            val layoutId = R.layout.layout_create_safe_intro_page
            val layout = inflater.inflate(layoutId, container, false)
            val page = SafeIntroPage.values()[position]
            layout.intro_page_title.text = getString(page.titleResId)
            layout.intro_page_text.text = getString(page.textResId)
            layout.intro_page_image.setImageResource(page.imgResId)
            container.addView(layout)
            return layout
        }

        override fun destroyItem(container: ViewGroup, position: Int, any: Any) {
            (any as? View)?.let { container.removeView(it) }
        }

        override fun isViewFromObject(view: View, any: Any) = view == any

        override fun getCount(): Int = SafeIntroPage.values().size
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.screen_create_safe_intro)
        create_safe_intro_pager.adapter = viewPager
        create_safe_intro_pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                updateUi(position)
            }
        })
        create_safe_intro_indicator.setViewPager(create_safe_intro_pager)
        create_safe_intro_button.setOnClickListener {
            if (create_safe_intro_pager.currentItem == (SafeIntroPage.values().size  - 1)) {
                startActivity(CreateSafeStepsActivity.createIntent(this))
            } else
                create_safe_intro_pager.currentItem++
        }
        create_safe_intro_back_arrow.setOnClickListener { onBackPressed() }
    }

    override fun onStart() {
        super.onStart()
        updateUi(create_safe_intro_pager.currentItem)
        disposables += create_safe_intro_back_arrow.clicks()
            .subscribeBy { onBackPressed() }
    }

    private fun updateUi(position: Int) {
        if ((SafeIntroPage.values().size - 1) == position) {
            create_safe_intro_button.text = getString(R.string.get_started)
        } else {
            create_safe_intro_button.text = getString(R.string.next)
        }
    }

    companion object {
        fun createIntent(context: Context) = Intent(context, CreateSafeIntroActivity::class.java)
    }
}


enum class SafeIntroPage(
    val titleResId: Int,
    val textResId: Int,
    val imgResId: Int
) {

    WHAT_IS_SAFE(
        R.string.what_is_the_gnosis_safe,
        R.string.your_safe_is_a_smart_contract,
        R.drawable.img_safe_intro_what_is_safe
    ),
    CONTRACT(
        R.string.secure_by_design,
        R.string.while_our_code_is_always_audited,
        R.drawable.img_safe_intro_contract
    ),
    CONNECT(
        R.string.crypto_without_the_hassle,
        R.string.with_walletconnect_you_can_connect,
        R.drawable.img_safe_intro_connect
    ),
    CONTROL(
        R.string.you_are_in_control,
        R.string.your_funds_are_held_securely,
        R.drawable.img_safe_intro_control
    )
}