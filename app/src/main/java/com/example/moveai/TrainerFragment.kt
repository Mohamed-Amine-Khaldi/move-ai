package com.example.moveai

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import com.airbnb.lottie.LottieAnimationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class TrainerFragment : Fragment() {

    private lateinit var prefs: SharedPreferences
    private lateinit var cameraExecutor: ExecutorService

    private var whistlePlayer: MediaPlayer? = null
    private var repPlayer: MediaPlayer? = null
    private var clapPlayer: MediaPlayer? = null

    // Targets and progress
    private var targetSquats = 0; private var targetPushUps = 0; private var targetLunges = 0
    private var targetJacks = 0; private var targetPlank = 0; private var targetSitups = 0
    private var progressSquats = 0; private var progressPushUps = 0; private var progressLunges = 0
    private var progressJacks = 0; private var progressPlank = 0; private var progressSitups = 0
    private var totalTargetReps = 0; private var totalProgressReps = 0

    // Camera and ML Kit
    private var cameraProvider: ProcessCameraProvider? = null
    private var currentActiveExercise = ""
    private var currentTargetForSession = 0
    private var currentSessionReps = 0
    private var isExerciseDown = false

    // Cooldown mechanism
    private var lastRepTime = 0L
    private val REP_COOLDOWN = 800L

    // Specific timer for Plank
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isHoldingPlank = false
    private var plankTimerRunnable: Runnable? = null

    private val poseDetector by lazy {
        val options = PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
        PoseDetection.getClient(options)
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startCamera() else Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_trainer, container, false)
        cameraExecutor = Executors.newSingleThreadExecutor()

        prefs = requireActivity().getSharedPreferences("MoveAI_Prefs", Context.MODE_PRIVATE)
        loadSavedData()

        // This is the new logic to dynamically change the name
        val authPrefs = requireActivity().getSharedPreferences("MoveAI_Auth", Context.MODE_PRIVATE)
        val userName = authPrefs.getString("userName", "User")
        val tvTrainerGreeting = view.findViewById<TextView>(R.id.tvTrainerGreeting)
        tvTrainerGreeting.text = "Hello, $userName 👋"

        whistlePlayer = MediaPlayer.create(requireContext(), R.raw.referee_start)
        repPlayer = MediaPlayer.create(requireContext(), R.raw.rep_beep)
        clapPlayer = MediaPlayer.create(requireContext(), R.raw.congrats_audio)

        setupPanelLogic(view)

        setupExerciseCard(view, R.id.cardSquats, "Squats") { targetSquats }
        setupExerciseCard(view, R.id.cardPushUps, "Push-Ups") { targetPushUps }
        setupExerciseCard(view, R.id.cardLunges, "Lunges") { targetLunges }
        setupExerciseCard(view, R.id.cardJacks, "Jumping Jacks") { targetJacks }
        setupExerciseCard(view, R.id.cardPlank, "Planks") { targetPlank }
        setupExerciseCard(view, R.id.cardSitups, "Sit-Ups") { targetSitups }

        view.findViewById<FloatingActionButton>(R.id.btnBack).setOnClickListener {
            exitCameraScreen(view)
        }

        updateAllCardsUI(view)
        return view
    }

    private fun setupPanelLogic(view: View) {
        val panelCustomPlan = view.findViewById<MaterialCardView>(R.id.panelCustomPlan)
        view.findViewById<MaterialCardView>(R.id.cardDailyGoal).setOnClickListener {
            panelCustomPlan.visibility = View.VISIBLE
            panelCustomPlan.translationY = 500f
            panelCustomPlan.animate().translationY(0f).setDuration(300).start()
        }

        view.findViewById<MaterialButton>(R.id.btnCancelPlan).setOnClickListener {
            panelCustomPlan.animate().translationY(500f).setDuration(300).withEndAction { panelCustomPlan.visibility = View.GONE }.start()
        }

        view.findViewById<MaterialButton>(R.id.btnSavePlan).setOnClickListener {
            targetSquats = getTargetFromPanel(view, R.id.cbSquats, R.id.etPlanSquats)
            targetPushUps = getTargetFromPanel(view, R.id.cbPushUps, R.id.etPlanPushUps)
            targetLunges = getTargetFromPanel(view, R.id.cbLunges, R.id.etPlanLunges)
            targetJacks = getTargetFromPanel(view, R.id.cbJacks, R.id.etPlanJacks)
            targetPlank = getTargetFromPanel(view, R.id.cbPlank, R.id.etPlanPlank)
            targetSitups = getTargetFromPanel(view, R.id.cbSitups, R.id.etPlanSitups)

            progressSquats = 0; progressPushUps = 0; progressLunges = 0; progressJacks = 0; progressPlank = 0; progressSitups = 0

            saveAllData()
            updateAllCardsUI(view)
            panelCustomPlan.animate().translationY(500f).setDuration(300).withEndAction { panelCustomPlan.visibility = View.GONE }.start()
        }
    }

    private fun getTargetFromPanel(view: View, cbId: Int, etId: Int): Int {
        val cb = view.findViewById<CheckBox>(cbId)
        val et = view.findViewById<EditText>(etId)
        return if (cb.isChecked && et.text.isNotEmpty()) et.text.toString().toInt() else 0
    }

    private fun setupExerciseCard(view: View, cardId: Int, exerciseName: String, getTarget: () -> Int) {
        view.findViewById<MaterialCardView>(cardId).setOnClickListener {
            val plannedTarget = getTarget()
            val overrideInput = view.findViewById<EditText>(R.id.etQuickOverride).text.toString()

            currentTargetForSession = if (overrideInput.isNotEmpty()) overrideInput.toInt() else plannedTarget

            if (currentTargetForSession > 0) {
                currentActiveExercise = exerciseName
                currentSessionReps = 0
                isExerciseDown = false
                lastRepTime = System.currentTimeMillis()

                view.findViewById<TextView>(R.id.tvActiveExercise).text = exerciseName
                view.findViewById<TextView>(R.id.etTargetRepsCamera).text = currentTargetForSession.toString()
                view.findViewById<TextView>(R.id.tvRepCount).text = "0"

                view.findViewById<NestedScrollView>(R.id.layoutSelection).visibility = View.GONE
                view.findViewById<ConstraintLayout>(R.id.layoutCamera).visibility = View.VISIBLE

                whistlePlayer?.seekTo(0)
                whistlePlayer?.start()

                checkCameraPermission()
                if (exerciseName == "Planks") startPlankTimer()
            } else {
                Toast.makeText(requireContext(), "Please set a target first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(view?.findViewById<PreviewView>(R.id.viewFinder)?.surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy -> processImageProxy(imageProxy) }
                }

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider?.unbindAll()
                cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, imageAnalyzer)
            } catch(exc: Exception) {
                Log.e("MoveAI", "Camera bind failed", exc)
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    val rotation = imageProxy.imageInfo.rotationDegrees
                    val imgWidth = if (rotation == 90 || rotation == 270) imageProxy.height else imageProxy.width
                    val imgHeight = if (rotation == 90 || rotation == 270) imageProxy.width else imageProxy.height

                    drawSkeleton(pose, imgWidth, imgHeight)
                    countRepsLogic(pose)
                }
                .addOnCompleteListener { imageProxy.close() }
        } else {
            imageProxy.close()
        }
    }

    private fun drawSkeleton(pose: Pose, imageWidth: Int, imageHeight: Int) {
        if (view == null || imageWidth == 0 || imageHeight == 0) return
        val overlay = view?.findViewById<ImageView>(R.id.skeletonOverlay) ?: return

        val bitmap = Bitmap.createBitmap(imageWidth, imageHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val paintPoint = Paint().apply { color = Color.parseColor("#6366F1"); style = Paint.Style.FILL }
        val paintLine = Paint().apply { color = Color.parseColor("#22C55E"); strokeWidth = 8f; style = Paint.Style.STROKE; strokeJoin = Paint.Join.ROUND }

        for (landmark in pose.allPoseLandmarks) {
            canvas.drawCircle(landmark.position.x, landmark.position.y, 12f, paintPoint)
        }

        fun drawLine(type1: Int, type2: Int) {
            val p1 = pose.getPoseLandmark(type1)
            val p2 = pose.getPoseLandmark(type2)
            if (p1 != null && p2 != null) {
                canvas.drawLine(p1.position.x, p1.position.y, p2.position.x, p2.position.y, paintLine)
            }
        }

        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW)
        drawLine(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST)
        drawLine(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP)
        drawLine(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE)
        drawLine(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW)
        drawLine(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST)
        drawLine(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP)
        drawLine(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE)
        drawLine(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)

        requireActivity().runOnUiThread { overlay.setImageBitmap(bitmap) }
    }

    private fun getAngle(firstPoint: PoseLandmark?, midPoint: PoseLandmark?, lastPoint: PoseLandmark?): Double {
        if (firstPoint == null || midPoint == null || lastPoint == null) return 0.0
        var result = Math.toDegrees(
            Math.atan2((lastPoint.position.y - midPoint.position.y).toDouble(), (lastPoint.position.x - midPoint.position.x).toDouble()) -
                    Math.atan2((firstPoint.position.y - midPoint.position.y).toDouble(), (firstPoint.position.x - midPoint.position.x).toDouble())
        )
        result = Math.abs(result)
        if (result > 180) result = 360.0 - result
        return result
    }

    private fun isConfident(vararg landmarks: PoseLandmark?): Boolean {
        return landmarks.all { it != null && it.inFrameLikelihood > 0.6f }
    }

    private fun countRepsLogic(pose: Pose) {
        if (currentSessionReps >= currentTargetForSession) return

        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

        when (currentActiveExercise) {
            "Push-Ups" -> {
                if (!isConfident(leftShoulder, leftElbow, leftWrist)) return
                val angle = getAngle(leftShoulder, leftElbow, leftWrist)
                if (angle < 90) isExerciseDown = true
                else if (angle > 150 && isExerciseDown) { isExerciseDown = false; registerValidRep() }
            }
            "Squats", "Lunges" -> {
                if (!isConfident(leftHip, leftKnee, leftAnkle)) return
                val angle = getAngle(leftHip, leftKnee, leftAnkle)
                if (angle < 100) isExerciseDown = true
                else if (angle > 150 && isExerciseDown) { isExerciseDown = false; registerValidRep() }
            }
            "Sit-Ups" -> {
                if (!isConfident(leftShoulder, leftHip, leftKnee)) return
                val angle = getAngle(leftShoulder, leftHip, leftKnee)
                if (angle > 140) isExerciseDown = true
                else if (angle < 80 && isExerciseDown) { isExerciseDown = false; registerValidRep() }
            }
            "Jumping Jacks" -> {
                if (!isConfident(leftElbow, leftShoulder, leftHip)) return
                val angle = getAngle(leftElbow, leftShoulder, leftHip)
                if (angle > 140) isExerciseDown = true
                else if (angle < 50 && isExerciseDown) { isExerciseDown = false; registerValidRep() }
            }
            "Planks" -> {
                if (!isConfident(leftShoulder, leftHip, leftKnee)) {
                    isHoldingPlank = false
                    return
                }
                val angle = getAngle(leftShoulder, leftHip, leftKnee)
                isHoldingPlank = angle > 150
            }
        }
    }

    private fun startPlankTimer() {
        plankTimerRunnable = object : Runnable {
            override fun run() {
                if (currentSessionReps < currentTargetForSession) {
                    if (isHoldingPlank) registerValidRep()
                    mainHandler.postDelayed(this, 1000)
                }
            }
        }
        mainHandler.postDelayed(plankTimerRunnable!!, 1000)
    }

    private fun registerValidRep() {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastRepTime < REP_COOLDOWN && currentActiveExercise != "Planks") {
            return
        }
        lastRepTime = currentTime

        currentSessionReps++

        repPlayer?.seekTo(0)
        repPlayer?.start()

        when(currentActiveExercise) {
            "Squats" -> progressSquats++; "Push-Ups" -> progressPushUps++
            "Lunges" -> progressLunges++; "Jumping Jacks" -> progressJacks++
            "Planks" -> progressPlank++; "Sit-Ups" -> progressSitups++
        }
        saveAllData()

        requireActivity().runOnUiThread {
            view?.findViewById<TextView>(R.id.tvRepCount)?.text = currentSessionReps.toString()

            if (currentSessionReps >= currentTargetForSession) {
                plankTimerRunnable?.let { mainHandler.removeCallbacks(it) }

                clapPlayer?.seekTo(0)
                clapPlayer?.start()

                view?.findViewById<LottieAnimationView>(R.id.lottieCongrats)?.apply {
                    visibility = View.VISIBLE
                    playAnimation()
                }

                mainHandler.postDelayed({
                    view?.let { exitCameraScreen(it) }
                }, 3000)
            }
        }
    }

    private fun exitCameraScreen(view: View) {
        cameraProvider?.unbindAll()
        view.findViewById<ImageView>(R.id.skeletonOverlay).setImageBitmap(null)
        plankTimerRunnable?.let { mainHandler.removeCallbacks(it) }

        view.findViewById<ConstraintLayout>(R.id.layoutCamera).visibility = View.GONE
        view.findViewById<NestedScrollView>(R.id.layoutSelection).visibility = View.VISIBLE
        view.findViewById<LottieAnimationView>(R.id.lottieCongrats).visibility = View.GONE

        updateAllCardsUI(view)
    }

    private fun saveAllData() {
        prefs.edit().apply {
            putInt("targetSquats", targetSquats); putInt("targetPushUps", targetPushUps)
            putInt("targetLunges", targetLunges); putInt("targetJacks", targetJacks)
            putInt("targetPlank", targetPlank); putInt("targetSitups", targetSitups)

            putInt("progressSquats", progressSquats); putInt("progressPushUps", progressPushUps)
            putInt("progressLunges", progressLunges); putInt("progressJacks", progressJacks)
            putInt("progressPlank", progressPlank); putInt("progressSitups", progressSitups)
            apply()
        }
    }

    private fun loadSavedData() {
        targetSquats = prefs.getInt("targetSquats", 0); targetPushUps = prefs.getInt("targetPushUps", 0)
        targetLunges = prefs.getInt("targetLunges", 0); targetJacks = prefs.getInt("targetJacks", 0)
        targetPlank = prefs.getInt("targetPlank", 0); targetSitups = prefs.getInt("targetSitups", 0)

        progressSquats = prefs.getInt("progressSquats", 0); progressPushUps = prefs.getInt("progressPushUps", 0)
        progressLunges = prefs.getInt("progressLunges", 0); progressJacks = prefs.getInt("progressJacks", 0)
        progressPlank = prefs.getInt("progressPlank", 0); progressSitups = prefs.getInt("progressSitups", 0)
    }

    private fun updateAllCardsUI(view: View) {
        totalTargetReps = targetSquats + targetPushUps + targetLunges + targetJacks + targetPlank + targetSitups
        totalProgressReps = progressSquats + progressPushUps + progressLunges + progressJacks + progressPlank + progressSitups

        view.findViewById<TextView>(R.id.tvDailyGoalProgress).text = "$totalProgressReps / $totalTargetReps reps"
        val totalPercentage = if (totalTargetReps > 0) (totalProgressReps * 100) / totalTargetReps else 0
        view.findViewById<TextView>(R.id.tvPercentage).text = "$totalPercentage%"

        view.findViewById<LinearProgressIndicator>(R.id.mainDailyProgressBar).progress = totalPercentage
        view.findViewById<CircularProgressIndicator>(R.id.circularProgressDaily).progress = totalPercentage

        updateSingleCard(view, targetSquats, progressSquats, R.id.tvSquatsTargetLabel, R.id.tvSquatsProgress, R.id.progressBarSquats, R.id.cardSquats, "reps")
        updateSingleCard(view, targetPushUps, progressPushUps, R.id.tvPushUpsTargetLabel, R.id.tvPushUpsProgress, R.id.progressBarPushUps, R.id.cardPushUps, "reps")
        updateSingleCard(view, targetLunges, progressLunges, R.id.tvLungesTargetLabel, R.id.tvLungesProgress, R.id.progressBarLunges, R.id.cardLunges, "reps")
        updateSingleCard(view, targetJacks, progressJacks, R.id.tvJacksTargetLabel, R.id.tvJacksProgress, R.id.progressBarJacks, R.id.cardJacks, "reps")
        updateSingleCard(view, targetPlank, progressPlank, R.id.tvPlankTargetLabel, R.id.tvPlankProgress, R.id.progressBarPlank, R.id.cardPlank, "sec")
        updateSingleCard(view, targetSitups, progressSitups, R.id.tvSitupsTargetLabel, R.id.tvSitupsProgress, R.id.progressBarSitups, R.id.cardSitups, "reps")
    }

    private fun updateSingleCard(view: View, target: Int, progress: Int, labelId: Int, progressTxtId: Int, progressBarId: Int, cardId: Int, unit: String) {
        val overrideVal = view.findViewById<EditText>(R.id.etQuickOverride).text.toString()
        val displayTarget = if (overrideVal.isNotEmpty() && target == 0) overrideVal.toInt() else target

        view.findViewById<TextView>(labelId).text = "Target: $displayTarget $unit"
        view.findViewById<TextView>(progressTxtId).text = "$progress / $displayTarget"
        view.findViewById<LinearProgressIndicator>(progressBarId).progress = if (displayTarget > 0) (progress * 100) / displayTarget else 0
        view.findViewById<MaterialCardView>(cardId).alpha = if (displayTarget == 0) 0.5f else 1.0f
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraExecutor.shutdown()
        whistlePlayer?.release()
        repPlayer?.release()
        clapPlayer?.release()
        plankTimerRunnable?.let { mainHandler.removeCallbacks(it) }
    }
}