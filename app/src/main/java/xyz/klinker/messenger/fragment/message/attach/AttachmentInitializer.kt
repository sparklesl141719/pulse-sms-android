package xyz.klinker.messenger.fragment.message.attach

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewStub
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import com.afollestad.materialcamera.MaterialCamera
import xyz.klinker.giphy.Giphy
import xyz.klinker.messenger.BuildConfig
import xyz.klinker.messenger.R
import xyz.klinker.messenger.fragment.Camera2BasicFragment
import xyz.klinker.messenger.fragment.message.MessageListFragment
import xyz.klinker.messenger.fragment.message.send.PermissionHelper
import xyz.klinker.messenger.shared.data.FeatureFlags
import xyz.klinker.messenger.shared.data.MmsSettings
import xyz.klinker.messenger.shared.data.Settings
import xyz.klinker.messenger.shared.util.ColorUtils
import xyz.klinker.messenger.shared.util.TvUtils
import xyz.klinker.messenger.shared.util.listener.TextSelectedListener
import xyz.klinker.messenger.view.*

@Suppress("DEPRECATION")
class AttachmentInitializer(private val fragment: MessageListFragment) {

    private val activity: FragmentActivity? by lazy { fragment.activity }
    private val argManager 
        get() = fragment.argManager
    private val attachListener
        get() = fragment.attachListener

