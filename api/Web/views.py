import os

from django.shortcuts import redirect, render
from django.http import FileResponse
from django.views.generic import TemplateView
from django.http import HttpResponse, JsonResponse, Http404

from api.models import Image
from api.views import predict

from PIL import Image as pilImage


class MainView(TemplateView):
    template_name = "main.html"


def upload_view(request):
    if request.method == "POST":
        input_image = request.FILES.get("file")
        output = predict(pilImage.open(input_image))
        # output = pilImage.open(input_image)
        output.save("media/test.png")

        # Not yet run. WHY!?
        response = FileResponse(open("media/test.png", "rb"), content_type="image/png")
        return response
    return JsonResponse({"post": "false"})