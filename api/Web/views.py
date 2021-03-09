from django.shortcuts import render
from django.views.generic import TemplateView
from django.http import HttpResponse, JsonResponse
from api.models import Image


class MainView(TemplateView):
    template_name = "index.html"


def upload_view(request):
    if request.method == "POST":
        upload_image = request.FILES.get("file")
        Image.objects.create(input_image=upload_image)
        return HttpResponse("Well done!")
    return JsonResponse({"post": "false"})
