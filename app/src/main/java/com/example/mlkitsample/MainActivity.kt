package com.example.mlkitsample

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        private const val TAG = "CameraXBasic"
        private const val CAMERA_PERMISSION_CODE = 0
    }

    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btn_camera.setOnClickListener(this)
        camera_capture_button.setOnClickListener(this)
        // SingleThread
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btn_camera -> {
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    // 카메라 Permission 이 있을 경우!
                    startCamera()
                    camera_capture_button.visibility = View.VISIBLE
                    viewFinder.visibility = View.VISIBLE
                    btn_camera.visibility = View.GONE
                } else {
                    // 카메라 Permission 이 없을 경우 사용자에게 요청을 한다.
                    requestPermission()
                }
            }
        }
    }

    private fun requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this@MainActivity,
                Manifest.permission.CAMERA
            )
        ) {
            // 카메라 권한 요구
            Toast.makeText(this@MainActivity, "카메라 권한이 요구됩니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        } else {
            // 다시는 보지 않기를 클릭하고 나서 요청을 보낼때!
            Toast.makeText(this@MainActivity, "카메라 허가를 받을 수 없습니다.", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }

    /**
     * 요청을 받고 나서 들어오는 Method
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
                camera_capture_button.visibility = View.VISIBLE
                viewFinder.visibility = View.VISIBLE
                btn_camera.visibility = View.GONE
            }
        } else {
            Toast.makeText(this@MainActivity, "카메라 권한 요청을 거절하였습니다.", Toast.LENGTH_SHORT).show()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {

        // ProcessCameraProvider
        // Camera 의 생명주기를 Activity 와 같은 LifeCycleOwner 의 생명주기에 Binding 시키는 것
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // fun ListenableFuture.addListener(runnable: Runnable, executor: Executor)
        // MainThread 에서 작동해야 하기 때문에 끝에 ContextCompat.getMainExecutor(this) 붙여준다.!!
        cameraProviderFuture.addListener({

            // 카메라의 수명주기 LifecycleOwner 애플리케이션의 프로세스 내에서 바인딩하는데 사용한다.
            // ProcessCameraProvider -> 생명주기에 Binding 하는 객체 가져오기
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }

            /**
             * STREAM_MODE ( 기본 )
             * 포즈 감지기는 먼저 이미지에서 가장 눈에 띄는 사람을 감지 한 다음 포즈 감지를 실행한다.
             * 가장 유력한 사람을 추적하고 각 추론에서 포즈를 반환하려고 시도 한다.
             * 비디오 스트림에서 포즈를 감지하려면 이 모드를 사용하면 된다.
             */
            val option = PoseDetectorOptions.Builder()
                .setDetectorMode(PoseDetectorOptions.STREAM_MODE).build()

            /**
             * SINGLE_IMAGE_MODE
             * 포즈 감지기는 사람을 감지 한 다음 포즈 감지를 실행한다.
             * 정적 이미지에서 포즈 감지를 사용하거나 추적이 필요하지 않은 경우 이 모드를 사용 한다.
             */
            // val option = AccuratePoseDetectorOptions.Builder()
            //     .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE).build()

            val poseDetector = PoseDetection.getClient(option)

            /**
             * 정확도를 높게 하려면 -> 해상도를 낮추고, 초점도 중요하다!!
             * 추출한 이미지에 그래픽을 오버레이 하는 경우 -> ML Kit 에서 결과를 가져온 다음 이미지를 렌더링하고
             * 단일 단계로 오버레이 한다. ( 각 입력 프레임에 대해 한 번만 디스플레이 표면에 렌더링 된다. )
             * SampleExample Class -> CameraSourcePreview, GraphicOverlay
             */

            val imageAnalysis = ImageAnalysis.Builder()
                // MLKit 를 사용할 경우 낮은 해상도에서 캡처를 고려해라!!
                // MLKit 자세를 정확하게 감지하려면 256x256 픽셀이어야 한다. ( 해상도를 낮춰서 분석할 것! )
                .setTargetResolution(Size(640, 480))
                // MLKit 를 사용할 경우 분석기가 사용 중일 때 더 많은 이미지가 생성되면 자동으로 삭제되고 대기열에 추가 되지 않음!!!
                // 분석중 image.close() 함수가 호출 시 가장 최근 이미지를 다시 가져온다.
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor, { imageProxy ->
                // 이미지 분석!! 시작
                val mediaImage = imageProxy.image
                if( null != mediaImage ) {
                    // MLKit 시작!!
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                    poseDetector.process(image)
                        .addOnSuccessListener { pose ->
                            var allPoseLandmarks = pose.allPoseLandmarks
                            if( allPoseLandmarks.size == 0 ) {
                                Log.d("asd", "사람 없음 ")
                                imageProxy.close()
                            } else {
                                tv_acc.text = "정확도 : ${allPoseLandmarks[PoseLandmark.NOSE].inFrameLikelihood}"
                                Log.d("asd", "캐치 성공! ${allPoseLandmarks[PoseLandmark.NOSE].landmarkType} : ${allPoseLandmarks[PoseLandmark.NOSE].inFrameLikelihood} ")
                                imageProxy.close()
                            }
                            // // 왼쪽 어깨
                            // val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                            // // 오른쪽 어깨
                            // val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                            // // 왼쪽 팔꿈치
                            // val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                            // // 오른쪽 팔꿈치
                            // val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
                            // // 왼쪽 손목
                            // val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                            // // 오른쪽 손목
                            // val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
                            // // 왼쪽 엉덩이
                            // val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                            // // 오른쪽 엉덩이
                            // val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                            // // 왼쪽 무릎
                            // val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                            // // 오른쪽 무릎
                            // val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
                            // // 왼쪽 발목
                            // val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                            // // 오른쪽 발목
                            // val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
                            // // 왼쪽 새끼 손가락
                            // val leftPinky = pose.getPoseLandmark(PoseLandmark.LEFT_PINKY)
                            // // 오른쪽 새끼 손가락
                            // val rightPinky = pose.getPoseLandmark(PoseLandmark.RIGHT_PINKY)
                            // // 왼쪽 검지 손가락
                            // val leftIndex = pose.getPoseLandmark(PoseLandmark.LEFT_INDEX)
                            // // 오른쪽 검지 손가락
                            // val rightIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_INDEX)
                            // // 왼쪽 엄지 손가락
                            // val leftThumb = pose.getPoseLandmark(PoseLandmark.LEFT_THUMB)
                            // // 오른쪽 엄지 손가락
                            // val rightThumb = pose.getPoseLandmark(PoseLandmark.RIGHT_THUMB)
                            // // 왼쪽 뒤꿈치
                            // val leftHeel = pose.getPoseLandmark(PoseLandmark.LEFT_HEEL)
                            // // 오른쪽 뒤꿈치
                            // val rightHeel = pose.getPoseLandmark(PoseLandmark.RIGHT_HEEL)
                            // // 왼쪽 검지 발가락
                            // val leftFootIndex = pose.getPoseLandmark(PoseLandmark.LEFT_FOOT_INDEX)
                            // // 오른쪽 검지 발가락
                            // val rightFootIndex = pose.getPoseLandmark(PoseLandmark.RIGHT_FOOT_INDEX)
                            // // 코
                            // val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
                            // // 왼쪽 눈 안
                            // val leftEyeInner = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_INNER)
                            // // 왼쪽 눈
                            // val leftEye = pose.getPoseLandmark(PoseLandmark.LEFT_EYE)
                            // // 왼쪽 눈 바깥
                            // val leftEyeOuter = pose.getPoseLandmark(PoseLandmark.LEFT_EYE_OUTER)
                            // // 오른쪽 눈 안
                            // val rightEyeInner = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_INNER)
                            // // 오른쪽 눈
                            // val rightEye = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE)
                            // // 오른쪽 눈 바깥
                            // val rightEyeOuter = pose.getPoseLandmark(PoseLandmark.RIGHT_EYE_OUTER)
                            // // 왼쪽 귀
                            // val leftEar = pose.getPoseLandmark(PoseLandmark.LEFT_EAR)
                            // // 오른쪽 귀
                            // val rightEar = pose.getPoseLandmark(PoseLandmark.RIGHT_EAR)
                            // // 왼쪽 입
                            // val leftMouth = pose.getPoseLandmark(PoseLandmark.LEFT_MOUTH)
                            // // 오른쪽 입
                            // val rightMouth = pose.getPoseLandmark(PoseLandmark.RIGHT_MOUTH)
                            // tv_acc.text = "정확도 : ${leftShoulder.inFrameLikelihood}"
                            // if( leftShoulder != null ) {
                            //     var position = leftShoulder.position
                            //     if( null != position ) {
                            //     }
                            // }
                            // 이걸 안쓰면 화면 멈춤! ( 이미지를 닫아 줘야 다음 프레임을 가져오기때문에 꼭 해줘야함! )
                        }
                        .addOnFailureListener { e ->
                            // 이걸 안쓰면 화면 멈춤! ( 이미지를 닫아 줘야 다음 프레임을 가져오기때문에 꼭 해줘야함! )
                            imageProxy.close()
                        }
                }
            })

            // Select back camera as a default
            // 후면 카메라로 Default 설정
            // DEFAULT_FRONT_CAMERA -> 정면 카메라, DEFAULT_BACK_CAMERA -> 후면 카메라
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}