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
