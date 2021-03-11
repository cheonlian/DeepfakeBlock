from django.shortcuts import render
from django.views.generic import TemplateView
from django.http import HttpResponse, JsonResponse
from api.models import Image

from api.views import predict
from PIL import Image as pilImage

class MainView(TemplateView):
    template_name = "index.html"


def upload_view(request):
    if request.method == "POST":
        upload_image = request.FILES.get("file")
        output = predict(pilImage.open(upload_image))
        output.save("test.png")
        return HttpResponse("Well done!")
    return JsonResponse({"post": "false"})
