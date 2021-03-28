var cropper;
var img;
var uploadState = 0;
var first = true;
const btnGroup = document.getElementById('btn-group');
const zoomInBtn = document.getElementById('ZoomInBtn');
const zoomOutBtn = document.getElementById('ZoomOutBtn');

var uploadFile;
$(document).ready(function () {
    // 사진 업로드 버튼
    $('#photoBtn').on('change', function () {
        $("#upload").html("변환");
        uploadState = 0;
        $('.them_img').empty().append('<img id="image" src="">');
        var image = $('#image');
        var imgFile = $('#photoBtn').val();
        img = document.getElementById("photoBtn");
        var fileForm = /(.*?)\.(jpg|jpeg|png)$/;

        // 이미지가 확장자 확인 후 노출
        if (imgFile.match(fileForm)) {
            var reader = new FileReader();
            reader.onload = function (event) {
                image.attr("src", event.target.result);
                cropper = image.cropper({
                    dragMode: 'crop',
                    viewMode: 1,
                    // aspectRatio: 1,
                    autoCropArea: 0.9,
                    minCropBoxWidth: 10,
                    restore: false,
                    guides: false,
                    center: false,
                    highlight: false,
                    cropBoxMovable: true,
                    cropBoxResizable: true,
                    toggleDragModeOnDblclick: false
                });
            }
            reader.readAsDataURL(event.target.files[0]);
            uploadFile = event.target.files[0];
            btnGroup.style.display = "block";
            zoomInBtn.addEventListener('click', function () { image.cropper("zoom", 0.1) });
            zoomOutBtn.addEventListener('click', function () { image.cropper("zoom", -0.1) });
        } else {
            alert("이미지 파일(jpg, png형식의 파일)만 올려주세요");
            $('#photoBtn').focus();
            return;
        }
    });
    // 업로드 버튼
    $('#upload').on('click', function () { upload(); });
    $('#download').on('click', function () { download(); });

    //드래그앤 드롭
    $('.them_img')
        .on("dragover", dragOver)
        .on("dragleave", dragOver)
        .on("drop", uploadFiles);

    function dragOver(e) {
        e.stopPropagation();
        e.preventDefault();
        if (e.type == "dragover") {
            $(e.target).css({
                "background-color": "black",
                "outline-offset": "-20px"
            });
        } else {
            $(e.target).css({
                "background-color": "gray",
                "outline-offset": "-10px"
            });
        }
    }
    function uploadFiles(e) {
        $("#upload").html("변환");

        e.stopPropagation();
        e.preventDefault();
        dragOver(e); //1

        e.dataTransfer = e.originalEvent.dataTransfer; //2
        var files = e.target.files || e.dataTransfer.files;

        uploadState = 0;
        $('.them_img').empty().append('<img id="image" src="">');

        var image = $('#image');
        
        img = document.getElementById("photoBtn");
        var fileForm = /(.*?)\.(jpg|jpeg|png)$/;

        // 이미지가 확장자 확인 후 노출
        if (files[0].type.match(/image.*/)) {
            image.attr("src", window.URL.createObjectURL(files[0]));
            cropper = image.cropper({
                dragMode: 'crop',
                viewMode: 1,
                // aspectRatio: 1,
                autoCropArea: 1,
                minCropBoxWidth: 10,
                restore: false,
                guides: false,
                center: false,
                highlight: false,
                cropBoxMovable: true,
                cropBoxResizable: true,
                toggleDragModeOnDblclick: false
            });
            uploadFile = files[0];
            // btnGroup.style.display = "block";
            zoomInBtn.addEventListener('click', function () { image.cropper("zoom", 0.1) });
            zoomOutBtn.addEventListener('click', function () { image.cropper("zoom", -0.1) });
        } else {
            alert("이미지 파일(jpg, png형식의 파일)만 올려주세요");
            $('#photoBtn').focus();
            return;
        }
    }

});


function upload() {
    //0은 업로드일때 누를때
    if (uploadState == 0) {
        // $('.them_img').append('<div class="result_box"><img id="result" src=""></div>');
        var image = $('#image');
        var result = $('#result');
        var canvas;

        if (uploadFile) {
            canvas = image.cropper('getData');
            var noise = $('#noise').val();
            var form = new FormData();
            form.append("input_image", uploadFile, uploadFile["name"]);
            form.append("x", canvas.x);
            form.append("y", canvas.y);
            form.append("w", canvas.width);
            form.append("h", canvas.height);
            form.append("n", noise)

            $.ajax('upload/', {
                method: 'POST',
                data: form,
                processData: false,
                contentType: false,
                success: function () {
                    first = false;
                    $("#toggle").prop("checked", false);
                    $('.them_img').empty().append('<img id="image" src="" style="width:100%;">');
                    $("#upload").html("되돌리기");

                    var image = $('#image');

                    var tmpDate = new Date();
                    image.attr("src", "media/adv.png?" + tmpDate.getTime());

                    uploadState = 2;
                    // alert('업로드 성공');
                },
                error: function () {
                    alert('업로드 실패');
                    $('.result_box').remove();
                },
            });
            $("#upload").html(`
            <div id="loading"></div>
            `);

        } else {
            alert('사진을 업로드 해주세요');
            $('input[type="file"]').focus();
            return;
        }
        uploadState = 1;
    }
    //1이면 이미지 처리중, 아무것도 안함
    else if (uploadState == 1) {
        //동글뱅이 넣어주기
        //마우스 가도 손모양 안나오기
    }
    else if (uploadState==2){
        revert();
        $("#upload").html("변환");
        uploadState=0;

    }
}

function download(){
    var image = $('#image');
    var link = document.createElement('a');
    var src = image[0].getAttribute('src');
    link.href = src;
    link.download = src;
    console.log(link);
    link.click();
}

function setNoise(){
    var src = "media/noise_example/" + $("#noise").val() + ".png";
    $("#example").attr("src", src);
}

function clickToggle(){
    if(first) return;
    var val = event.target.checked;
    var tmpDate = new Date();
    if(val) $('#image').attr("src", "media/after.png?" + tmpDate.getTime());
    else $('#image').attr("src", "media/adv.png?" + tmpDate.getTime());
}

function revert(){
    var image = $("#image");
    image.attr("src", "media/ori.png");
    cropper = image.cropper({
        dragMode: 'crop',
        viewMode: 1,
        // aspectRatio: 1,
        autoCropArea: 1,
        minCropBoxWidth: 10,
        restore: false,
        guides: false,
        center: false,
        highlight: false,
        cropBoxMovable: true,
        cropBoxResizable: true,
        toggleDragModeOnDblclick: false
    });
    // btnGroup.style.display = "block";
    zoomInBtn.addEventListener('click', function () { image.cropper("zoom", 0.1) });
    zoomOutBtn.addEventListener('click', function () { image.cropper("zoom", -0.1) });
}