    private val attachLayoutStub: ViewStub by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_stub) as ViewStub }
    private val send: View by lazy { fragment.rootView!!.findViewById<View>(R.id.send) }
    private val attach: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach) }
    private val attachLayout: View by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_layout) }
    private val attachHolder: FrameLayout by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_holder) as FrameLayout }
    private val attachButtonHolder: LinearLayout by lazy { fragment.rootView!!.findViewById<View>(R.id.attach_button_holder) as LinearLayout }
    private val dragDismissFrameLayout: View by lazy { fragment.rootView!! }

    private var cameraFragment: Camera2BasicFragment? = null

    fun initAttachHolder() {
        if (!TvUtils.hasTouchscreen(activity)) {
            attach.visibility = View.GONE
            send.nextFocusDownId = R.id.message_entry
        }

        attach.setOnClickListener {
            if (attachLayoutStub.parent != null) {
                initAttachStub()
            }

            val animator: ValueAnimator
            if (attachLayout.visibility == View.VISIBLE) {
                dragDismissFrameLayout.isEnabled = true
                animator = ValueAnimator.ofInt(attachLayout.height, 0)
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        attachHolder.removeAllViews()
                        attachLayout.visibility = View.GONE
                    }
                })
            } else {
                dragDismissFrameLayout.isEnabled = false
                attachImage(true)
                attachLayout.visibility = View.VISIBLE
                animator = ValueAnimator.ofInt(0,
                        fragment.resources.getDimensionPixelSize(R.dimen.attach_menu_height))
            }

            val params = attachLayout.layoutParams as ViewGroup.MarginLayoutParams

            animator.addUpdateListener { valueAnimator ->
                params.height = valueAnimator.animatedValue as Int
                attachLayout.requestLayout()
            }

            animator.duration = 200
            animator.start()
        }
    }

    private fun initAttachStub() {
        attachLayoutStub.inflate()

        val attachImage = fragment.rootView!!.findViewById<View>(R.id.attach_image) as ImageButton
        val captureImage = fragment.rootView!!.findViewById<View>(R.id.capture_image) as ImageButton
        val attachGif = fragment.rootView!!.findViewById<View>(R.id.attach_gif) as ImageButton
        val recordVideo = fragment.rootView!!.findViewById<View>(R.id.record_video) as ImageButton
        val recordAudio = fragment.rootView!!.findViewById<View>(R.id.record_audio) as ImageButton
        val attachLocation = fragment.rootView!!.findViewById<View>(R.id.attach_location) as ImageButton
        val attachContact = fragment.rootView!!.findViewById<View>(R.id.attach_contact) as ImageButton
        val applyTemplate = fragment.rootView!!.findViewById<View>(R.id.apply_template) as ImageButton

        attachImage.setOnClickListener { attachImage() }
        captureImage.setOnClickListener { captureImage() }
        attachGif.setOnClickListener { attachGif() }
        recordVideo.setOnClickListener { recordVideo() }
        recordAudio.setOnClickListener { recordAudio() }
        attachLocation.setOnClickListener { attachLocation() }
        attachContact.setOnClickListener { attachContact() }
        applyTemplate.setOnClickListener { applyTemplate() }

        var colorButtonsDark = false
        if (Settings.useGlobalThemeColor) {
            attachButtonHolder.setBackgroundColor(Settings.mainColorSet.color)
            if (!ColorUtils.isColorDark(Settings.mainColorSet.color)) {
                colorButtonsDark = true
            }
        } else {
            attachButtonHolder.setBackgroundColor(fragment.argManager.color)
            if (!ColorUtils.isColorDark(fragment.argManager.color)) {
                colorButtonsDark = true
            }
        }

        if (colorButtonsDark) {
            val list = ColorStateList.valueOf(fragment.resources.getColor(R.color.lightToolbarTextColor))
            attachImage.imageTintList = list
            captureImage.imageTintList = list
            attachGif.imageTintList = list
            recordVideo.imageTintList = list
            recordAudio.imageTintList = list
            attachLocation.imageTintList = list
            attachContact.imageTintList = list
            applyTemplate.imageTintList = list
        }
    }

    private fun attachImage() {
        attachImage(false)
    }

    internal fun attachImage(alwaysOpen: Boolean) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 0 || activity == null) {
            return
        }

        try {
            prepareAttachHolder(0)
            if (ContextCompat.checkSelfPermission(activity!!,
                    Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                attachHolder.addView(AttachImageView(activity!!, attachListener,
                        if (Settings.useGlobalThemeColor)  Settings.mainColorSet.color else argManager.color))
            } else {
                attachPermissionRequest(PermissionHelper.PERMISSION_STORAGE_REQUEST,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        } catch (e: NullPointerException) {

        }

    }

    private fun captureImage() {
        if (getBoldedAttachHolderPosition() == 1) {
            return
        }

        prepareAttachHolder(1)

        cameraFragment = Camera2BasicFragment.newInstance()
        activity?.supportFragmentManager?.beginTransaction()?.add(R.id.attach_holder, cameraFragment)?.commit()
        cameraFragment?.attachImageSelectedListener(attachListener)
    }

    private fun attachGif() {
        if (getBoldedAttachHolderPosition() == 2) {
            return
        }

        prepareAttachHolder(2)
        Giphy.Builder(activity, BuildConfig.GIPHY_API_KEY)
                .maxFileSize(MmsSettings.maxImageSize)
                .start()
    }

    private fun recordVideo() {
        if (getBoldedAttachHolderPosition() == 3 || activity == null) {
            return
        }

        prepareAttachHolder(3)

        val camera = MaterialCamera(activity!!)
                .saveDir(activity!!.filesDir.path)
                .qualityProfile(MaterialCamera.QUALITY_LOW)
                .maxAllowedFileSize(MmsSettings.maxImageSize)
                .allowRetry(false)
                .autoSubmit(true)
                .showPortraitWarning(false)

        if (Settings.useGlobalThemeColor) {
            camera.primaryColor(Settings.mainColorSet.color)
        } else {
            camera.primaryColor(argManager.color)
        }

        camera.start(AttachmentListener.RESULT_VIDEO_REQUEST)
    }

    private fun recordAudio() {
        recordAudio(false)
    }

    internal fun recordAudio(alwaysOpen: Boolean) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 4 || activity == null) {
            return
        }

        prepareAttachHolder(4)
        if (ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(RecordAudioView(activity!!, attachListener,
                    if (Settings.useGlobalThemeColor) Settings.mainColorSet.colorAccent else argManager.colorAccent))
        } else {
            attachPermissionRequest(PermissionHelper.PERMISSION_AUDIO_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun attachLocation() {
        attachLocation(false)
    }

    internal fun attachLocation(alwaysOpen: Boolean) {
        if (!alwaysOpen && getBoldedAttachHolderPosition() == 5 || activity == null) {
            return
        }

        prepareAttachHolder(5)
        if (ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(AttachLocationView(activity!!, fragment.attachListener, attachListener,
                    if (Settings.useGlobalThemeColor) Settings.mainColorSet.colorAccent else argManager.colorAccent))
        } else {
            attachPermissionRequest(PermissionHelper.PERMISSION_LOCATION_REQUEST,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun attachContact() {
        if (getBoldedAttachHolderPosition() == 6 || activity == null) {
            return
        }

        prepareAttachHolder(6)
        if (ContextCompat.checkSelfPermission(activity!!,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            attachHolder.addView(AttachContactView(activity!!, attachListener,
                    if (Settings.useGlobalThemeColor) Settings.mainColorSet.color else argManager.color))
        } else {
            attachPermissionRequest(PermissionHelper.PERMISSION_AUDIO_REQUEST,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun applyTemplate() {
        if (getBoldedAttachHolderPosition() == 7 || activity == null) {
            return
        }

        prepareAttachHolder(7)
        attachHolder.addView(TemplateManagerView(activity!!,
                if (Settings.useGlobalThemeColor) Settings.mainColorSet.colorAccent else argManager.colorAccent,
                object : TextSelectedListener {
                    override fun onTextSelected(text: String) {
                        fragment.onBackPressed()
                        attachListener.onTextSelected(text)
                    }
                }))
    }

    private fun attachPermissionRequest(permissionRequestCode: Int, vararg permissions: String) {
        LayoutInflater.from(activity).inflate(R.layout.permission_request, attachHolder, true)
        val request = attachHolder.findViewById<View>(R.id.permission_needed) as Button
        request.setOnClickListener { fragment.requestPermissions(permissions, permissionRequestCode) }
    }

    private fun prepareAttachHolder(positionToBold: Int) {
        fragment.dismissKeyboard()
        attachHolder.removeAllViews()

        for (i in 0 until attachButtonHolder.childCount) {
            if (positionToBold == i) {
                attachButtonHolder.getChildAt(i).alpha = 1.0f
            } else {
                attachButtonHolder.getChildAt(i).alpha = 0.5f
            }
        }
    }

    private fun getBoldedAttachHolderPosition(): Int {
        return (0 until attachButtonHolder.childCount).firstOrNull { attachButtonHolder.getChildAt(it).alpha == 1.0f }
                ?: -1
    }
}