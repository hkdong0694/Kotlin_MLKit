# Kotlin_MLKit
CameraX API 와 MLKit 를 이용하여 이미지 실시간 분석 예제 ( Pose Detection )


~~~kotlin

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
                                /**
                                 *
                                PoseLandmark.NOSE = 0;                      // 코
                                PoseLandmark.LEFT_EYE_INNER = 1;            // 왼쪽 눈 안
                                PoseLandmark.LEFT_EYE = 2;                  // 왼쪽 눈
                                PoseLandmark.LEFT_EYE_OUTER = 3;            // 왼쪽 눈 바깥
                                PoseLandmark.RIGHT_EYE_INNER = 4;           // 오른쪽 눈 안
                                PoseLandmark.RIGHT_EYE = 5;                 // 오른쪽 눈
                                PoseLandmark.RIGHT_EYE_OUTER = 6;           // 오른쪽 눈 바깥
                                PoseLandmark.LEFT_EAR = 7;                  // 왼쪽 귀
                                PoseLandmark.RIGHT_EAR = 8;                 // 오른쪽 귀
                                PoseLandmark.LEFT_MOUTH = 9;                // 왼쪽 입
                                PoseLandmark.RIGHT_MOUTH = 10;              // 오른쪽 입
                                PoseLandmark.LEFT_SHOULDER = 11;            // 왼쪽 어깨
                                PoseLandmark.RIGHT_SHOULDER = 12;           // 오른쪽 어깨
                                PoseLandmark.LEFT_ELBOW = 13;               // 왼쪽 팔꿈치
                                PoseLandmark.RIGHT_ELBOW = 14;              // 오른쪽 팔꿈치
                                PoseLandmark.LEFT_WRIST = 15;               // 왼쪽 손목
                                PoseLandmark.RIGHT_WRIST = 16;              // 오른쪽 손목
                                PoseLandmark.LEFT_PINKY = 17;               // 왼쪽 새끼 손가락
                                PoseLandmark.RIGHT_PINKY = 18;              // 오른쪽 새끼 손가락
                                PoseLandmark.LEFT_INDEX = 19;               // 왼쪽 검지 손가락
                                PoseLandmark.RIGHT_INDEX = 20;              // 오른쪽 검지 손가락
                                PoseLandmark.LEFT_THUMB = 21;               // 왼쪽 엄지 손가락
                                PoseLandmark.RIGHT_THUMB = 22;              // 오른쪽 엄지 손가락
                                PoseLandmark.LEFT_HIP = 23;                 // 왼쪽 엉덩이
                                PoseLandmark.RIGHT_HIP = 24;                // 오른쪽 엉덩이
                                PoseLandmark.LEFT_KNEE = 25;                // 왼쪽 무릎
                                PoseLandmark.RIGHT_KNEE = 26;               // 오른쪽 무릎
                                PoseLandmark.LEFT_ANKLE = 27;               // 왼쪽 발목
                                PoseLandmark.RIGHT_ANKLE = 28;              // 오른쪽 발목
                                PoseLandmark.LEFT_HEEL = 29;                // 왼쪽 뒤꿈치
                                PoseLandmark.RIGHT_HEEL = 30;               // 오른쪽 뒤꿈치
                                PoseLandmark.LEFT_FOOT_INDEX = 31;          // 왼쪽 검지 발가락
                                PoseLandmark.RIGHT_FOOT_INDEX = 32;         // 오른쪽 검지 발가락
                                 */
                                tv_acc.text = "정확도 : ${allPoseLandmarks[PoseLandmark.NOSE].inFrameLikelihood}"
                                Log.d("asd", "캐치 성공! ${allPoseLandmarks[PoseLandmark.NOSE].landmarkType} : ${allPoseLandmarks[PoseLandmark.NOSE].inFrameLikelihood} ")
                                // 이걸 안쓰면 화면 멈춤! ( 이미지를 닫아 줘야 다음 프레임을 가져오기때문에 꼭 해줘야함! )
                                imageProxy.close()
                            }
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

~~~