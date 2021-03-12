$(function () {
    var cropper;
    var img;
    // 사진 업로드 버튼
    $('#photoBtn').on('change', function () {
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
            }
            reader.readAsDataURL(event.target.files[0]);
        } else {
            alert("이미지 파일(jpg, png형식의 파일)만 올려주세요");
            $('#photoBtn').focus();
            return;
        }
    });
    // 업로드 버튼
    $('#complete').on('click', function () {
        $('.them_img').append('<div class="result_box"><img id="result" src=""></div>');
        var image = $('#image');
        var result = $('#result');
        var canvas;
        if ($('input[type="file"]').val() != "") {
            canvas = image.cropper('getData');
            var form = new FormData();
            console.log(img.files);
            form.append("input_image", img.files[0], img.files[0]["name"]);
            form.append("x", canvas.x);
            form.append("y", canvas.y);
            form.append("w", canvas.width);
            form.append("h", canvas.height);

            // crop/ 으로 요청 보내도록 했습니다.
            // 현재 문제는 다운로드가 완료되기 전에 이미지를 호출하는 것입니다..
            // upload/ 로 요청 보내서 이미지 다운로드 완료 후 호출하는 방법이 있을 것 같습니다.
            $.ajax('crop/', {
                method: 'POST',
                data: form,
                processData: false,
                contentType: false,
                success: function () {
                    alert('업로드 성공');
                },
                error: function () {
                    alert('업로드 실패');
                    $('.result_box').remove()
                },
            });

            setTimeout(function() {
                $('.them_img').empty().append('<img id="image" src="">');
                var image = $('#image');
                image.attr("src", "media/adv.png");
                console.log(image)
                
                // 이미지 캔버스에 띄우기
                
                //이미지 다운로드
                var link = document.createElement('a');
                var src = image[0].getAttribute('src');
                link.href = src
                link.download = src
                console.log(link)
                link.click();
            }, 3000);
        } else {
            alert('사진을 업로드 해주세요');
            $('input[type="file"]').focus();
            return;
        }
    });
});