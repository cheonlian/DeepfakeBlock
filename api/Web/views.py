import os
from django.http.response import HttpResponseNotAllowed

from django.shortcuts import redirect, render
from django.http import FileResponse
from django.views.generic import TemplateView
from django.http import HttpResponse, JsonResponse, Http404
from django.views.decorators.csrf import csrf_exempt

from api.models import Image

from api.views import crop
from PIL import Image as pilImage


class MainView(TemplateView):
    template_name = "main.html"

def index(request):
    return render(request,"main.html")


# 이 함수를 사용해서 새로운 탬플릿을 만드는 것도 좋아보여요
@csrf_exempt
def upload_view(request):
    if request.method == "POST":
        crop(request)
    return HttpResponse("")




    #     x1, y1, x2, y2 = request.POST["x1"], request.POST["y1"], request.POST["x2"], request.POST["y2"]
    #     print(x1,y1,x2,y2)
    #     input_image = request.FILES["input_image"]
    #     if input_image is not None:
    #         # output = predict(pilImage.open(input_image))
    #         output = pilImage.open(input_image)
    #         output.save("media/test.png")

            
    #         response = FileResponse(
    #             open("media/test.png", "rb"), content_type="image/png"
    #         )
    #         return response
    #         # image 띄우기 완료 -> 어떻게 다운로드 할 수 있는지?
    #         # render로 context 넘겨서 html상 띄울 수 있나?
    #     else:
    #         return redirect("web-main")
    # return JsonResponse({"post": "false"})
