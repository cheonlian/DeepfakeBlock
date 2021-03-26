package com.AI.kgt_test_app.ui.main

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.*
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.AI.kgt_test_app.R
import com.theartofdev.edmodo.cropper.CropImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class MainFragment : Fragment() {
    /*  # companion object #
        ## variable ##
        imageView = Target Image를 표시할 Image View
        camera = 카메라 버튼
        gallery = 갤러리 버튼
        trans = 전환 버튼
        packageManager = 권환 관련
        store_path = 사진 임시 저장 장소

        file_path = 파일 경로
        store_path = 저장소 경로
        file_Uri = 파일 Uri
        currentPhotoPath = 파일 실제 저장 위치
        fileName = 파일 이름

        viewModel = MainViewModel

        ## function ##
        send_Image(image, path): 이미지, 파일 저장 위치
        crop_send_Image(image, path, location): 이미지, 파일 저장 위치, [중심점, 넓이 높이]
        - 공격한 이미지를 저장하고 성공 여부 리턴

        viewModel.showSaveToast
        - 상위 작업이 완료 되면 메시지 Toast
     */
    companion object {
        fun newInstance() = MainFragment()

        val REQUEST_IMAGE_CAPTURE = 1
        val REQUEST_GALLERY_TAKE = 2

        private val TAG = "MAIN_FRAGMENT"

        lateinit var packageManager:PackageManager

        lateinit var targetImage:ImageView
        lateinit var preView:ImageView
        lateinit var camera:Button
        lateinit var gallery:Button
        lateinit var trans:Button
        lateinit var noise:SeekBar
        lateinit var previewTitle: TextView

        lateinit var filePath:File
        lateinit var storePath:File
        lateinit var fileUri:Uri

        var currentPhotoPath: String? = null

        private lateinit var viewModel: MainViewModel

        @SuppressLint("SimpleDateFormat")
        val fileName = "VISION_" + SimpleDateFormat("yyMMdd_HHmm").format(Date())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {

        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(viewModelStore, MainViewModelFactory(this.activity!!.application)).get(
            MainViewModel::class.java)

        targetImage = view!!.findViewById(R.id.targetImageView)
        preView = view!!.findViewById(R.id.preview)
        camera = view!!.findViewById(R.id.camera_btn)
        gallery = view!!.findViewById(R.id.gallery_btn)
        trans = view!!.findViewById(R.id.trans)
        noise = view!!.findViewById(R.id.noisePercent)
        previewTitle = view!!.findViewById(R.id.previewTitle)
        packageManager = this.context!!.packageManager

        storePath = this.context!!.getExternalFilesDir(Environment.DIRECTORY_DCIM)!!

        preView.setImageBitmap(viewModel.Req_preview(noise.progress))

        noise.setOnSeekBarChangeListener(object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // noisebar가 변경 되면 preview 요청
                // preView.setImageBitmap(viewModel.Req_preview(noise.progress))
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                previewTitle.text = "노이즈 변경중..."
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                previewTitle.text = "노이즈 " + noise.progress.toString()
                when(noise.progress){
                    1 -> preView.setImageResource(R.drawable.noise1)
                    2 -> preView.setImageResource(R.drawable.noise2)
                    3 -> preView.setImageResource(R.drawable.noise3)
                    4 -> preView.setImageResource(R.drawable.noise4)
                    5 -> preView.setImageResource(R.drawable.noise5)
                }
            }
        })

        camera.setOnClickListener {
            trans.text = "변환"
            Log.d(TAG + "_Camera", "Start")
            dispatchTakePictureIntent()
            Log.d(TAG + "_Camera", "End")
        }
        gallery.setOnClickListener {
            trans.text = "변환"
            Log.d(TAG + "_Gallery", "Start")
            openGalleryForImage()
            Log.d(TAG + "_Gallery", "End")
        }
        trans.setOnClickListener {
            Log.d(TAG + "_Crop", "Start")
            if (targetImage.drawable == null) {
                Log.e(TAG, targetImage.toString())
                Toast.makeText(
                    this.activity!!.applicationContext,
                    "No Image",
                    Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                trans.text = "처리중 ..."
                CropImage.activity(fileUri).start(context!!, this)
            }
            Log.d(TAG + "_Crop", "End")
        }
    }

    /*  # function onActivityResult #

        ## variable ##
        bitmap = Image or Image's Uri
        targetImage = Image

        result = crop 결과
        isCrob = 0: crob을 하지 않는 방식, 1: crob을 하는 방식

        xy = [x1, y1, x2, y2]: 크롭 범위의 좌표
        realxy = [x1, y1, x2, y2]: 원본 사진 범위의 좌표
        x = (x1 + x2) / 2
        y = (y1 + y2) / 2
        w = x2 - x1
        h = y2 - y1
        xywh = [x, y, w, h]: 중심점의 좌표, 넓이, 높이
        str_xy = xywh의 원소를 String으로 변환

        ## function ##
        send_Image(image, path): 이미지, 파일 저장 위치
        crop_send_Image(image, path, location): 이미지, 파일 저장 위치, [중심점, 넓이 높이]
        - 공격한 이미지를 저장하고 성공 여부 리턴

        viewModel.showSaveToast
        - 상위 작업이 완료 되면 메시지 Toast
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && filePath.isFile) {
            val bitmap = getImage()
            targetImage.setImageBitmap(bitmap)
        }else if (requestCode == REQUEST_GALLERY_TAKE && resultCode == RESULT_OK){
            val bitmap = data?.data
            fileUri = bitmap!!
            currentPhotoPath = bitmap!!.path
            targetImage.setImageURI(bitmap) // handle chosen image
        }else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            Log.d(TAG + "_Crop Result", "Start")
            val result = CropImage.getActivityResult(data)
            trans.text = "저장중 ..."
            if (resultCode == RESULT_OK) {
                val isCrob = 1

                var xy = listOf(result.cropRect.left, result.cropRect.top, result.cropRect.right, result.cropRect.bottom)
                val realXY = listOf(result.wholeImageRect.left, result.wholeImageRect.top, result.wholeImageRect.right, result.wholeImageRect.bottom)

                val x = xy[0].toFloat()
                val y = xy[1].toFloat()
                val w = (xy[2] - xy[0]).toFloat()
                val h = (xy[3] - xy[1]).toFloat()

                val xywh = listOf(x, y, w, h)
                val targetImage = targetImage.drawable.toBitmap()

                Log.d(TAG + "_TRANS", "Photo path: ${currentPhotoPath}")
                Toast.makeText(
                        this.activity!!.applicationContext,
                        "Saving... Wait Next Message",
                        Toast.LENGTH_SHORT
                ).show()

                if (isCrob == 0){
                    if (viewModel.Send_Image(targetImage, storePath)) {
                        viewModel.Show_Save_Toast.observe(this, {
                            it.getContentIfNotHandled()?.let {
                                Toast.makeText(
                                    this.activity!!.applicationContext,
                                    "Save Success!! :)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            trans.text = "저장 성공!!"
                        })
                    }else{
                        viewModel.Show_Save_Toast.observe(this, {
                            it.getContentIfNotHandled()?.let {
                                Toast.makeText(
                                    this.activity!!.applicationContext,
                                    "Save Fail... :(",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            trans.text = "저장 실패ㅠㅠ"
                        })
                    }
                }else if(isCrob == 1) {
                    if (viewModel.Crop_Send_Image(targetImage, storePath, xywh, noise.progress)) {
                        viewModel.Show_Save_Toast.observe(this, {
                            it.getContentIfNotHandled()?.let {
                                Toast.makeText(
                                    this.activity!!.applicationContext,
                                    "Save Success!! :)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            trans.text = "저장 성공!!"
                        })
                    } else {
                        viewModel.Show_Save_Toast.observe(this, {
                            it.getContentIfNotHandled()?.let {
                                Toast.makeText(
                                    this.activity!!.applicationContext,
                                    "Save Fail... :(",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            trans.text = "저장 실패ㅠㅠ"
                        })
                    }
                }

            }
            Log.d(TAG + "_Crop Activity Result", "End")
        }else if(requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            Log.e(TAG, "CROP ERROR!!")
            trans.text = "저장 실패ㅠㅠ"
        }
    }

    /*  # onRequestPermissionsResult #
        권한요청 관련 처리
    */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG + "_PERMMISION", "Permission: " + permissions[0] + "was " + grantResults[0])
        }else{
            Log.d(TAG + "_PERMMISION","Denies")
        }
    }

    /*  # createImageFile #
        Image Temp File 생성
        fileName의 png 파일을 storepath에 생성
    */
    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val storageDir: File = storePath
        Log.d(TAG + "_CREATETEMP", "Storage Path: ${storageDir.absolutePath}")
        return File.createTempFile(
            fileName, /* prefix */
            ".png", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    /*  # createImageFile #
        카메라 열기
        카메라 앱 열어서 찍은 사진을 임시파일에 저장
    */
    private fun dispatchTakePictureIntent() {
        filePath = createImageFile()
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    ex.printStackTrace()
                    null
                }
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this.context!!,
                        "com.AI.kgt_test.fileprovider",
                        filePath
                    )
                    Log.d(TAG + "_DISPATCH", "Photo uri: $photoURI")
                    fileUri = photoURI
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    /*  # createImageFile #
        갤러리 열기
        갤러리의 사진을 선택하여 저장
    */
    private fun openGalleryForImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_GALLERY_TAKE)
    }

    /*  # getImage #
        파일에서 이미지를 읽어와서 return
    */
    private fun getImage(): Bitmap {
        val options:BitmapFactory.Options = BitmapFactory.Options()
        options.inJustDecodeBounds = true

        try {
            val inStream:InputStream = FileInputStream(filePath)
            BitmapFactory.decodeStream(inStream, null, options)
            inStream.close()
        } catch ( e:Exception ){
            e.printStackTrace()
        }

        val imOptions = BitmapFactory.Options()

        return BitmapFactory.decodeFile(filePath.absolutePath, imOptions)
    }
}